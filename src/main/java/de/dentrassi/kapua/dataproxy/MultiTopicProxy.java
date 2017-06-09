/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package de.dentrassi.kapua.dataproxy;

import static java.time.Instant.now;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.eclipse.kapua.gateway.client.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiTopicProxy implements Runnable {

    public static class State {

        private Payload.Builder payload = new Payload.Builder();

        private Instant expires = Instant.now();

        private String device;

        public State(final String device, final Instant expires) {
            this.device = device;
            this.expires = expires;
        }

        public void add(final String key, final FieldHandler handler, final String value) throws Exception {
            handler.handle(key, value, payload);
        }

        public Payload getPayload() {
            return payload.build();
        }

        public boolean hasAll(Set<String> requiredFields) {
            return payload.values().keySet().containsAll(requiredFields);
        }

        public boolean isTimedOut() {
            return !this.expires.isAfter(Instant.now());
        }

        @Override
        public String toString() {
            return String.format("%s/%s -> %s", device, expires, payload);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(MultiTopicProxy.class);

    private String url;
    private String username;
    private String password;
    private String baseTopic;

    private Map<String, FieldHandler> requiredFields;
    private Duration dataPeriod;

    private ProxyReceiver proxyReceiver;

    private Map<String, State> states = new HashMap<>();

    private ScheduledExecutorService executor;

    public MultiTopicProxy(final String url, final String username, final String password, final String baseTopic, final Map<String, FieldHandler> requiredFields, final Duration dataPeriod,
            final ScheduledExecutorService executor, final ProxyReceiver proxyReceiver) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(proxyReceiver);
        Objects.requireNonNull(requiredFields);
        Objects.requireNonNull(dataPeriod);
        Objects.requireNonNull(executor);

        this.url = url;
        this.username = username;
        this.password = password;
        this.baseTopic = baseTopic;

        this.requiredFields = new HashMap<>(requiredFields);
        this.dataPeriod = dataPeriod;

        this.executor = executor;
        this.proxyReceiver = proxyReceiver;
    }

    @Override
    public void run() {
        executor.scheduleAtFixedRate(this::evict, dataPeriod.toMillis(), dataPeriod.toMillis(), TimeUnit.MILLISECONDS);

        try {
            while (true) {
                try {
                    runOnce();
                } catch (Exception e) {
                    logger.warn("Failed to run message loop", e);
                    try {
                        Thread.sleep(1_000);
                    } catch (InterruptedException e1) {
                        return;
                    }
                }
            }
        } finally {

        }
    }

    private synchronized void evict() {
        final Iterator<Map.Entry<String, State>> i = this.states.entrySet().iterator();

        while (i.hasNext()) {
            final Entry<String, State> entry = i.next();
            if (entry.getValue().isTimedOut()) {
                // handleTimeOut ( state );
                i.remove();
                logger.debug("Timeout: {}", entry.getValue());
            }
        }
    }

    protected void runOnce() throws Exception {
        final JmsConnectionFactory factory = new JmsConnectionFactory(username, password, url);

        try (final Connection connection = factory.createConnection()) {
            connection.start();

            try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                final Destination dest;
                if (baseTopic != null && !baseTopic.isEmpty()) {
                    dest = session.createTopic(baseTopic + ".#");
                } else {
                    dest = session.createTopic("#");
                }

                logger.info("Proxying for: {}", dest);

                try (final MessageConsumer consumer = session.createConsumer(dest)) {
                    while (true) {
                        final Message message = consumer.receive();
                        if (message instanceof BytesMessage) {
                            processMessage((BytesMessage) message);
                        }
                    }

                }

            }

        }
    }

    private void processMessage(final BytesMessage message) throws Exception {
        final Optional<org.eclipse.kapua.gateway.client.Topic> topic = makeTopic(message.getJMSDestination());
        if (!topic.isPresent()) {
            return;
        }

        handleChange(topic.get(), Bytes.getAsString(message));
    }

    private void handleChange(org.eclipse.kapua.gateway.client.Topic topic, String value) throws Exception {
        logger.debug("Processing - {} -> {}", topic, value);

        if (topic.getSegments().size() < 2) {
            return;
        }

        final String name = topic.getSegments().get(topic.getSegments().size() - 1);
        final FieldHandler handler = requiredFields.get(name);
        if (handler == null) {
            logger.debug("Received unknown value: {} / {}", topic, name);
            return;
        }

        final String device = topic.getSegments().get(topic.getSegments().size() - 2);

        synchronized (this) {
            State state = states.get(device);
            if (state == null || state.isTimedOut()) {
                logger.debug("Start new state for: {}", device);
                state = new State(device, now().plus(dataPeriod));
                states.put(device, state);
            }

            state.add(name, handler, value);

            if (state.hasAll(requiredFields.keySet())) {
                states.remove(device);
                publish(topic.getSegments().subList(0, topic.getSegments().size() - 1), state);
            }
        }
    }

    private void publish(final List<String> topic, final State state) throws Exception {
        logger.debug("Publish: {} -> {}", topic, state);
        proxyReceiver.dataChange(org.eclipse.kapua.gateway.client.Topic.of(topic), state.getPayload());
    }

    private static Optional<org.eclipse.kapua.gateway.client.Topic> makeTopic(final Destination source) throws JMSException {
        if (source instanceof Topic) {
            return parseTopic(((Topic) source).getTopicName());
        } else if (source instanceof Queue) {
            return parseTopic(((Queue) source).getQueueName());
        }
        return Optional.empty();
    }

    private static Optional<org.eclipse.kapua.gateway.client.Topic> parseTopic(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(org.eclipse.kapua.gateway.client.Topic.split(name.replace('.', '/')));
    }
}

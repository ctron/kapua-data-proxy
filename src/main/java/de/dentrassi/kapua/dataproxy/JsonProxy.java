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

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

import com.google.gson.GsonBuilder;

import de.dentrassi.kapua.dataproxy.config.Broker;
import de.dentrassi.kapua.dataproxy.util.Bytes;

public class JsonProxy extends AbstractProxy implements Runnable {

    private static final String TIMESTAMP_FIELD = "@timestamp";

    private static final Logger logger = LoggerFactory.getLogger(JsonProxy.class);

    public static class Configuration {

        private Broker broker;

        private String baseTopic;

        public void setBroker(Broker broker) {
            this.broker = broker;
        }

        public Broker getBroker() {
            return broker;
        }

        public void setBaseTopic(String baseTopic) {
            this.baseTopic = baseTopic;
        }

        public String getBaseTopic() {
            return baseTopic;
        }
    }

    private String url;
    private String username;
    private String password;
    private String baseTopic;

    public JsonProxy(final String url, final String username, final String password, final String baseTopic, final ProxyReceiver proxyReceiver) {
        super(proxyReceiver);

        Objects.requireNonNull(url);

        this.url = url;
        this.username = username;
        this.password = password;
        this.baseTopic = baseTopic;
    }

    public JsonProxy(final Configuration configuration, final ProxyReceiver proxyReceiver) {
        this(
                configuration.getBroker().getUrl().toString(),
                configuration.getBroker().getUser(),
                configuration.getBroker().getPassword(),
                configuration.getBaseTopic(),
                proxyReceiver);
    }

    @Override
    public void run() {
        while (true) {
            logger.info("Entering message loop");
            try {
                runOnce();
            } catch (final Exception e) {
                logger.warn("Failed to run message loop", e);
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e1) {
                    logger.warn("Go interrupted, exiting ...", e1);
                    return;
                }
            }
        }
    }

    protected void runOnce() throws Exception {
        final JmsConnectionFactory factory = new JmsConnectionFactory(username, password, url);

        try (final Connection connection = factory.createConnection()) {
            connection.start();

            try (final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                final Destination dest;
                if (baseTopic != null && !baseTopic.isEmpty()) {
                    dest = session.createTopic(baseTopic + ".#");
                } else {
                    dest = session.createTopic("#");
                }

                logger.info("Proxying for: {}", dest);

                try (final MessageConsumer consumer = session.createConsumer(dest)) {
                    logger.info("Running loop");
                    while (true) {
                        final Message message = consumer.receive();
                        logger.debug("Received message: {}", message);
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

        final String payload = Bytes.getAsString(message);

        final Map<String, Object> values = parseValues(payload);
        final Instant timestamp = createTimestamp(message, values);

        publish(topic.get(), Payload.of(timestamp, values));
    }

    private Instant createTimestamp(final Message message, final Map<String, Object> values) throws JMSException {
        final Instant timestamp;
        if (message.getJMSTimestamp() > 0) {
            timestamp = Instant.ofEpochMilli(message.getJMSTimestamp());
        } else if (message.getJMSDeliveryTime() > 0) {
            timestamp = Instant.ofEpochMilli(message.getJMSDeliveryTime());
        } else if (values.get(TIMESTAMP_FIELD) instanceof Number) {
            timestamp = Instant.ofEpochMilli(((Number) values.get(TIMESTAMP_FIELD)).longValue());
        } else {
            timestamp = Instant.now();
        }
        return timestamp;
    }

    private static Map<String, Object> parseValues(final String payload) {
        try {
            return new GsonBuilder().create().<Map<String, Object>> fromJson(payload, Map.class);
        } catch (final Exception e) {
            logger.warn("Failed to decode payload: {}", payload, e);
            throw e;
        }
    }

    private static Optional<org.eclipse.kapua.gateway.client.Topic> makeTopic(final Destination source) throws JMSException {
        if (source instanceof Topic) {
            return parseTopic(((Topic) source).getTopicName());
        } else if (source instanceof Queue) {
            return parseTopic(((Queue) source).getQueueName());
        }
        return Optional.empty();
    }

    private static Optional<org.eclipse.kapua.gateway.client.Topic> parseTopic(final String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(org.eclipse.kapua.gateway.client.Topic.split(name.replace('.', '/')));
    }
}

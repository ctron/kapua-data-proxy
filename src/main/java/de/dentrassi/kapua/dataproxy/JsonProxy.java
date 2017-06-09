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

import java.nio.charset.StandardCharsets;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

public class JsonProxy implements Runnable {

    private static final String TIMESTAMP_FIELD = "@timestamp";

    private static final Logger logger = LoggerFactory.getLogger(JsonProxy.class);

    private String url;
    private String username;
    private String password;
    private String baseTopic;

    private ProxyReceiver proxyReceiver;

    public JsonProxy(String url, String username, String password, String baseTopic, ProxyReceiver proxyReceiver) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(proxyReceiver);

        this.url = url;
        this.username = username;
        this.password = password;
        this.baseTopic = baseTopic;
        this.proxyReceiver = proxyReceiver;
    }

    @Override
    public void run() {
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

        final String payload = new String(message.getBody(byte[].class), StandardCharsets.UTF_8);
        final Map<String, Object> values = parseValues(payload);

        final Instant timestamp = createTimestamp(message, values);

        proxyReceiver.dataChange(topic.get(), timestamp, values);
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
        return new GsonBuilder().create().<Map<String, Object>> fromJson(payload, Map.class);
    }

    private static Optional<org.eclipse.kapua.gateway.client.Topic> makeTopic(Destination source) throws JMSException {
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
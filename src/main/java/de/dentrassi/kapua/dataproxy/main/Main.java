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
package de.dentrassi.kapua.dataproxy.main;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kapua.gateway.client.Client;
import org.eclipse.kapua.gateway.client.Credentials;
import org.eclipse.kapua.gateway.client.mqtt.fuse.FuseClient;
import org.eclipse.kapua.gateway.client.profile.kura.KuraMqttProfile;

import de.dentrassi.kapua.dataproxy.FieldHandler;
import de.dentrassi.kapua.dataproxy.JsonProxy;
import de.dentrassi.kapua.dataproxy.MultiTopicProxy;
import de.dentrassi.kapua.dataproxy.ProxyApplication;

public class Main {

    public static void main(String[] args) throws Exception {
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {

            try (final Client client = KuraMqttProfile.newProfile(FuseClient.Builder::new)
                    .accountName("kapua-sys")
                    .brokerUrl("tcp://adaris.muc.redhat.com:31883")
                    .clientId("proxy-1")
                    .credentials(Credentials.userAndPassword("kapua-broker", "kapua-password"))
                    .build()) {

                try (final ProxyApplication proxyApplication = new ProxyApplication.Builder(client)
                        .build()) {

                    new Thread(new JsonProxy("amqp://10.200.68.162:5672", "secret", "fooBAR", "sensors", proxyApplication)).start();

                    final Map<String, FieldHandler> fields = new HashMap<>();

                    fields.put("TIME", FieldHandler.timestamp(value -> {
                        final LocalDateTime ldt = LocalDateTime.from(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(value));
                        return ldt.atZone(ZoneId.of("+1")).toInstant();
                    }));
                    fields.put("TEMP", FieldHandler.data(Double::parseDouble));

                    new Thread(new MultiTopicProxy("amqp://10.200.68.162:5672", "secret", "fooBAR", "tele", fields, Duration.ofSeconds(1), executor, proxyApplication))
                            .start();

                    while (true) {
                        Thread.sleep(Long.MAX_VALUE);
                    }

                }

            }
        } finally {
            executor.shutdown();
        }

    }

}

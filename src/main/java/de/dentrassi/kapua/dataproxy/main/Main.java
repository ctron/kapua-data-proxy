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

import static org.eclipse.kapua.gateway.client.Credentials.userAndPassword;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.kapua.gateway.client.Client;
import org.eclipse.kapua.gateway.client.mqtt.fuse.FuseClient;
import org.eclipse.kapua.gateway.client.profile.kura.KuraMqttProfile;

import de.dentrassi.kapua.dataproxy.JsonProxy;
import de.dentrassi.kapua.dataproxy.MultiTopicProxy;
import de.dentrassi.kapua.dataproxy.ProxyApplication;
import de.dentrassi.kapua.dataproxy.config.Configuration;
import de.dentrassi.kapua.dataproxy.config.Proxy;
import de.dentrassi.kapua.dataproxy.provider.Providers;
import de.dentrassi.kapua.dataproxy.util.FieldHandler;

public class Main {

    public static void main(String[] args) throws Exception {

        final String proxyBrokerUrl = System.getenv("PROXY_BROKER_URL");

        if (proxyBrokerUrl == null || proxyBrokerUrl.isEmpty()) {
            throw new IllegalStateException("Missing env-var 'PROXY_BROKER_URL'");
        }

        if ( args.length < 1 ) {
            throw new IllegalArgumentException("main <path-to-config.json>");
        }
        
        final Configuration configuration = Configuration.fromJson(Paths.get(args[0]));

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {

            final KuraMqttProfile<?> profile = KuraMqttProfile.newProfile(FuseClient.Builder::new);
            profile.accountName(configuration.getKapua().getAccountName());
            profile.brokerUrl(configuration.getKapua().getBroker().getUrl().toString());
            profile.clientId(configuration.getKapua().getClientId());

            if (configuration.getKapua().getBroker().getUser() != null && configuration.getKapua().getBroker().getPassword() != null) {
                profile.credentials(
                        userAndPassword(
                                configuration.getKapua().getBroker().getUser(),
                                configuration.getKapua().getBroker().getPassword()));
            }

            try (final Client client = profile.build()) {

                try (final ProxyApplication proxyApplication = new ProxyApplication.Builder(client)
                        .build()) {

                    /*
                    for (Map.Entry<String, Proxy> entry : configuration.getProxies().entrySet()) {
                        Providers.create(entry.getValue());
                    }
                    */

                    new Thread(new JsonProxy(proxyBrokerUrl, "secret", "fooBAR", "sensors", proxyApplication)).start();

                    final Map<String, FieldHandler> fields = new HashMap<>();

                    fields.put("TIME", FieldHandler.timestamp(value -> {
                        final LocalDateTime ldt = LocalDateTime.from(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(value));
                        return ldt.atZone(ZoneId.of("+1")).toInstant();
                    }));
                    fields.put("TEMP", FieldHandler.data(Double::parseDouble));

                    new Thread(new MultiTopicProxy(proxyBrokerUrl, "secret", "fooBAR", "tele", fields, Duration.ofSeconds(1), executor, proxyApplication))
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

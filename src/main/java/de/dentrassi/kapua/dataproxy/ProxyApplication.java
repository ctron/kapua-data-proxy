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

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kapua.gateway.client.Application;
import org.eclipse.kapua.gateway.client.Client;
import org.eclipse.kapua.gateway.client.Payload;
import org.eclipse.kapua.gateway.client.Topic;

public final class ProxyApplication implements AutoCloseable, ProxyReceiver {

    public static class Builder {

        private Application.Builder application;

        public Builder(final Client client) {
            requireNonNull(client);
            this.application = client.buildApplication("proxy");
        }

        public ProxyApplication build() {
            Application app = application.build();
            return new ProxyApplication(app);
        }
    }

    private Application application;

    private ProxyApplication(Application application) {
        requireNonNull(application);
        this.application = application;
    }

    @Override
    public void close() throws Exception {
        this.application.close();
    }

    @Override
    public void dataChange(final Topic topic, final Instant timestamp, final Map<String, ?> values) throws Exception {
        requireNonNull(topic);
        requireNonNull(timestamp);
        requireNonNull(values);

        application.data(topic).send(Payload.of(timestamp, new HashMap<>(values)));
    }

}

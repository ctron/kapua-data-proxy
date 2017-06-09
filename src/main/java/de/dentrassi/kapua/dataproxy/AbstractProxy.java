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

import java.util.Objects;

import org.eclipse.kapua.gateway.client.Payload;
import org.eclipse.kapua.gateway.client.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractProxy {

    private static final Logger logger = LoggerFactory.getLogger(AbstractProxy.class);

    private ProxyReceiver proxyReceiver;

    public AbstractProxy(ProxyReceiver proxyReceiver) {
        Objects.requireNonNull(proxyReceiver);

        this.proxyReceiver = proxyReceiver;
    }

    protected void publish(final Topic topic, final Payload payload) throws Exception {
        logger.debug("Publish: {} -> {}", topic, payload);
        proxyReceiver.dataChange(topic, payload);
    }
}
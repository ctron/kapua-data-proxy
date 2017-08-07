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
package de.dentrassi.kapua.dataproxy.provider;

import java.util.Optional;
import java.util.ServiceLoader;

import de.dentrassi.kapua.dataproxy.config.Proxy;

public final class Providers {

    private Providers() {
    }

    public static Optional<ProxyInstance> create(final Proxy proxy) {
        for (final ProxyProvider provider : ServiceLoader.load(ProxyProvider.class)) {
            if (provider.supports(proxy)) {
                return Optional.of(provider.create(proxy));
            }
        }
        return Optional.empty();
    }
}

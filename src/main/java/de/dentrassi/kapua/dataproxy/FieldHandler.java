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
import java.time.temporal.TemporalAccessor;

import org.eclipse.kapua.gateway.client.Payload;

@FunctionalInterface
public interface FieldHandler {

    public void handle(final String key, final String value, final Payload.Builder payload) throws Exception;

    public static <T> FieldHandler data(final Parser<T> parser) {
        return (key, value, payload) -> payload.put(key, parser.parse(value));
    }

    public static FieldHandler timestamp(final Parser<TemporalAccessor> parser) {
        return (key, value, payload) -> payload.timestamp(Instant.from(parser.parse(value)));
    }
}

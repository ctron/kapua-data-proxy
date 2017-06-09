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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.jms.BytesMessage;
import javax.jms.JMSException;

public final class Bytes {

    private Bytes() {
    }

    public static String getAsString(final BytesMessage message) throws JMSException {
        final ByteBuffer data = ByteBuffer.wrap(message.getBody(byte[].class));

        // limit with first null byte
        for (int i = 0; i < data.limit(); i++) {
            if (data.get(i) == 0) {
                data.limit(i);
                break;
            }
        }

        // return result
        return StandardCharsets.UTF_8.decode(data).toString();
    }

}

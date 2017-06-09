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

import org.eclipse.kapua.gateway.client.Topic;

public interface ProxyReceiver {

    public void dataChange(Topic topic, Instant timestamp, Map<String, ?> values) throws Exception;
}

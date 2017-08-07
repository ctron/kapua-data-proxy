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

import de.dentrassi.kapua.dataproxy.config.Proxy;

public interface ProxyProvider {

    public boolean supports(final Proxy proxy);

    public ProxyInstance create(final Proxy proxy);
}

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
package de.dentrassi.kapua.dataproxy.config;

import com.google.gson.JsonObject;

public class Proxy {

    private String type;

    private JsonObject configuration;

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setConfiguration(JsonObject configuration) {
        this.configuration = configuration;
    }

    public JsonObject getConfiguration() {
        return configuration;
    }

}

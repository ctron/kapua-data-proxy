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

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Configuration {

    private KapuaConfiguration kapua;
    private Map<String, Proxy> proxies;

    public void setKapua(final KapuaConfiguration kapua) {
        this.kapua = kapua;
    }

    public KapuaConfiguration getKapua() {
        return kapua;
    }

    public Map<String, Proxy> getProxies() {
        return proxies;
    }

    public static Gson createGson() {
        return createGson(null);
    }

    public static Gson createGson(final Consumer<GsonBuilder> customizer) {
        final GsonBuilder builder = new GsonBuilder();
        if (customizer != null) {
            customizer.accept(builder);
        }
        return builder.create();
    }

    public static Configuration fromJson(final String json) {
        return createGson().fromJson(json, Configuration.class);
    }

    public static Configuration fromJson(final Reader reader) {
        return createGson().fromJson(reader, Configuration.class);
    }

    public static Configuration fromJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return fromJson(reader);
        }
    }
}

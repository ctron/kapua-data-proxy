package de.dentrassi.kapua.dataproxy.provider.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.dentrassi.kapua.dataproxy.JsonProxy;
import de.dentrassi.kapua.dataproxy.JsonProxy.Configuration;
import de.dentrassi.kapua.dataproxy.ProxyApplication;
import de.dentrassi.kapua.dataproxy.config.Proxy;
import de.dentrassi.kapua.dataproxy.provider.ProxyInstance;
import de.dentrassi.kapua.dataproxy.provider.ProxyProvider;

public class JsonProxyProvider implements ProxyProvider {

    private final class JsonProxyInstance implements ProxyInstance {

        private Configuration configuration;
        private Thread thread;

        public JsonProxyInstance(final Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void open(final ProxyApplication proxyApplication) {
            final JsonProxy instance = new JsonProxy(configuration, proxyApplication);
            this.thread = new Thread(instance::run);
            this.thread.start();
        }

        @Override
        public void close() throws Exception {
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
        }
    }

    @Override
    public boolean supports(final Proxy proxy) {
        return "json".equals(proxy.getType());
    }

    @Override
    public ProxyInstance create(final Proxy proxy) {
        final Gson gson = new GsonBuilder().create();
        final Configuration configuration = gson.fromJson(proxy.getConfiguration(), Configuration.class);
        return new JsonProxyInstance(configuration);
    }

}

package no.nav.foreldrepenger.tilganger.server;

import org.slf4j.bridge.SLF4JBridgeHandler;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.tilganger.server.api.ApiConfig;
import no.nav.foreldrepenger.tilganger.server.internal.InternalApiConfig;
import no.nav.vedtak.server.jetty.JettyServerBuilder;

public class JettyServer {

    private static final Environment ENV = Environment.current();

    private static final String CONTEXT_PATH = ENV.getProperty("context.path", "/fptilgang");

    private final Integer serverPort;

    JettyServer(int serverPort) {
        this.serverPort = serverPort;
    }

    static void main() throws Exception {
        jettyServer().bootStrap();
    }

    private static JettyServer jettyServer() {
        return new JettyServer(ENV.getProperty("server.port", Integer.class, 8080));
    }

    void bootStrap() throws Exception {
        konfigurerLogging();
        start();
    }

    /**
     * Vi bruker SLF4J + logback, Jersey brukes JUL for logging.
     * Setter opp en bridge til å få Jersey til å logge gjennom Logback også.
     */
    private void konfigurerLogging() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private void start() throws Exception {
        var server = JettyServerBuilder.builder()
            .port(serverPort)
            .contextPath(CONTEXT_PATH)
            .addEventListener(new ServiceStarterListener())
            .registerRestApp(InternalApiConfig.API_URI, InternalApiConfig.class)
            .registerRestApp(ApiConfig.API_URI, ApiConfig.class)
            .build();
        server.start();
        server.join();
    }
}

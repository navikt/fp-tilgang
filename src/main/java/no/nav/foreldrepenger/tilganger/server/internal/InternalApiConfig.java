package no.nav.foreldrepenger.tilganger.server.internal;

import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath(InternalApiConfig.API_URI)
public class InternalApiConfig extends Application {

    public static final String API_URI = "/internal";

    public InternalApiConfig() {
        // CDI
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(HealtCheckRest.class, PrometheusRestService.class);
    }

}

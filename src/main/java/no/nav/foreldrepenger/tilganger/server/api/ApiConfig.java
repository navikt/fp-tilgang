package no.nav.foreldrepenger.tilganger.server.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import org.glassfish.jersey.server.ServerProperties;

import no.nav.foreldrepenger.tilganger.tjenester.ansatt.AnsattInfoRestTjeneste;
import no.nav.foreldrepenger.tilganger.tjenester.populasjon.PopulasjonRestTjeneste;
import no.nav.foreldrepenger.tilganger.tjenester.ruting.RutingRestTjeneste;
import no.nav.vedtak.server.rest.FpRestJackson2Feature;

@ApplicationPath(ApiConfig.API_URI)
public class ApiConfig extends Application {

    public static final String API_URI = "/api";

    @Override
    public Set<Class<?>> getClasses() {
        // eksponert grensesnitt bak sikkerhet
        return Set.of(AuthorizationAbacFilter.class,
            FpRestJackson2Feature.class,
            AnsattInfoRestTjeneste.class,
            PopulasjonRestTjeneste.class,
            RutingRestTjeneste.class);
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }

}

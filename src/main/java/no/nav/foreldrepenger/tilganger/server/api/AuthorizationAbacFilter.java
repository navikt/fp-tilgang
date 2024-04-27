package no.nav.foreldrepenger.tilganger.server.api;

import java.lang.reflect.Method;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationAbacFilter implements ContainerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationAbacFilter.class);

    @Context
    private ResourceInfo resourceinfo;

    public AuthorizationAbacFilter() {
        // Ingenting
    }

    @Override
    public void filter(ContainerRequestContext req) {
        Method method = resourceinfo.getResourceMethod();
        var utenAutentisering = method.getAnnotation(UtenAutentisering.class);
        if (utenAutentisering != null) {
            LOG.debug("{} er whitelistet.", method.getName());
            return;
        }
        if (!KontekstHolder.harKontekst() || !KontekstHolder.getKontekst().getIdentType().erSystem()) {
            throw new ManglerTilgangException("MANGLER-TILGANG", "Bruker kall er ikke tillatt. Kun system kall er mulig.");
        }
    }

}

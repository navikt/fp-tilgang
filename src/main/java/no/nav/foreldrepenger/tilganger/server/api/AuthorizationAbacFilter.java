package no.nav.foreldrepenger.tilganger.server.api;

import java.lang.reflect.Method;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

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
        if (!KontekstHolder.harKontekst() || Set.of(IdentType.InternBruker, IdentType.Systemressurs).stream().noneMatch(it -> it.equals(KontekstHolder.getKontekst().getIdentType()))) {
            throw new ManglerTilgangException("MANGLER-TILGANG", "Kun gyldige bruker kall er tillatt.");
        }
    }

}

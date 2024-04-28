package no.nav.foreldrepenger.tilganger.tjenester;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import no.nav.foreldrepenger.tilganger.domene.BrukerInformasjonTjeneste;

@RequestScoped
@Path("/bruker")
public class BrukerInformasjonRestTjeneste {

    private BrukerInformasjonTjeneste brukerInformasjonTjeneste;

    BrukerInformasjonRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public BrukerInformasjonRestTjeneste(BrukerInformasjonTjeneste tjeneste) {
        this.brukerInformasjonTjeneste = tjeneste;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/fraKontekst")
    public Response meUser() {
        return Response.ok(brukerInformasjonTjeneste.hentBrukerinformasjon()).build();
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/informasjon")
    public Response finnUser(@NotNull @QueryParam("ident") String ident) {
        return Response.ok(brukerInformasjonTjeneste.hentBrukerinformasjon(ident)).build();
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/informasjon/v2")
    public Response finnUserV2(@NotNull @QueryParam("oid") UUID oid) {
        return Response.ok(brukerInformasjonTjeneste.hentBrukerinformasjon(oid)).build();
    }
}

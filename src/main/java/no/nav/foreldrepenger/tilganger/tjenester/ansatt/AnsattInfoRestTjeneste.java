package no.nav.foreldrepenger.tilganger.tjenester.ansatt;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.tilganger.domene.ansatt.Ansatt;
import no.nav.foreldrepenger.tilganger.domene.ansatt.AnsattTjeneste;
import no.nav.foreldrepenger.tilganger.domene.ansatt.GrupperTjeneste;
import no.nav.vedtak.felles.integrasjon.ansatt.AnsattInfoDto;
import no.nav.vedtak.felles.integrasjon.ansatt.GrupperDto;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

/**
 * Brukes av applikasjoner som skal finne informasjon om en ansatt, eller har behov for å sjekke medlemskap i AD-grupper.
 * Kontrakter i fp-felles / tilgang-klient
 */
@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Path("/ansattinfo")
public class AnsattInfoRestTjeneste {

    private GrupperTjeneste grupperTjeneste;
    private AnsattTjeneste ansattTjeneste;

    AnsattInfoRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public AnsattInfoRestTjeneste(GrupperTjeneste tjeneste, AnsattTjeneste ansattTjeneste) {
        this.grupperTjeneste = tjeneste;
        this.ansattTjeneste = ansattTjeneste;
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/ansatt-ident")
    public AnsattInfoDto.Respons finnFraAnsattIdent(@NotNull @Valid AnsattInfoDto.IdentRequest request) {
        validerSystemKontekst();
        return ansattTjeneste.finnAnsatt(request.ansattIdent()).map(AnsattInfoRestTjeneste::mapTilProfilDto).orElse(null);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/ansatt-oid")
    public AnsattInfoDto.Respons hentAnsattOid(@NotNull @Valid AnsattInfoDto.OidRequest request) {
        validerSystemKontekst();
        return ansattTjeneste.hentAnsatt(request.ansattOid()).map(AnsattInfoRestTjeneste::mapTilProfilDto).orElse(null);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/refresh-ansatt-ident")
    public AnsattInfoDto.Respons refreshAnsattIdent(@NotNull @Valid AnsattInfoDto.IdentRequest request) {
        validerSystemKontekst();
        return ansattTjeneste.refreshAnsatt(request.ansattIdent()).map(AnsattInfoRestTjeneste::mapTilProfilDto).orElse(null);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/refresh-ansatt-oid")
    public AnsattInfoDto.Respons refreshAnsattOid(@NotNull @Valid AnsattInfoDto.OidRequest request) {
        validerSystemKontekst();
        return ansattTjeneste.refreshAnsatt(request.ansattOid()).map(AnsattInfoRestTjeneste::mapTilProfilDto).orElse(null);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/grupper-medlem")
    public GrupperDto.Respons grupperAlle(@NotNull @Valid GrupperDto.MedlemRequest gruppeDto) {
        validerSystemKontekst();
        return new GrupperDto.Respons(grupperTjeneste.alleGrupperForAnsatt(gruppeDto.ansattOid()));
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/grupper-filter")
    public GrupperDto.Respons grupperMedlemskap(@NotNull @Valid GrupperDto.FilterRequest gruppeDto) {
        validerSystemKontekst();
        return new GrupperDto.Respons(grupperTjeneste.filtrertGrupperforAnsatt(gruppeDto.ansattOid(), gruppeDto.grupper()));
    }

    private static AnsattInfoDto.Respons mapTilProfilDto(Ansatt ansatt) {
        return new AnsattInfoDto.Respons(ansatt.uid(), ansatt.ident(), ansatt.navn(), ansatt.ansattVedEnhetId());
    }


    private static void validerSystemKontekst() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!KontekstHolder.getKontekst().getIdentType().erSystem()) {
            throw new WebApplicationException("Trenger en gyldig CC token.", Response.Status.FORBIDDEN);
        }
    }




}

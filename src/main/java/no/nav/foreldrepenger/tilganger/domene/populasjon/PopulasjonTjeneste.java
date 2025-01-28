package no.nav.foreldrepenger.tilganger.domene.populasjon;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.tilganger.domene.ansatt.AnsattTjeneste;
import no.nav.vedtak.felles.integrasjon.pdlpip.PersondataPipDto;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppeProvider;

/**
 * Brukes av applikasjoner som skal gjøre tilgangskontroll for internbruker eller eksternbruker
 */
@ApplicationScoped
public class PopulasjonTjeneste {

    private static final AnsattGruppeProvider PROVIDER = AnsattGruppeProvider.instance();
    private static final Set<UUID> ALLE_ANSATTGRUPPE_OIDS = Arrays.stream(AnsattGruppe.values())
        .map(PROVIDER::getAnsattGruppeOid)
        .collect(Collectors.toSet());


    private PopulasjonCache populasjonCache;
    private AnsattTjeneste ansattTjeneste;

    PopulasjonTjeneste() {
        // CDI proxy
    }

    @Inject
    public PopulasjonTjeneste(PopulasjonCache populasjonCache, AnsattTjeneste ansattTjeneste) {
        this.populasjonCache = populasjonCache;
        this.ansattTjeneste = ansattTjeneste;
    }


    public TilgangVurdering vurderInternBruker(UUID ansattOID, Set<String> personIdenter, Set<String> aktørIdenter) {
        Set<AnsattGruppe> nødvendigeGrupper = new HashSet<>();

        // Finn behov for ekstra AD-grupper for tilfelle av adressebeskyttelse eller skjerming
        var allePersonPips = populasjonCache.finnPdlPipFor(personIdenter, aktørIdenter);
        if (allePersonPips.stream().anyMatch(PersondataPipDto::harStrengAdresseBeskyttelse)) {
            nødvendigeGrupper.add(AnsattGruppe.STRENGTFORTROLIG);
        } else if (allePersonPips.stream().anyMatch(PersondataPipDto::harAdresseBeskyttelse)) {
            nødvendigeGrupper.add(AnsattGruppe.FORTROLIG);
        }
        if (populasjonCache.finnSkjermingFor(allePersonPips)) {
           nødvendigeGrupper.add(AnsattGruppe.SKJERMET);
        }

        // Tidlig godkjenning dersom det ikke er behov for særskilte AD-grupper, ellers hent den ansattes grupper
        if (nødvendigeGrupper.isEmpty()) {
            return TilgangVurdering.godkjenn();
        }
        var gruppeOids = ansattTjeneste.hentGrupper(ansattOID, ALLE_ANSATTGRUPPE_OIDS);
        var harAnsattGrupper = PROVIDER.getAnsattGrupperFra(gruppeOids);

        // Sjekk om den ansatte er med i nødvendige grupper
        if (nødvendigeGrupper.contains(AnsattGruppe.STRENGTFORTROLIG) && !harAnsattGrupper.contains(AnsattGruppe.STRENGTFORTROLIG)) {
            return TilgangVurdering.avslå(TilgangResultat.AVSLÅTT_KODE_6);
        } else if (nødvendigeGrupper.contains(AnsattGruppe.FORTROLIG) && !harAnsattGrupper.contains(AnsattGruppe.FORTROLIG)) {
            return TilgangVurdering.avslå(TilgangResultat.AVSLÅTT_KODE_7);
        } else if (nødvendigeGrupper.contains(AnsattGruppe.SKJERMET) && !harAnsattGrupper.contains(AnsattGruppe.SKJERMET)) {
            return TilgangVurdering.avslå(TilgangResultat.AVSLÅTT_EGEN_ANSATT);
        } else {
            return TilgangVurdering.godkjenn();
        }
    }

    public TilgangVurdering vurderEksternBruker(String subjectPersonIdent, Set<String> personIdenter, Set<String> aktørIdenter) {
        var subjectPipOpt = populasjonCache.finnPdlPipFor(Set.of(subjectPersonIdent), Set.of()).stream().findFirst();
        if (subjectPipOpt.isEmpty()) {
            return TilgangVurdering.avslåGenerell("Finner ikke innlogget bruker");
        }
        var subjectPip = subjectPipOpt.get();
        if (subjectPip.erIkkeMyndig()) {
            return TilgangVurdering.avslåGenerell("Ikke gammel nok");
        }
        var subjectAktørId = subjectPip.aktørId();
        if (personIdenter.size() == 1 && personIdenter.stream().anyMatch(pi -> Objects.equals(pi, subjectPersonIdent))) {
            return TilgangVurdering.godkjenn();
        }
        if (aktørIdenter.size() == 1 && aktørIdenter.stream().anyMatch(ai -> Objects.equals(ai, subjectAktørId))) {
            return TilgangVurdering.godkjenn();
        }
        return TilgangVurdering.avslåGenerell("Har bare tilgang til seg selv");
    }
}

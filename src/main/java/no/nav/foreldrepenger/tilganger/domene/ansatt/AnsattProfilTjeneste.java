package no.nav.foreldrepenger.tilganger.domene.ansatt;


import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppeProvider;

@Dependent
public class AnsattProfilTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AnsattProfilTjeneste.class);
    private static final Environment ENV = Environment.current();
    private static final AnsattGruppeProvider PROVIDER = AnsattGruppeProvider.instance();
    private static final Set<UUID> ALLE_ANSATTGRUPPE_OIDS = Arrays.stream(AnsattGruppe.values())
        .map(PROVIDER::getAnsattGruppeOid)
        .collect(Collectors.toSet());

    private AnsattTjeneste ansattTjeneste;

    public AnsattProfilTjeneste() {
        // CDI proxy
    }

    @Inject
    public AnsattProfilTjeneste(AnsattTjeneste ansattTjeneste) {
        this.ansattTjeneste = ansattTjeneste;
    }

    /**
     * Henter informasjon for bruker logget inn i kontekst.
     * Trenger en gyldig OBO azure token.
     */
    public AnsattProfil hentProfil() {
        var ansatt = ansattTjeneste.hentAnsattFraKontekst().orElseThrow();
        var grupper = ansattTjeneste.hentGrupperFraKontekst(ALLE_ANSATTGRUPPE_OIDS);
        return mapAnsattProfil(ansatt, grupper);
    }

    public Set<AnsattGruppe> medlemAvGrupper(Set<AnsattGruppe> ansattGrupper) {
        var gruppeOidsForAnsatt = ansattTjeneste.hentGrupperFraKontekst(ALLE_ANSATTGRUPPE_OIDS);
        var grupperForAnsatt = PROVIDER.getAnsattGrupperFra(gruppeOidsForAnsatt);
        if (ENV.isDev()) {
            LOG.info("FPTILGRUPPE Request: {}", ansattGrupper);
            LOG.info("FPTILGRUPPE Fant gruppe-oids: {}", gruppeOidsForAnsatt);
            LOG.info("FPTILGRUPPE Fant gruppe-enums: {}", grupperForAnsatt);
        }
        return grupperForAnsatt.stream().filter(ansattGrupper::contains).collect(Collectors.toSet());
    }

    private AnsattProfil getAnsattProfil(Ansatt ansatt) {
        if (ansatt == null) {
            throw new IllegalStateException("Ingen bruker oppgitt");
        }
        var grupper = ansattTjeneste.hentGrupper(ansatt, ALLE_ANSATTGRUPPE_OIDS);
        return mapAnsattProfil(ansatt, grupper);
    }

    private AnsattProfil mapAnsattProfil(Ansatt ansatt, List<UUID> grupper) {
        var ansattGrupper = PROVIDER.getAnsattGrupperFra(grupper);
        return new AnsattProfil(ansatt.ident(), ansatt.navn(), ansatt.ansattVedEnhetId(), ansattGrupper);
    }


}

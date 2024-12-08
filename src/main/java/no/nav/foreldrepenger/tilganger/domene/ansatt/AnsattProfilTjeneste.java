package no.nav.foreldrepenger.tilganger.domene.ansatt;


import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppeProvider;

@Dependent
public class AnsattProfilTjeneste {
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

    public boolean medlemAvGruppe(AnsattGruppe ansattGruppe) {
        var gruppeOid = PROVIDER.getAnsattGruppeOid(ansattGruppe);
        return gruppeOid != null && ansattTjeneste.hentGrupperFraKontekst(ALLE_ANSATTGRUPPE_OIDS).stream().anyMatch(gruppeOid::equals);
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
        return new AnsattProfil.Builder(ansatt.ident(), ansatt.navn(), ansatt.ansattVedEnhetId())
            .medAnsattGrupper(ansattGrupper)
            .kanSaksbehandle(grupper.contains(PROVIDER.getAnsattGruppeOid(AnsattGruppe.SAKSBEHANDLER)))
            .kanVeilede(grupper.contains(PROVIDER.getAnsattGruppeOid(AnsattGruppe.VEILEDER)))
            .kanBeslutte(grupper.contains(PROVIDER.getAnsattGruppeOid(AnsattGruppe.BESLUTTER)))
            .kanOverstyre(grupper.contains(PROVIDER.getAnsattGruppeOid(AnsattGruppe.OVERSTYRER)))
            .kanOppgavestyre(grupper.contains(PROVIDER.getAnsattGruppeOid(AnsattGruppe.OPPGAVESTYRER)))
            .kanBehandleKodeEgenAnsatt(grupper.contains(PROVIDER.getAnsattGruppeOid(AnsattGruppe.SKJERMET)))
            .kanBehandleKode6(grupper.contains(PROVIDER.getAnsattGruppeOid(AnsattGruppe.STRENGTFORTROLIG)))
            .kanBehandleKode7(grupper.contains(PROVIDER.getAnsattGruppeOid(AnsattGruppe.FORTROLIG)))
            .kanDrifte(grupper.contains(PROVIDER.getAnsattGruppeOid(AnsattGruppe.DRIFT)))
            .build();
    }


}

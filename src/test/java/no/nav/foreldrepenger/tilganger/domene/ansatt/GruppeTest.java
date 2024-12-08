package no.nav.foreldrepenger.tilganger.domene.ansatt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppeProvider;

class GruppeTest {

    protected static final UUID VEILEDER_GRUPPE_FRA_TEST_KONFIG = UUID.fromString("edfe14fe-9a34-4ecb-8840-536ac2bc2818"); //application-vtp.properties

    protected static final UUID SKJERMET_GRUPPE_FRA_TEST_KONFIG = UUID.fromString("63b3f84f-1ec5-444b-ad33-2ad2d3495da1"); //application-vtp.properties

    @Test
    void testAtDetErTiGrupperILista() {
        assertThat(AnsattGruppe.values()).hasSize(10);
    }

    @Test
    void testVeilederGruppe() { // Har satt oid fra felles / ressursfil
        var provider = AnsattGruppeProvider.instance();
        assertThat(provider.getAnsattGruppeOid(AnsattGruppe.VEILEDER)).isNotNull();
        assertThat(provider.getAnsattGruppeOid(AnsattGruppe.VEILEDER)).isEqualTo(VEILEDER_GRUPPE_FRA_TEST_KONFIG);
    }


    @Test
    void testSkjermetGruppe() { // Har satt oid fra lokal env
        var provider = AnsattGruppeProvider.instance();
        assertThat(provider.getAnsattGruppeOid(AnsattGruppe.SKJERMET)).isNotNull();
        assertThat(provider.getAnsattGruppeOid(AnsattGruppe.SKJERMET)).isEqualTo(SKJERMET_GRUPPE_FRA_TEST_KONFIG);
    }

    @Test
    void testAlleGrupperHarGyldigUUID() {
        var provider = AnsattGruppeProvider.instance();
        assertThat(AnsattGruppe.values()).allSatisfy(ansattgruppe ->  assertThat(provider.getAnsattGruppeOid(ansattgruppe)).isNotNull());
    }
}

package no.nav.foreldrepenger.tilganger.domene.ansatt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class GruppeTest {

    protected static final String VEILEDER_GRUPPE_FRA_TEST_KONFIG = "edfe14fe-9a34-4ecb-8840-536ac2bc2818"; //application-vtp.properties

    @Test
    void testAtDetErNiGrupperILista() {
        assertThat(Gruppe.getAlleGrupper()).hasSize(9);
    }

    @Test
    void testVeilederGruppe() {
        assertThat(Gruppe.VEILEDER).isNotNull();
        assertThat(Gruppe.VEILEDER.getId().toString()).isEqualTo(VEILEDER_GRUPPE_FRA_TEST_KONFIG);
    }

    @Test
    void testAlleGrupperHarGyldigUUID() {
        assertThat(Gruppe.getAlleGrupper()).allSatisfy(uuid -> UUID.fromString(uuid.toString()));
    }
}

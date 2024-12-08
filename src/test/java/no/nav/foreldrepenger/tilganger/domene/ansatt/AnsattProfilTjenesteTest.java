package no.nav.foreldrepenger.tilganger.domene.ansatt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppeProvider;

@ExtendWith(MockitoExtension.class)
class AnsattProfilTjenesteTest {

    private static final AnsattGruppeProvider PROVIDER = AnsattGruppeProvider.instance();
    private static final List<UUID> ALLE_ANSATTGRUPPE_OIDS = Arrays.stream(AnsattGruppe.values())
        .map(PROVIDER::getAnsattGruppeOid)
        .toList();

    @Mock
    private AnsattTjeneste ansattTjeneste;

    private AnsattProfilTjeneste ansattProfilTjeneste;

    @BeforeEach
    void setUp() {
        ansattProfilTjeneste = new AnsattProfilTjeneste(ansattTjeneste);
        Mockito.when(ansattTjeneste.hentGrupperFraKontekst(ArgumentMatchers.any())).thenReturn(ALLE_ANSATTGRUPPE_OIDS);
    }

    @Test
    void testAlleGrupper() {
        assertThat(ansattProfilTjeneste.medlemAvGrupper(new HashSet<>(Arrays.stream(AnsattGruppe.values()).toList()))).hasSize(ALLE_ANSATTGRUPPE_OIDS.size());
    }

    @Test
    void testNoenGrupper() {
        var oppgitt = Set.of(AnsattGruppe.SAKSBEHANDLER, AnsattGruppe.OVERSTYRER);
        assertThat(ansattProfilTjeneste.medlemAvGrupper(oppgitt)).hasSize(oppgitt.size());
        assertThat(ansattProfilTjeneste.medlemAvGrupper(oppgitt)).containsAll(oppgitt);
    }

    @Test
    void testHistoriskGrupper() {
        var oppgitt = Set.of(AnsattGruppe.HISTORISK);
        assertThat(ansattProfilTjeneste.medlemAvGrupper(oppgitt)).hasSize(oppgitt.size());
        assertThat(ansattProfilTjeneste.medlemAvGrupper(oppgitt)).containsAll(oppgitt);
    }

    @Test
    void testTomListeUtenGrupper() {
        assertThat(ansattProfilTjeneste.medlemAvGrupper(Set.of())).isEmpty();
    }
}

package no.nav.foreldrepenger.tilganger.domene.ansatt;

import no.nav.foreldrepenger.tilganger.domene.cache.GrupperCache;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.AzureGraph;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.Group;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GrupperTjenesteTest {

    private static final UUID ANSATT = UUID.randomUUID();

    private static final AnsattGruppeProvider PROVIDER = AnsattGruppeProvider.instance();
    private static final List<UUID> ALLE_ANSATTGRUPPE_OIDS = Arrays.stream(AnsattGruppe.values())
        .map(PROVIDER::getAnsattGruppeOid)
        .toList();

    @Mock
    private AzureGraph azureGraph;
    @Mock
    private GrupperCache grupperCache;

    private GrupperTjeneste grupperTjeneste;

    @BeforeEach
    void setUp() {
        var gruppeGroups = ALLE_ANSATTGRUPPE_OIDS.stream().map(Group::new).collect(Collectors.toSet());
        Mockito.when(azureGraph.hentGrupper(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(gruppeGroups);
        grupperTjeneste = new GrupperTjeneste(grupperCache, azureGraph);
    }

    @Test
    void testGrupperForAnsatt() {
        var alle = new HashSet<>(Arrays.stream(AnsattGruppe.values()).toList());
        assertThat(grupperTjeneste.alleGrupperForAnsatt(ANSATT)).containsExactlyInAnyOrderElementsOf(alle);
    }

    @Test
    void testAlleGrupper() {
        var alle = new HashSet<>(Arrays.stream(AnsattGruppe.values()).toList());
        assertThat(grupperTjeneste.filtrertGrupperforAnsatt(ANSATT, alle)).containsExactlyInAnyOrderElementsOf(alle);
    }

    @Test
    void testNoenGrupper() {
        var oppgitt = Set.of(AnsattGruppe.SAKSBEHANDLER, AnsattGruppe.OVERSTYRER);
        assertThat(grupperTjeneste.filtrertGrupperforAnsatt(ANSATT, oppgitt)).containsExactlyInAnyOrderElementsOf(oppgitt);
    }

    @Test
    void testHistoriskGrupper() {
        var oppgitt = Set.of(AnsattGruppe.HISTORISK);
        assertThat(grupperTjeneste.filtrertGrupperforAnsatt(ANSATT, oppgitt)).containsExactlyInAnyOrderElementsOf(oppgitt);
    }

    @Test
    void testTomListeUtenGrupper() {
        assertThat(grupperTjeneste.filtrertGrupperforAnsatt(ANSATT, Set.of())).isEmpty();
    }
}

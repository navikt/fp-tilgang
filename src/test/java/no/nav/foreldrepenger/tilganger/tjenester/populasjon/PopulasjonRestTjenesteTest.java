package no.nav.foreldrepenger.tilganger.tjenester.populasjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.tilganger.domene.populasjon.PopulasjonTjeneste;
import no.nav.foreldrepenger.tilganger.domene.populasjon.TilgangVurdering;
import no.nav.vedtak.felles.integrasjon.populasjon.PopulasjonDto;
import no.nav.vedtak.felles.integrasjon.populasjon.PopulasjonTilgangResultat;
import no.nav.vedtak.felles.integrasjon.sak.InvaliderSakRequest;
import no.nav.vedtak.felles.integrasjon.tilgangfilter.FilterDto;

@ExtendWith(MockitoExtension.class)
class PopulasjonRestTjenesteTest {

    private static final String AKTØR_ID = "1234567890121";
    private static final String AKTØR_ID_2 = "1234567890122";
    private static final String AKTØR_ID_3 = "1234567890123";

    private static final String PERSON_ID = "12345678901";
    private static final String PERSON_ID_2 = "12345678902";
    private static final String PERSON_ID_3 = "12345678903";

    private static final String SAK = "123456789";
    private static final String SAK_2 = "123456788";
    private static final String SAK_3 = "123456787";

    private static final UUID BEHANDLING = UUID.randomUUID();

    @Mock
    private PopulasjonTjeneste populasjonTjeneste;

    @Test
    void internMedAktør() {
        var request = lagInternRequest(Set.of(AKTØR_ID), null, null);
        when(populasjonTjeneste.validerBehandling(any(), any())).thenReturn(null);
        when(populasjonTjeneste.vurderInternBruker(any(), any(), any())).thenReturn(TilgangVurdering.godkjenn());
        when(populasjonTjeneste.utledInternAuditIdent(any(), any())).thenReturn(PERSON_ID);
        var tjeneste = new MockRest(populasjonTjeneste);
        var response = tjeneste.sjekkInternBruker(request);
        assertThat(response).isNotNull();
        assertThat(response.tilgangResultat()).isEqualTo(PopulasjonTilgangResultat.GODKJENT);
        assertThat(response.auditIdent()).isEqualTo(PERSON_ID);
    }

    @Test
    void internMedSak() {
        var request = lagInternRequest(Set.of(), SAK, null);
        when(populasjonTjeneste.validerBehandling(any(), eq(SAK))).thenReturn(SAK);
        when(populasjonTjeneste.vurderInternBruker(any(), any(), eq(SAK))).thenReturn(TilgangVurdering.godkjenn());
        when(populasjonTjeneste.utledInternAuditIdent(any(), eq(SAK))).thenReturn(PERSON_ID);
        var tjeneste = new MockRest(populasjonTjeneste);
        var response = tjeneste.sjekkInternBruker(request);
        assertThat(response).isNotNull();
        assertThat(response.tilgangResultat()).isEqualTo(PopulasjonTilgangResultat.GODKJENT);
        assertThat(response.auditIdent()).isEqualTo(PERSON_ID);
    }

    @Test
    void internMedBehandling() {
        var request = lagInternRequest(Set.of(), null, BEHANDLING);
        when(populasjonTjeneste.validerBehandling(eq(BEHANDLING), any())).thenReturn(SAK);
        when(populasjonTjeneste.vurderInternBruker(any(), any(), eq(SAK))).thenReturn(TilgangVurdering.godkjenn());
        when(populasjonTjeneste.utledInternAuditIdent(any(), eq(SAK))).thenReturn(PERSON_ID);
        var tjeneste = new MockRest(populasjonTjeneste);
        var response = tjeneste.sjekkInternBruker(request);
        assertThat(response).isNotNull();
        assertThat(response.tilgangResultat()).isEqualTo(PopulasjonTilgangResultat.GODKJENT);
        assertThat(response.auditIdent()).isEqualTo(PERSON_ID);
    }

    @Test
    void internMismatchSakBehandling() {
        var request = lagInternRequest(Set.of(), SAK_2, BEHANDLING);
        when(populasjonTjeneste.validerBehandling(BEHANDLING, SAK_2)).thenThrow(IllegalArgumentException.class);
        var tjeneste = new MockRest(populasjonTjeneste);
        var response = tjeneste.sjekkInternBruker(request);
        assertThat(response).isNotNull();
        assertThat(response.tilgangResultat()).isEqualTo(PopulasjonTilgangResultat.AVSLÅTT_ANNEN_ÅRSAK);
        assertThat(response.auditIdent()).isNull();
    }

    @Test
    void eksternUtenIdenter() {
        var request = lagEksternRequest(Set.of());
        when(populasjonTjeneste.vurderEksternBruker(eq(PERSON_ID), eq(18), any())).thenReturn(TilgangVurdering.godkjenn());
        when(populasjonTjeneste.utledEksternAuditIdent(any(), any())).thenReturn(PERSON_ID);
        var tjeneste = new MockRest(populasjonTjeneste);
        var response = tjeneste.sjekkEksternBruker(request);
        assertThat(response).isNotNull();
        assertThat(response.tilgangResultat()).isEqualTo(PopulasjonTilgangResultat.GODKJENT);
        assertThat(response.auditIdent()).isEqualTo(PERSON_ID);
    }

    @Test
    void eksternMedIdent() {
        var request = lagEksternRequest(Set.of(AKTØR_ID));
        when(populasjonTjeneste.vurderEksternBruker(PERSON_ID, 18, Set.of(AKTØR_ID))).thenReturn(TilgangVurdering.godkjenn());
        when(populasjonTjeneste.utledEksternAuditIdent(any(), any())).thenReturn(PERSON_ID);
        var tjeneste = new MockRest(populasjonTjeneste);
        var response = tjeneste.sjekkEksternBruker(request);
        assertThat(response).isNotNull();
        assertThat(response.tilgangResultat()).isEqualTo(PopulasjonTilgangResultat.GODKJENT);
        assertThat(response.auditIdent()).isEqualTo(PERSON_ID);
    }

    @Test
    void eksternMedIdentMismatch() {
        var request = lagEksternRequest(Set.of(AKTØR_ID_2));
        when(populasjonTjeneste.vurderEksternBruker(PERSON_ID, 18, Set.of(AKTØR_ID_2))).thenReturn(TilgangVurdering.avslåGenerell("feil"));
        when(populasjonTjeneste.utledEksternAuditIdent(any(), any())).thenReturn(PERSON_ID_2);
        var tjeneste = new MockRest(populasjonTjeneste);
        var response = tjeneste.sjekkEksternBruker(request);
        assertThat(response).isNotNull();
        assertThat(response.tilgangResultat()).isEqualTo(PopulasjonTilgangResultat.AVSLÅTT_ANNEN_ÅRSAK);
        assertThat(response.auditIdent()).isEqualTo(PERSON_ID_2);
    }

    @Test
    void filterIdent() {
        var request = new FilterDto.IdenterRequest(UUID.randomUUID(), Set.of(AKTØR_ID, AKTØR_ID_2, AKTØR_ID_3));
        when(populasjonTjeneste.vurderInternBruker(any(), any(), any())).thenReturn(TilgangVurdering.godkjenn());
        when(populasjonTjeneste.vurderInternBruker(any(), eq(Set.of(AKTØR_ID_2)), any())).thenReturn(TilgangVurdering.avslå(PopulasjonTilgangResultat.AVSLÅTT_EGEN_ANSATT));
        var tjeneste = new MockRest(populasjonTjeneste);
        var response = tjeneste.filterIdenter(request);
        assertThat(response).isNotNull();
        assertThat(response.harTilgang()).containsOnly(AKTØR_ID, AKTØR_ID_3);
    }

    @Test
    void filterMedSak() {
        var request = new FilterDto.SaksnummerRequest(UUID.randomUUID(), Set.of(SAK, SAK_2, SAK_3));
        when(populasjonTjeneste.vurderInternBruker(any(), any(), any())).thenReturn(TilgangVurdering.godkjenn());
        when(populasjonTjeneste.vurderInternBruker(any(), any(), eq(SAK))).thenReturn(TilgangVurdering.avslå(PopulasjonTilgangResultat.AVSLÅTT_KODE_7));
        var tjeneste = new MockRest(populasjonTjeneste);
        var response = tjeneste.filterSaksnummer(request);
        assertThat(response).isNotNull();
        assertThat(response.harTilgang()).containsOnly(SAK_2, SAK_3);
    }

    @Test
    void invaliderKallerTjeneste() {
        var request = new InvaliderSakRequest(SAK);
        var tjeneste = new MockRest(populasjonTjeneste);
        try (var response = tjeneste.invaliderSak(request)) {
            assertThat(response.getStatus()).isEqualTo(200);
            Mockito.verify(populasjonTjeneste).invaliderSak(SAK);
        }
    }

    private static class MockRest extends PopulasjonRestTjeneste {

        public MockRest(PopulasjonTjeneste tjeneste) {
            super(tjeneste);
        }

        @Override
        protected void validerSystemKontekst() {
            // Vil ikke sette opp kallkontekst
        }
    }

    private PopulasjonDto.InternRequest lagInternRequest(Set<String> identer, String saksnummer, UUID behandling) {
        return new PopulasjonDto.InternRequest(UUID.randomUUID(), identer, saksnummer, behandling);
    }

    private PopulasjonDto.EksternRequest lagEksternRequest(Set<String> identer) {
        return new PopulasjonDto.EksternRequest(PERSON_ID, identer, 18);
    }

}

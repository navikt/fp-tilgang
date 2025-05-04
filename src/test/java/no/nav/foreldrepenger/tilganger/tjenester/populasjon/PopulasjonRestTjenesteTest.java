package no.nav.foreldrepenger.tilganger.tjenester.populasjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import no.nav.foreldrepenger.tilganger.integrasjoner.pip.PersondataPipDto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.tilganger.domene.populasjon.PopulasjonTjeneste;
import no.nav.foreldrepenger.tilganger.domene.populasjon.TilgangResultat;
import no.nav.foreldrepenger.tilganger.domene.populasjon.TilgangVurdering;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppeProvider;

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

    private static final AnsattGruppeProvider PROVIDER = AnsattGruppeProvider.instance();

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
        assertThat(response.tilgangResultat()).isEqualTo(TilgangResultat.GODKJENT);
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
        assertThat(response.tilgangResultat()).isEqualTo(TilgangResultat.GODKJENT);
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
        assertThat(response.tilgangResultat()).isEqualTo(TilgangResultat.GODKJENT);
        assertThat(response.auditIdent()).isEqualTo(PERSON_ID);
    }

    @Test
    void internMismatchSakBehandling() {
        var request = lagInternRequest(Set.of(), SAK_2, BEHANDLING);
        when(populasjonTjeneste.validerBehandling(BEHANDLING, SAK_2)).thenThrow(IllegalArgumentException.class);
        var tjeneste = new MockRest(populasjonTjeneste);
        var response = tjeneste.sjekkInternBruker(request);
        assertThat(response).isNotNull();
        assertThat(response.tilgangResultat()).isEqualTo(TilgangResultat.AVSLÅTT_ANNEN_ÅRSAK);
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
        assertThat(response.tilgangResultat()).isEqualTo(TilgangResultat.GODKJENT);
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
        assertThat(response.tilgangResultat()).isEqualTo(TilgangResultat.GODKJENT);
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
        assertThat(response.tilgangResultat()).isEqualTo(TilgangResultat.AVSLÅTT_ANNEN_ÅRSAK);
        assertThat(response.auditIdent()).isEqualTo(PERSON_ID_2);
    }

    @Test
    void filterIdent() {
        var request = new PopulasjonRestTjeneste.FilterIdenterRequest(UUID.randomUUID(), Set.of(AKTØR_ID, AKTØR_ID_2, AKTØR_ID_3));
        when(populasjonTjeneste.vurderInternBruker(any(), any(), any())).thenReturn(TilgangVurdering.godkjenn());
        when(populasjonTjeneste.vurderInternBruker(any(), eq(Set.of(AKTØR_ID_2)), any())).thenReturn(TilgangVurdering.avslå(TilgangResultat.AVSLÅTT_EGEN_ANSATT));
        var tjeneste = new MockRest(populasjonTjeneste);
        var response = tjeneste.filterIdenter(request);
        assertThat(response).isNotNull();
        assertThat(response.harTilgang()).containsOnly(AKTØR_ID, AKTØR_ID_3);
    }

    @Test
    void filterMedSak() {
        var request = new PopulasjonRestTjeneste.FilterSaksnummerRequest(UUID.randomUUID(), Set.of(SAK, SAK_2, SAK_3));
        when(populasjonTjeneste.vurderInternBruker(any(), any(), any())).thenReturn(TilgangVurdering.godkjenn());
        when(populasjonTjeneste.vurderInternBruker(any(), any(), eq(SAK))).thenReturn(TilgangVurdering.avslå(TilgangResultat.AVSLÅTT_KODE_7));
        var tjeneste = new MockRest(populasjonTjeneste);
        var response = tjeneste.filterSaksnummer(request);
        assertThat(response).isNotNull();
        assertThat(response.harTilgang()).containsOnly(SAK_2, SAK_3);
    }

    @Test
    void invaliderKallerTjeneste() {
        var request = new PopulasjonRestTjeneste.SakRequest(SAK);
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

    private PopulasjonRestTjeneste.PopulasjonInternRequest lagInternRequest(Set<String> identer, String saksnummer, UUID behandling) {
        return new PopulasjonRestTjeneste.PopulasjonInternRequest(UUID.randomUUID(), identer, saksnummer, behandling);
    }

    private PopulasjonRestTjeneste.PopulasjonEksternRequest lagEksternRequest(Set<String> identer) {
        return new PopulasjonRestTjeneste.PopulasjonEksternRequest(PERSON_ID, identer, 18);
    }

    private PersondataPipDto lagPip(String personIdent, String aktørIdent, PersondataPipDto.Gradering gradering) {
        return lagPip(personIdent, aktørIdent, gradering, null);
    }


    private PersondataPipDto lagPip(String personIdent, String aktørIdent, PersondataPipDto.Gradering gradering, LocalDate fødselsdato) {
        var fødsel = Optional.ofNullable(fødselsdato).map(PersondataPipDto.Fødsel::new).map(List::of).orElseGet(List::of);
        return new PersondataPipDto(aktørIdent,
            new PersondataPipDto.Person(List.of(new PersondataPipDto.Adressebeskyttelse(gradering)), fødsel, List.of(), List.of()),
            new PersondataPipDto.Identer(List.of(
                new PersondataPipDto.Ident(personIdent, false, PersondataPipDto.IdentGruppe.FOLKEREGISTERIDENT),
                new PersondataPipDto.Ident(aktørIdent, false, PersondataPipDto.IdentGruppe.AKTORID))),
            new PersondataPipDto.GeografiskTilknytning(PersondataPipDto.GtType.KOMMUNE, "a", "b", "c", "d"));
    }

    private List<UUID> getGrupper(AnsattGruppe... ansattGruppe) {
        List<UUID> resultat = new ArrayList<>();
        resultat.add(PROVIDER.getAnsattGruppeOid(AnsattGruppe.SAKSBEHANDLER));
        Arrays.stream(ansattGruppe).forEach(a -> resultat.add(PROVIDER.getAnsattGruppeOid(a)));
        return resultat;
    }


}

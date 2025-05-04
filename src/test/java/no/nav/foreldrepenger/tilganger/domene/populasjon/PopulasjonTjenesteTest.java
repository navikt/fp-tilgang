package no.nav.foreldrepenger.tilganger.domene.populasjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.tilganger.domene.ansatt.GrupperTjeneste;
import no.nav.foreldrepenger.tilganger.integrasjoner.pip.PersondataPipDto;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;

@ExtendWith(MockitoExtension.class)
class PopulasjonTjenesteTest {

    private static final String AKTØR_ID = "1234567890121";
    private static final String AKTØR_ID_2 = "1234567890122";
    private static final String AKTØR_ID_3 = "1234567890123";

    private static final String PERSON_ID = "12345678901";
    private static final String PERSON_ID_2 = "12345678902";
    private static final String PERSON_ID_3 = "12345678903";

    @Mock
    private GrupperTjeneste grupperTjeneste;
    @Mock
    private PopulasjonCache populasjonCache;
    @Mock
    private SakTjeneste sakTjeneste;


    @Test
    void happy_case_intern_en_ubeskyttet_person() {
        var saksbehandler = UUID.randomUUID();
        var pip = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(false);
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(AKTØR_ID), null);

        assertThat(vurdering.fikkTilgang()).isTrue();
    }

    @Test
    void intern_en_skjermet_person_ikke_tilgang() {
        var saksbehandler = UUID.randomUUID();
        var pip = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(true);
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of());
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(AKTØR_ID), null);

        assertThat(vurdering.fikkTilgang()).isFalse();
    }

    @Test
    void intern_en_skjermet_person_tilgang() {
        var saksbehandler = UUID.randomUUID();
        var pip = lagPip(PERSON_ID_2, AKTØR_ID_2, PersondataPipDto.Gradering.UDEFINERT);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(true);
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.SKJERMET));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(AKTØR_ID), null);

        assertThat(vurdering.fikkTilgang()).isTrue();
    }

    @Test
    void intern_en_fortrolig_person_ikke_tilgang() {
        var saksbehandler = UUID.randomUUID();
        var pip1 = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        var pip3 = lagPip(PERSON_ID_3, AKTØR_ID_3, PersondataPipDto.Gradering.FORTROLIG);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip1, pip3));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(false);
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of());
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(AKTØR_ID, AKTØR_ID_3), null);

        assertThat(vurdering.fikkTilgang()).isFalse();
    }

    @Test
    void intern_en_fortrolig_person_tilgang() {
        var saksbehandler = UUID.randomUUID();
        var pip1 = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        var pip2 = lagPip(PERSON_ID_2, AKTØR_ID_2, PersondataPipDto.Gradering.UDEFINERT);
        var pip3 = lagPip(PERSON_ID_3, AKTØR_ID_3, PersondataPipDto.Gradering.FORTROLIG);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip1, pip2, pip3));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(false);
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.FORTROLIG));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(AKTØR_ID, AKTØR_ID_3, AKTØR_ID_2), null);

        assertThat(vurdering.fikkTilgang()).isTrue();
    }


    @Test
    void intern_en_strengt_fortrolig_person_ikke_tilgang() {
        var saksbehandler = UUID.randomUUID();
        var pip1 = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        var pip3 = lagPip(PERSON_ID_3, AKTØR_ID_3, PersondataPipDto.Gradering.STRENGT_FORTROLIG);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip1, pip3));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(false);
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of());
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(AKTØR_ID, AKTØR_ID_3), null);

        assertThat(vurdering.fikkTilgang()).isFalse();
    }

    @Test
    void intern_en_strengt_fortrolig_person_ikke_tilgang_fotrolig() {
        var saksbehandler = UUID.randomUUID();
        var pip1 = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        var pip2 = lagPip(PERSON_ID_2, AKTØR_ID_2, PersondataPipDto.Gradering.STRENGT_FORTROLIG);
        var pip3 = lagPip(PERSON_ID_3, AKTØR_ID_3, PersondataPipDto.Gradering.FORTROLIG);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip1, pip2, pip3));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(false);
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.FORTROLIG));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(AKTØR_ID, AKTØR_ID_3), null);

        assertThat(vurdering.fikkTilgang()).isFalse();
    }

    @Test
    void intern_en_strengt_fortrolig_person_tilgang() {
        var saksbehandler = UUID.randomUUID();
        var pip1 = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        var pip2 = lagPip(PERSON_ID_2, AKTØR_ID_2, PersondataPipDto.Gradering.STRENGT_FORTROLIG);
        var pip3 = lagPip(PERSON_ID_3, AKTØR_ID_3, PersondataPipDto.Gradering.FORTROLIG);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip1, pip2, pip3));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(false);
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.STRENGTFORTROLIG));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(AKTØR_ID, AKTØR_ID_3, AKTØR_ID_2), null);

        assertThat(vurdering.fikkTilgang()).isTrue();
    }


    @Test
    void intern_combo_person_ikke_tilgang_fortrolig() {
        var saksbehandler = UUID.randomUUID();
        var pip1 = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        var pip2 = lagPip(PERSON_ID_2, AKTØR_ID_2, PersondataPipDto.Gradering.STRENGT_FORTROLIG);
        var pip3 = lagPip(PERSON_ID_3, AKTØR_ID_3, PersondataPipDto.Gradering.FORTROLIG);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip1, pip2, pip3));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(true);
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.FORTROLIG));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(AKTØR_ID, AKTØR_ID_3), null);

        assertThat(vurdering.fikkTilgang()).isFalse();
    }

    @Test
    void intern_combo_person_ikke_tilgang_skjermet() {
        var saksbehandler = UUID.randomUUID();
        var pip1 = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        var pip2 = lagPip(PERSON_ID_2, AKTØR_ID_2, PersondataPipDto.Gradering.STRENGT_FORTROLIG);
        var pip3 = lagPip(PERSON_ID_3, AKTØR_ID_3, PersondataPipDto.Gradering.FORTROLIG);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip1, pip2, pip3));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(true);
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.SKJERMET));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(AKTØR_ID, AKTØR_ID_3), null);

        assertThat(vurdering.fikkTilgang()).isFalse();
    }

    @Test
    void intern_combo_person_tilgang() {
        var saksbehandler = UUID.randomUUID();
        var pip1 = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        var pip2 = lagPip(PERSON_ID_2, AKTØR_ID_2, PersondataPipDto.Gradering.STRENGT_FORTROLIG);
        var pip3 = lagPip(PERSON_ID_3, AKTØR_ID_3, PersondataPipDto.Gradering.FORTROLIG);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip1, pip2, pip3));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(true);
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.STRENGTFORTROLIG, AnsattGruppe.SKJERMET));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(AKTØR_ID, AKTØR_ID_3, AKTØR_ID_2), null);

        assertThat(vurdering.fikkTilgang()).isTrue();
    }

    @Test
    void ekstern_seg_selv_aktør() {
        var pip = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderEksternBruker(PERSON_ID, null, Set.of(AKTØR_ID));

        assertThat(vurdering.fikkTilgang()).isTrue();
    }

    @Test
    void ekstern_seg_selv_fnr() {
        var pip = lagPip(PERSON_ID_3, AKTØR_ID_3, PersondataPipDto.Gradering.UDEFINERT);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderEksternBruker(PERSON_ID_3, null, Set.of(PERSON_ID_3));

        assertThat(vurdering.fikkTilgang()).isTrue();
    }

    @Test
    void ekstern_seg_selv_begge() {
        var pip = lagPip(PERSON_ID_2, AKTØR_ID_2, PersondataPipDto.Gradering.UDEFINERT);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderEksternBruker(PERSON_ID_2, null, Set.of(PERSON_ID_2, AKTØR_ID_2));

        assertThat(vurdering.fikkTilgang()).isFalse(); // Må velge aktørid eller fnr/dnr
    }

    @Test
    void ekstern_seg_annen_aktør() {
        var pip = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderEksternBruker(PERSON_ID, null, Set.of(AKTØR_ID_2));

        assertThat(vurdering.fikkTilgang()).isFalse();
    }

    @Test
    void ekstern_seg_annen_fnr() {
        var pip = lagPip(PERSON_ID_3, AKTØR_ID_3, PersondataPipDto.Gradering.UDEFINERT);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderEksternBruker(PERSON_ID_3, null, Set.of(PERSON_ID_2));

        assertThat(vurdering.fikkTilgang()).isFalse();
    }

    @Test
    void ekstern_seg_selv_flere() {
        var pip = lagPip(PERSON_ID_2, AKTØR_ID_2, PersondataPipDto.Gradering.UDEFINERT);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderEksternBruker(PERSON_ID_2, null, Set.of(AKTØR_ID_2, AKTØR_ID_3));

        assertThat(vurdering.fikkTilgang()).isFalse();
    }

    @Test
    void ekstern_seg_selv_25() {
        var pip = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT, LocalDate.now().minusYears(25));
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderEksternBruker(PERSON_ID, null, Set.of(AKTØR_ID));

        assertThat(vurdering.fikkTilgang()).isTrue();
    }

    @Test
    void ekstern_seg_selv_25_skriv() {
        var pip = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT, LocalDate.now().minusYears(25));
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderEksternBruker(PERSON_ID, 18, Set.of(AKTØR_ID));

        assertThat(vurdering.fikkTilgang()).isTrue();
    }

    @Test
    void ekstern_seg_selv_17_skriv() {
        var pip = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT, LocalDate.now().minusYears(17));
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderEksternBruker(PERSON_ID, 18, Set.of(AKTØR_ID));

        assertThat(vurdering.fikkTilgang()).isFalse();
    }

    @Test
    void ekstern_seg_selv_17_les() {
        var pip = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT, LocalDate.now().minusYears(17));
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderEksternBruker(PERSON_ID, 15, Set.of(AKTØR_ID));

        assertThat(vurdering.fikkTilgang()).isTrue();
    }

    @Test
    void ekstern_seg_selv_14_les() {
        var pip = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT, LocalDate.now().minusYears(14));
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderEksternBruker(PERSON_ID, 15, Set.of(AKTØR_ID));

        assertThat(vurdering.fikkTilgang()).isFalse();
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

}

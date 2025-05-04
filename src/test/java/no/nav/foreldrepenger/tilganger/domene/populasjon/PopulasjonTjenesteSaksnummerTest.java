package no.nav.foreldrepenger.tilganger.domene.populasjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
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
class PopulasjonTjenesteSaksnummerTest {

    private static final String SAKSNUMMER = "123456789";

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
        Mockito.when(sakTjeneste.identerForSak(any())).thenReturn(Set.of(AKTØR_ID));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(), SAKSNUMMER);

        assertThat(vurdering.fikkTilgang()).isTrue();
    }

    @Test
    void intern_en_skjermet_person_ikke_tilgang() {
        var saksbehandler = UUID.randomUUID();
        var pip = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(true);
        Mockito.when(sakTjeneste.identerForSak(any())).thenReturn(Set.of(AKTØR_ID));
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of());
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(), SAKSNUMMER);

        assertThat(vurdering.fikkTilgang()).isFalse();
    }

    @Test
    void intern_en_skjermet_person_tilgang() {
        var saksbehandler = UUID.randomUUID();
        var pip = lagPip(PERSON_ID_2, AKTØR_ID_2, PersondataPipDto.Gradering.UDEFINERT);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(true);
        Mockito.when(sakTjeneste.identerForSak(any())).thenReturn(Set.of(AKTØR_ID));
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.SKJERMET));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(), SAKSNUMMER);

        assertThat(vurdering.fikkTilgang()).isTrue();
    }

    @Test
    void intern_en_fortrolig_person_ikke_tilgang() {
        var saksbehandler = UUID.randomUUID();
        var pip1 = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        var pip3 = lagPip(PERSON_ID_3, AKTØR_ID_3, PersondataPipDto.Gradering.FORTROLIG);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip1, pip3));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(false);
        Mockito.when(sakTjeneste.identerForSak(any())).thenReturn(Set.of(AKTØR_ID, AKTØR_ID_3));
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of());
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(), SAKSNUMMER);

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
        Mockito.when(sakTjeneste.identerForSak(any())).thenReturn(Set.of(AKTØR_ID, AKTØR_ID_3, AKTØR_ID_2));
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.FORTROLIG));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(), SAKSNUMMER);

        assertThat(vurdering.fikkTilgang()).isTrue();
    }


    @Test
    void intern_en_strengt_fortrolig_person_ikke_tilgang() {
        var saksbehandler = UUID.randomUUID();
        var pip1 = lagPip(PERSON_ID, AKTØR_ID, PersondataPipDto.Gradering.UDEFINERT);
        var pip3 = lagPip(PERSON_ID_3, AKTØR_ID_3, PersondataPipDto.Gradering.STRENGT_FORTROLIG);
        Mockito.when(populasjonCache.finnPdlPipFor(any())).thenReturn(Set.of(pip1, pip3));
        Mockito.when(populasjonCache.finnSkjermingFor(any())).thenReturn(false);
        Mockito.when(sakTjeneste.identerForSak(any())).thenReturn(Set.of(AKTØR_ID, AKTØR_ID_3));
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of());
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(), SAKSNUMMER);

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
        Mockito.when(sakTjeneste.identerForSak(any())).thenReturn(Set.of(AKTØR_ID, AKTØR_ID_3));
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.FORTROLIG));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(), SAKSNUMMER);

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
        Mockito.when(sakTjeneste.identerForSak(any())).thenReturn(Set.of(AKTØR_ID, AKTØR_ID_3, AKTØR_ID_2));
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.STRENGTFORTROLIG));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(), SAKSNUMMER);

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
        Mockito.when(sakTjeneste.identerForSak(any())).thenReturn(Set.of(AKTØR_ID, AKTØR_ID_3));
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.FORTROLIG));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(), SAKSNUMMER);

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
        Mockito.when(sakTjeneste.identerForSak(any())).thenReturn(Set.of(AKTØR_ID, AKTØR_ID_3));
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.SKJERMET));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(), SAKSNUMMER);

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
        Mockito.when(sakTjeneste.identerForSak(any())).thenReturn(Set.of(AKTØR_ID, AKTØR_ID_3, AKTØR_ID_2));
        Mockito.when(grupperTjeneste.alleGrupperForAnsatt(any())).thenReturn(Set.of(AnsattGruppe.STRENGTFORTROLIG, AnsattGruppe.SKJERMET));
        var populasjonTjeneste = new PopulasjonTjeneste(populasjonCache, sakTjeneste, grupperTjeneste);

        var vurdering = populasjonTjeneste.vurderInternBruker(saksbehandler, Set.of(), SAKSNUMMER);

        assertThat(vurdering.fikkTilgang()).isTrue();
    }

    private PersondataPipDto lagPip(String personIdent, String aktørIdent, PersondataPipDto.Gradering gradering) {
        return new PersondataPipDto(aktørIdent,
            new PersondataPipDto.Person(List.of(new PersondataPipDto.Adressebeskyttelse(gradering)), List.of(), List.of(), List.of()),
            new PersondataPipDto.Identer(List.of(
                new PersondataPipDto.Ident(personIdent, false, PersondataPipDto.IdentGruppe.FOLKEREGISTERIDENT),
                new PersondataPipDto.Ident(aktørIdent, false, PersondataPipDto.IdentGruppe.AKTORID))),
            new PersondataPipDto.GeografiskTilknytning(PersondataPipDto.GtType.KOMMUNE, "a", "b", "c", "d"));
    }

}

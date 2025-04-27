package no.nav.foreldrepenger.tilganger.integrasjoner.pip;

import java.util.Set;

public record SakMedPersonerDto(String saksnummer, String saksident, Set<String> identer) {
}

package no.nav.foreldrepenger.tilganger.domene.ansatt;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record Ansatt(@NotNull UUID uid, @NotNull String ident, String navn, String fornavnEtternavn, String epost, String enhetId) {
}

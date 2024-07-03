package no.nav.foreldrepenger.tilganger.domene;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record Ansatt(@NotNull UUID uid,
                     @NotNull String ident,
                     String navn,
                     String epost) {}

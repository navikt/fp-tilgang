package no.nav.foreldrepenger.tilganger.domene;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record Ansatt(@NotNull UUID uid,
                     @NotNull String ident,
                     String navn,
                     String epost) {}

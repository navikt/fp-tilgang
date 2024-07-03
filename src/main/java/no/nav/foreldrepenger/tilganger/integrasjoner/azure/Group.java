package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record Group(@NotNull UUID id) { }

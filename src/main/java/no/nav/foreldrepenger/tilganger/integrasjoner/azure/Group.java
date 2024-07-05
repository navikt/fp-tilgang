package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record Group(@NotNull UUID id) {
}

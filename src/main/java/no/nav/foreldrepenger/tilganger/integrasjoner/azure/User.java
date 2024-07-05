package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record User(@NotNull UUID id, @NotNull String onPremisesSamAccountName, @NotNull String displayName, String mail) {
}

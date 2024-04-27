package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record User(@NotNull UUID id,
                   @NotNull String onPremisesSamAccountName,
                   @NotNull String odataType,
                   @NotNull String displayName,
                   String mail) {}

package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record GroupsResponse(@NotNull List<Group> value) {
    public record Group(@NotNull UUID id) {
    }
}

package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

record GroupsResponse(@NotNull Set<Group> value) {}

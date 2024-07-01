package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import java.util.List;
import java.util.UUID;

public record GroupsResponse(List<Group> value) {
    public record Group(UUID id,
                        String onPremisesSamAccountName,
                        String displayName) {
    }
}

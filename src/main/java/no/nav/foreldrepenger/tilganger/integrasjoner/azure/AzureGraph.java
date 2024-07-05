package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AzureGraph {

    /**
     * @return bruker som er logget inn i konteksten.
     */
    User me();

    /**
     * Returns the groups for the given user.
     * If the groupFilter is used the only the groups from the filter are returned - if user if a member there.
     *
     * @param groupFilter the IDs of groups you want to get returned. If empty then all groups are returned.
     * @return bruker som er logget inn i konteksten.
     */
    Set<Group> memberOf(Set<UUID> groupFilter);

    Optional<User> finnUser(String ident);

    Optional<User> hentUser(UUID id);

    /**
     * Returns the groups for the given user.
     * If the groupFilter is used the only the groups from the filter are returned - if user if a member there.
     *
     * @param userUid     - the UID of the user
     * @param groupFilter the IDs of groups you want to get returned. If empty then all groups are returned.
     * @return Set with groups.
     */
    Set<Group> hentGrupper(UUID userUid, Set<UUID> groupFilter);
}

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
     * @return bruker som er logget inn i konteksten.
     */
    Set<Group> memberOf();

    Optional<User> finnUser(String ident);

    Optional<User> hentUser(UUID id);

    Set<Group> hentGrupper(UUID userUid);
}

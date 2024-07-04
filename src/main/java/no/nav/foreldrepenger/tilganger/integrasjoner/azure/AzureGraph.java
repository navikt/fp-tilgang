package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public interface AzureGraph {

    String NAVIDENT_REGEX = "^[a-zA-Z]\\d{6}$";
    Pattern NAVIDENT_PATTERN = Pattern.compile(NAVIDENT_REGEX);

    /**
     * @return bruker som er logget inn i konteksten.
     */
    User me();

    Set<Group> memberOf();

    Optional<User> finnUser(String ident);

    Optional<User> finnUser(UUID id);

    Set<Group> hentGrupper(UUID userUid);
}

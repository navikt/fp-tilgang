package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public interface AzureGraph {

    Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}$");
    String NAVIDENT_REGEX = "^[a-zA-Z]\\d{6}$";
    Pattern NAVIDENT_PATTERN = Pattern.compile(NAVIDENT_REGEX);

    /**
     * @return bruker som er logget inn i konteksten.
     */
    User me();

    /**
     * @return grupper til brukeren som er logget inn i konteksten.
     */
    List<GroupsResponse.Group> memberOf();

    Optional<User> user(String ident);

    Optional<User> user(UUID id);

    List<GroupsResponse.Group> groups(User user);
}

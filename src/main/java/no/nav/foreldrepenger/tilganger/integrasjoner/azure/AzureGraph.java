package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AzureGraph {

    /**
     * @return informasjon om bruker som er satt i konteksten (oid fra OBO-token).
     */
    User me();

    /**
     * @param groupFilter grupper som skal hentes, dersom tom liste så hentes alle grupper (kan være mange).
     * @return            grupper (filtrert) for bruker som er satt i konteksten (oid fra OBO-token).
     */
    Set<Group> memberOf(Set<UUID> groupFilter);

    /**
     * @param  ident  søk etter bruker med denne identen i onPremisesSamAccountName
     * @return        informasjon om bruker dersom finnes
     */
    Optional<User> finnUser(String ident);

    /**
     * @param id     - hent bruker med denne Entra-OID'en
     * @return       - informasjon om bruker
     */
    Optional<User> hentUser(UUID id);

    /**
     * @param userUid     Entra-OID for bruker som man skal hente grupper for
     * @param groupFilter grupper som skal hentes, dersom tom liste så hentes alle grupper (kan være mange).
     * @return            grupper (filtrert) for angitt bruker
     */
    Set<Group> hentGrupper(UUID userUid, Set<UUID> groupFilter);
}

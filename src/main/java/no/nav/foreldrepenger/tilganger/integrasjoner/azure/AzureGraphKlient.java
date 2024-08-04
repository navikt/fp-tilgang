package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import static no.nav.foreldrepenger.tilganger.utils.RegexUtils.NAVIDENT_PATTERN;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.integrasjon.rest.ProxyRestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, endpointProperty = "ms.graph.url", endpointDefault = "https://graph.microsoft.com/v1.0", scopesProperty = "ms.graph.scopes", scopesDefault = "https://graph.microsoft.com/.default")
class AzureGraphKlient implements AzureGraph {
    private static final Logger LOG = LoggerFactory.getLogger(AzureGraphKlient.class);

    private static final Environment ENV = Environment.current();

    protected static final String USERS_PATH = "/users";
    protected static final String ME_PATH = "/me";
    protected static final String MEMBER_OF_PATH = "/memberOf";
    protected static final String PARAM_NAME_SELECT = "$select";
    protected static final String PARAM_NAME_FILTER = "$filter";
    protected static final String PARAM_NAME_COUNT = "$count";
    protected static final String PARAM_NAME_TOP = "$top";
    protected static final String PARAM_VALUE_SELECT_USER = "id,onPremisesSamAccountName,displayName,givenName,surname,mail,streetAddress";
    protected static final String PARAM_VALUE_SELECT_GROUPS = "id";
    protected static final String HEADER_CONSISTENCY_LEVEL = "ConsistencyLevel";
    protected static final String EVENTUAL = "eventual";

    private final ProxyRestClient restKlient;
    private final RestConfig restConfig;
    private final URI meEndpoint;
    private final URI userEndpoint;

    AzureGraphKlient() {
        this(ProxyRestClient.client());
    }

    AzureGraphKlient(ProxyRestClient client) {
        this.restKlient = client;
        this.restConfig = RestConfig.forClient(this.getClass());
        this.userEndpoint = UriBuilder.fromUri(this.restConfig.endpoint().toString()).path(USERS_PATH).build();
        this.meEndpoint = UriBuilder.fromUri(this.restConfig.endpoint().toString()).path(ME_PATH).build();
    }

    @Override
    public User me() {
        URI requestUri = UriBuilder.fromUri(meEndpoint).queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_USER).build();

        var request = RestRequest.newGET(requestUri, restConfig).header(HEADER_CONSISTENCY_LEVEL, EVENTUAL);

        logDebugMelding(requestUri);
        return restKlient.send(request, User.class);
    }

    @Override
    public Set<Group> memberOf(Set<UUID> groupFilter) {
        var requestUri = UriBuilder.fromUri(meEndpoint)
            .path(MEMBER_OF_PATH)
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_GROUPS)
            .queryParam(PARAM_NAME_TOP, 500) // ellers er kun 100 returnert - filter hjelper om brukt
            .queryParam(PARAM_NAME_COUNT, true); // må være satt til å få filter til å virke

        insertGroupFilter(groupFilter, requestUri);

        var requestTarget = requestUri.build();
        var request = RestRequest.newGET(requestTarget, restConfig).header(HEADER_CONSISTENCY_LEVEL, EVENTUAL);

        logDebugMelding(requestTarget);
        var response = restKlient.send(request, GroupsResponse.class);

        var grupper = response.value();
        LOG.info("Fant grupper {}", grupper.size());
        loggGrupperIfDebug("Grupper: {}", grupper.stream().map(Objects::toString));
        return new HashSet<>(grupper);
    }

    @Override
    public Optional<User> finnUser(String ident) {
        if (!ENV.isLocal() && !NAVIDENT_PATTERN.matcher(ident).matches()) {
            return Optional.empty();
        }
        URI requestUri = UriBuilder.fromUri(userEndpoint)
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_USER)
            .queryParam(PARAM_NAME_FILTER, "onPremisesSamAccountName eq '" + ident + "'")
            .queryParam(PARAM_NAME_COUNT, true) // må være satt til å få filter til å virke
            .build();

        var request = RestRequest.newGET(requestUri, restConfig).header(HEADER_CONSISTENCY_LEVEL, EVENTUAL);

        logDebugMelding(requestUri);
        var response = restKlient.send(request, UsersResponse.class);
        if (response == null || response.value() == null || response.value().isEmpty()) {
            LOG.info("Fant ikke bruker med ident: {}", ident);
            return Optional.empty();
        }

        var brukere = response.value();
        if (brukere.size() > 1) {
            LOG.info("Fant flere brukere med ident: {}", ident);
            loggGrupperIfDebug("Brukere {}", brukere.stream().map(User::onPremisesSamAccountName));
            var firstUser = brukere.stream().filter(user -> user.onPremisesSamAccountName().equals(ident)).findFirst();
            LOG.info("Returnerer: {}", firstUser);
            return firstUser;
        }
        var bruker = response.value().getFirst();
        LOG.info("Fant bruker: {}", ident.equals(bruker.onPremisesSamAccountName()));
        return Optional.of(bruker);
    }

    @Override
    public Optional<User> hentUser(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        URI requestUri = UriBuilder.fromUri(userEndpoint)
            .path(id.toString())
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_USER)
            .queryParam(PARAM_NAME_COUNT, true) // må være satt til å få filter til å virke
            .build();

        var request = RestRequest.newGET(requestUri, restConfig).header(HEADER_CONSISTENCY_LEVEL, EVENTUAL);
        logDebugMelding(requestUri);

        var response = restKlient.send(request, User.class);
        if (response != null) {
            LOG.info("Fant bruker: {}", id.equals(response.id()));
            if (LOG.isDebugEnabled()) {
                LOG.debug("Bruker: {}", response);
            }
        } else {
            LOG.info("Fant ikke bruker");
        }
        return Optional.ofNullable(response);
    }

    @Override
    public Set<Group> hentGrupper(UUID userUid, Set<UUID> groupFilter) {
        if (userUid == null) {
            return Set.of();
        }
        var requestUri = UriBuilder.fromUri(userEndpoint)
            .path(userUid.toString())
            .path(MEMBER_OF_PATH)
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_GROUPS)
            .queryParam(PARAM_NAME_TOP, 500) // ellers er kun 100 returnert - filter hjelper om brukt
            .queryParam(PARAM_NAME_COUNT, true); // må være satt til å få filter til å virke
        insertGroupFilter(groupFilter, requestUri);

        var requestTarget = requestUri.build();
        var request = RestRequest.newGET(requestTarget, restConfig).header(HEADER_CONSISTENCY_LEVEL, EVENTUAL);

        logDebugMelding(requestTarget);
        var response = restKlient.send(request, GroupsResponse.class);

        var grupper = response.value();
        LOG.info("Fant grupper: {}", grupper.size());
        loggGrupperIfDebug("Grupper: {}", grupper.stream().map(Objects::toString));
        return new HashSet<>(grupper);
    }

    private static void insertGroupFilter(Set<UUID> groupFilter, UriBuilder requestUri) {
        if (!groupFilter.isEmpty()) {
            var grupper = groupFilter.stream().map(uuid -> "'" + uuid.toString() + "'").collect(Collectors.joining(","));
            requestUri.queryParam(PARAM_NAME_FILTER, "id in (" + grupper + ")");
        }
    }

    private static void loggGrupperIfDebug(String format, Stream<String> grupper) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(format, grupper.collect(Collectors.joining(", ")));
        }
    }

    private static void logDebugMelding(URI requestUri) {
        LOG.debug("Kaller til MS Graph: {}", requestUri);
    }

    record UsersResponse(@NotNull List<User> value) {
    }

    record GroupsResponse(@NotNull List<Group> value) {
    }
}

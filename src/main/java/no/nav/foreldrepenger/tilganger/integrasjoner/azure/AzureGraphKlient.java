package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.felles.integrasjon.rest.ProxyRestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE,
    endpointProperty = "ms.graph.url",
    endpointDefault = "https://graph.microsoft.com/v1.0",
    scopesProperty = "ms.graph.scopes",
    scopesDefault = "https://graph.microsoft.com/.default")
class AzureGraphKlient implements AzureGraph {
    private static final Logger LOG = LoggerFactory.getLogger(AzureGraphKlient.class);

    private static final Environment ENV = Environment.current();

    protected static final String USERS_PATH = "/users";
    protected static final String ME_PATH = "/me";
    protected static final String MEMBER_OF_PATH = "/memberOf";
    protected static final String PARAM_NAME_SELECT = "$select";
    protected static final String PARAM_VALUE_SELECT_USER = "id,onPremisesSamAccountName,displayName,mail";
    protected static final String CONSISTENCY_LEVEL = "ConsistencyLevel";
    protected static final String EVENTUAL = "eventual";
    protected static final String PARAM_NAME_FILTER = "$filter";
    protected static final String PARAM_VALUE_SELECT_GROUPS = "id";
    protected static final String PARAM_NAME_EXPAND = "$expand";
    protected static final String PARAM_NAME_COUNT = "$count";
    protected static final String PARAM_VALUE_EXPAND_MEMBER_OF = "memberOf($select=id)";

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
        URI requestUri = UriBuilder.fromUri(meEndpoint)
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_USER)
            .queryParam(PARAM_NAME_EXPAND, PARAM_VALUE_EXPAND_MEMBER_OF)
            .build();

        var request = RestRequest.newGET(requestUri, restConfig)
            .header(CONSISTENCY_LEVEL, EVENTUAL);

        logDebugMelding(requestUri);
        var response = restKlient.sendReturnUnhandled(request);

        return mapResponse(handleResponse(response, requestUri), User.class);
    }

    @Override
    public Set<Group> memberOf() {
        var requestUri = UriBuilder.fromUri(meEndpoint).path(MEMBER_OF_PATH).queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_GROUPS).build();
        var request = RestRequest.newGET(requestUri, restConfig)
            .header(CONSISTENCY_LEVEL, EVENTUAL);

        var response = restKlient.sendReturnUnhandled(request);
        var groupsResponse = mapResponse(handleResponse(response, requestUri), GroupsResponse.class);

        var grupper = groupsResponse.value();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Grupper={}", grupper.stream().map(Objects::toString).collect(Collectors.joining(", ")));
        }
        LOG.info("Finner {} grupper.", grupper.size());
        return new HashSet<>(grupper);
    }

    @Override
	public Optional<User> finnUser(String ident) {
		if (!ENV.isLocal() && !AzureGraph.NAVIDENT_PATTERN.matcher(ident).matches()) {
			return Optional.empty();
		}
        URI requestUri = UriBuilder.fromUri(userEndpoint)
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_USER)
            .queryParam(PARAM_NAME_FILTER, "onPremisesSamAccountName eq '" + ident + "'")
            .queryParam(PARAM_NAME_EXPAND, PARAM_VALUE_EXPAND_MEMBER_OF)
            .queryParam(PARAM_NAME_COUNT, true)
            .build();

        var request = RestRequest.newGET(requestUri, restConfig)
            .header(CONSISTENCY_LEVEL, EVENTUAL);

        logDebugMelding(requestUri);
        var response = restKlient.sendReturnUnhandled(request);

        return Optional.ofNullable(mapResponse(handleResponse(response, requestUri), UsersResponse.class).value().getFirst());
    }

    @Override
    public Optional<User> finnUser(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        URI requestUri = UriBuilder.fromUri(userEndpoint)
            .path(id.toString())
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_USER)
            .queryParam(PARAM_NAME_EXPAND, PARAM_VALUE_EXPAND_MEMBER_OF)
            .queryParam(PARAM_NAME_COUNT, true)
            .build();

        var request = RestRequest.newGET(requestUri, restConfig)
            .header(CONSISTENCY_LEVEL, EVENTUAL);

        logDebugMelding(requestUri);
        var response = restKlient.sendReturnUnhandled(request);

        return Optional.ofNullable(mapResponse(handleResponse(response, requestUri), User.class));
    }

    @Override
	public Set<Group> hentGrupper(UUID userUid) {
		if (userUid == null) {
			return Set.of();
		}
        var requestUri = UriBuilder.fromUri(userEndpoint)
            .path(userUid.toString())
            .path(MEMBER_OF_PATH)
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_GROUPS)
            .queryParam(PARAM_NAME_COUNT, true)
            .build();

        var request = RestRequest.newGET(requestUri, restConfig)
            .header(CONSISTENCY_LEVEL, EVENTUAL);

        logDebugMelding(requestUri);
        var response = restKlient.sendReturnUnhandled(request);

        var groupsResponse = mapResponse(handleResponse(response, requestUri), GroupsResponse.class);

        var grupper = groupsResponse.value();
        if (LOG.isDebugEnabled()) {
            LOG.debug("MS: Grupper={}", grupper.stream().map(Objects::toString).collect(Collectors.joining(", ")));
        }
        LOG.info("MS: Finner {} grupper.", grupper.size());
        return new HashSet<>(grupper);
	}

    private static String handleResponse(final HttpResponse<String> response, URI endpoint) {
        int status = response.statusCode();
        if (status == HttpURLConnection.HTTP_NO_CONTENT) {
            return null;
        }
        if ((status >= HttpURLConnection.HTTP_OK && status < HttpURLConnection.HTTP_MULT_CHOICE)) {
            return response.body();
        }
        if (status == HttpURLConnection.HTTP_FORBIDDEN) {
            throw new ManglerTilgangException("F-468816", "Feilet mot " + endpoint);
        }
        if (status == HttpURLConnection.HTTP_BAD_REQUEST) {
            ErrorResponse errorResponse = mapResponse(response.body(), ErrorResponse.class);
            if (errorResponse != null && errorResponse.error() != null) {
                var error = errorResponse.error();
                throw new IntegrasjonException("F-468817", String.format("Uventet respons %s fra %s med kode: %s og melding: %s", status, endpoint, error.code(), error.message()));
            }
        }
        throw new IntegrasjonException("F-468817", String.format("Uventet respons %s fra %s", status, endpoint));
    }

    private static <T> T mapResponse(String response, Class<T> clazz) {
        if (clazz.isAssignableFrom(String.class)) {
            return clazz.cast(response);
        }
        return DefaultJsonMapper.fromJson(response, clazz);
    }

    private static void logDebugMelding(URI requestUri) {
        LOG.debug("Kaller til MS Graph med: {}", requestUri);
    }

    record UsersResponse(@NotNull List<User> value) {}
    record GroupsResponse(@NotNull List<Group> value) {}
    record ErrorResponse(@NotNull Error error) {
        record Error(@NotNull String code, @NotNull String message) {}
    }
}

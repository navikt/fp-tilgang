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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.constraints.NotNull;

import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.ManglerTilgangException;

import no.nav.vedtak.mapper.json.DefaultJsonMapper;

import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.server.spi.ResponseErrorMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.integrasjon.rest.ProxyRestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

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
    protected static final String $FILTER = "$filter";
    protected static final String PARAM_VALUE_SELECT_GROUPS = "id";
    protected static final String PARAM_NAME_EXPAND = "$expand";

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
        var request = RestRequest.newGET(UriBuilder.fromUri(meEndpoint)
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_USER)
            .build(), restConfig);
        request.header(CONSISTENCY_LEVEL, EVENTUAL);

        return restKlient.send(request, User.class);
    }

    @Override
    public User meExtended() {
        URI requestUri = UriBuilder.fromUri(meEndpoint)
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_USER)
            .queryParam(PARAM_NAME_EXPAND, "memberOf($select=id)")
            .build();

        var request = RestRequest.newGET(requestUri, restConfig)
            .header(CONSISTENCY_LEVEL, EVENTUAL);

        LOG.debug("Kaller til MS Graph med: {}", requestUri);
        var response = restKlient.sendReturnUnhandled(request);

        return mapResponse(handleResponse(response, requestUri), User.class);
    }

    @Override
    public Set<Group> memberOf() {
        var request = RestRequest.newGET(UriBuilder.fromUri(meEndpoint).path(MEMBER_OF_PATH)
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_GROUPS)
            .build(), restConfig);
        request.header(CONSISTENCY_LEVEL, EVENTUAL);

        return new HashSet<>(restKlient.send(request, GroupsResponse.class).value());
    }

    @Override
	public Optional<User> finnUser(String ident) {
		if (!ENV.isLocal() && !NAVIDENT_PATTERN.matcher(ident).matches()) {
			return Optional.empty();
		}
        return findUserInfo(ident);
	}

    @Override
    public Optional<User> finnUser(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        var request = RestRequest.newGET(UriBuilder.fromUri(userEndpoint).path(id.toString())
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_USER)
            .build(), restConfig);
        request.header(CONSISTENCY_LEVEL, EVENTUAL);

        try {
            User user = restKlient.send(request, User.class);
            if (user == null) {
                LOG.info("MS: finner ikke bruker med id={}", id);
                return Optional.empty();
            }
            LOG.debug("MS: fant bruker med id={}", id);
            return Optional.of(user);
        } catch (Exception e) {
            LOG.info("MS: Teknisk feil. Message={}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<User> findUserInfo(String userId) {
        var request = RestRequest.newGET(UriBuilder.fromUri(userEndpoint)
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_USER)
            .queryParam($FILTER, "onPremisesSamAccountName eq '" + userId + "'")
            .queryParam("$count", true)
            .build(), restConfig);
        request.header(CONSISTENCY_LEVEL, EVENTUAL);

        try {
            UsersResponse users = restKlient.send(request, UsersResponse.class);
            if (users == null || users.value() == null || users.value().isEmpty()) {
                LOG.info("MS: finner ikke bruker med id={}", userId);
                return Optional.empty();
            }

            var first = users.value().getFirst();
            if (users.value().size() > 1) {
                LOG.info("MS: finner flere brukere med id={}", userId);
                return Optional.of(first);
            }

            LOG.debug("MS: fant bruker med id={}", first.id());
            return Optional.of(first);
        } catch (Exception e) {
            LOG.info("MS: Teknisk feil. Message={}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
	public Set<Group> hentGrupper(UUID userUid) {
		if (userUid == null) {
			return Set.of();
		}
        // Bruker til å liste alle grupper for en bruker, men det er mulig å liste alle brukere av en gruppe med
        // /v1.0/groups/<group-uid>/members?$count=true
        var request = RestRequest.newGET(UriBuilder.fromUri(userEndpoint).path(userUid.toString()).path(MEMBER_OF_PATH)
            .queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_GROUPS)
            .queryParam("$count", true)
            .build(), restConfig);
        request.header(CONSISTENCY_LEVEL, EVENTUAL);

        try {
			var grupper = restKlient.send(request, GroupsResponse.class);
            if (grupper == null || grupper.value() == null || grupper.value().isEmpty()) {
                LOG.info("MS: finner ikke grupper for bruker={}", userUid);
                return Set.of();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("MS: Grupper={}", grupper.value().stream().map(Objects::toString).collect(Collectors.joining(", ")));
            }
            LOG.info("MS: Finner {} grupper.", grupper.value().size());
            return new HashSet<>(grupper.value());
		} catch (Exception e) {
            LOG.info("MS: Teknisk feil. Message={}", e.getMessage());
			return Set.of();
		}
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

    record UsersResponse(@NotNull List<User> value) {}
    record GroupsResponse(@NotNull List<Group> value) {}
    record ErrorResponse(@NotNull Error error) {
        record Error(@NotNull String code, @NotNull String message) {}
    }
}

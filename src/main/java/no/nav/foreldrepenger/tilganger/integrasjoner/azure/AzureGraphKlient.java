package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
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
    protected static final String USER_SELECT = "id,onPremisesSamAccountName,displayName,mail";
    protected static final String $_SELECT = "$select";
    protected static final String CONSISTENCY_LEVEL = "ConsistencyLevel";
    protected static final String EVENTUAL = "eventual";
    protected static final String $_FILTER = "$filter";
    protected static final String GROUPS_SELECT = "id,onPremisesSamAccountName,displayName";

    private final RestClient restKlient;
    private final RestConfig restConfig;
    private final URI meEndpoint;
    private final URI userEndpoint;

    AzureGraphKlient() {
        this(RestClient.client());
    }

    AzureGraphKlient(RestClient client) {
        this.restKlient = client;
        this.restConfig = RestConfig.forClient(this.getClass());
        this.userEndpoint = UriBuilder.fromUri(this.restConfig.endpoint().toString()).path(USERS_PATH).build();
        this.meEndpoint = UriBuilder.fromUri(this.restConfig.endpoint().toString()).path(ME_PATH).build();

        if (!this.restConfig.tokenConfig().isAzureAD()) {
            throw new IllegalStateException("Kan kun kalles med en OBO Azure kontekst.");
        }
	}

    @Override
    public User me() {
        var request = RestRequest.newGET(UriBuilder.fromUri(meEndpoint)
            .queryParam($_SELECT, USER_SELECT)
            .build(), restConfig);
        request.header(CONSISTENCY_LEVEL, EVENTUAL);

        return restKlient.send(request, User.class);
    }

    @Override
    public List<GroupsResponse.Group> memberOf() {
        var request = RestRequest.newGET(UriBuilder.fromUri(meEndpoint).path(MEMBER_OF_PATH)
            .queryParam($_SELECT, GROUPS_SELECT)
            .build(), restConfig);
        request.header(CONSISTENCY_LEVEL, EVENTUAL);

        return restKlient.send(request, GroupsResponse.class).value();
    }

    @Override
	public Optional<User> user(String ident) {
		if (!ENV.isLocal() && !NAVIDENT_PATTERN.matcher(ident).matches()) {
			return Optional.empty();
		}
        return findUserInfo(ident);
	}

    @Override
    public Optional<User> user(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return findUserInfo(id.toString());
    }

    private Optional<User> findUserInfo(String userId) {
        var request = RestRequest.newGET(UriBuilder.fromUri(userEndpoint)
            .queryParam($_SELECT, USER_SELECT)
            .queryParam($_FILTER, getFilter(userId))
            .build(), restConfig);
        request.header(CONSISTENCY_LEVEL, EVENTUAL);

        try {
            User user = restKlient.send(request, User.class);
            if (user == null) {
                LOG.info("MS: finner ikke bruker med id={}", userId);
                return Optional.empty();
            }
            return Optional.of(user);
        } catch (Exception e) {
            LOG.info("MS Graph: Teknisk feil. Message={}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static String getFilter(String userId) {
        if (UUID_PATTERN.matcher(userId).matches()) {
            return "id eq '" + userId + "'";
        }
        return "onPremisesSamAccountName eq '" + userId + "'";
    }

    @Override
	public List<GroupsResponse.Group> groups(User user) {
		if (user == null || user.id() == null) {
			return List.of();
		}
        // Bruker til å liste alle grupper for en bruker, men det er mulig å liste alle brukere av en gruppe med
        // /v1.0/groups/<group-oid>/members?$count=true
        var request = RestRequest.newGET(UriBuilder.fromUri(userEndpoint).path(user.id().toString()).path(MEMBER_OF_PATH)
            .queryParam($_SELECT, GROUPS_SELECT)
            .build(), restConfig);
        request.header(CONSISTENCY_LEVEL, EVENTUAL);

        try {
			var grupper = restKlient.send(request, GroupsResponse.class);
            if (grupper == null || grupper.value() == null || grupper.value().isEmpty()) {
                LOG.info("MS Graph: finner ikke grupper for bruker={}", user.id());
                return List.of();
            }
            return grupper.value();
		} catch (Exception e) {
            LOG.info("MS Graph: Teknisk feil. Message={}", e.getMessage(), e);
			return List.of();
		}
	}

}

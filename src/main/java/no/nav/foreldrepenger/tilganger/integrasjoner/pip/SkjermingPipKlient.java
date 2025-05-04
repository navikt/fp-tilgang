package no.nav.foreldrepenger.tilganger.integrasjoner.pip;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC,
    endpointProperty = "skjermet.person.pip.base.url", endpointDefault = "https://skjermede-personer-pip.intern.nav.no",
    scopesProperty = "skjermet.person.pip.scope", scopesDefault = "api://prod-gcp.nom.skjermede-personer-pip/.default")
public class SkjermingPipKlient {

    private static final Logger LOG = LoggerFactory.getLogger(SkjermingPipKlient.class);

    private static final String SKJERMET_PATH = "skjermet";
    private static final String BULK_PATH = "skjermetBulk";

    private final RestClient client;
    private final RestConfig restConfig;
    private final URI bulkEndpoint;
    private final URI skjermetEndpoint;

    protected SkjermingPipKlient() {
        this(RestClient.client());
    }

    protected SkjermingPipKlient(RestClient restClient) {
        this.client = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
        this.skjermetEndpoint =  UriBuilder.fromUri(restConfig.endpoint()).path(SKJERMET_PATH).build();
        this.bulkEndpoint =  UriBuilder.fromUri(restConfig.endpoint()).path(BULK_PATH).build();
        if (!restConfig.tokenConfig().isAzureAD()) {
            throw new IllegalArgumentException("Utviklerfeil: klient må annoteres med Azure CC");
        }
    }


    public boolean erSkjermet(String fnr) {
        if (fnr == null) {
            return false;
        }

        var request = RestRequest.newPOSTJson(new SkjermetRequestDto(fnr), skjermetEndpoint, restConfig);

        try {
            return kallMedSjekk(request);
        } catch (Exception e) {
            LOG.info("SkjermetPerson fikk feil", e);
        }
        return kallMedSjekk(request);
    }

    private boolean kallMedSjekk(RestRequest request) {
        var skjermet = client.send(request, String.class);
        return "true".equalsIgnoreCase(skjermet);
    }

    public Map<String, Boolean> erSkjermet(List<String> fnr) {
        if (fnr == null || fnr.isEmpty()) {
            return Map.of();
        }

        var request = RestRequest.newPOSTJson(new SkjermetBulkRequestDto(fnr), bulkEndpoint, restConfig);

        try {
            return kallBulk(request);
        } catch (Exception e) {
            LOG.info("SkjermetPerson fikk feil", e);
        }
        return kallBulk(request);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Boolean> kallBulk(RestRequest request) {
        // Se github / skjerming / PipController
        return client.send(request, Map.class);
    }

    private record SkjermetRequestDto(String personident) { }

    private record SkjermetBulkRequestDto(List<String> personidenter) { }
}

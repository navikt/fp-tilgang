package no.nav.foreldrepenger.tilganger.integrasjoner.pip;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPSAK)
public class FpsakPipKlient {

    static final Logger LOG = LoggerFactory.getLogger(FpsakPipKlient.class);

    private final RestClient restClient;
    private final RestConfig restConfig;

    private final URI sakIdentEndpoint;
    private final URI sakFullEndpoint;
    private final URI behSaksnummerEndpoint;

    public FpsakPipKlient() {
        this(RestClient.client());
    }

    public FpsakPipKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
        this.sakIdentEndpoint = UriBuilder.fromUri(restConfig.fpContextPath()).path("/api/pip/ident-for-sak").build();
        this.sakFullEndpoint = UriBuilder.fromUri(restConfig.fpContextPath()).path("/api/pip/full-for-sak").build();
        this.behSaksnummerEndpoint = UriBuilder.fromUri(restConfig.fpContextPath()).path("/api/pip/saksnummer-for-behandling").build();
        if (!restConfig.tokenConfig().isAzureAD()) {
            throw new IllegalArgumentException("Utviklerfeil: klient må annoteres med Azure CC");
        }
    }


    public Optional<SakMedPersonerDto> personerForSak(String saksnummer) {
        if (saksnummer == null) {
            return Optional.empty();
        }

        var uri = UriBuilder.fromUri(sakFullEndpoint)
            .queryParam("saksnummer", saksnummer)
            .build();
        var request = RestRequest.newGET(uri, restConfig);

        try {
            return restClient.sendReturnOptional(request, SakMedPersonerDto.class);
        } catch (Exception e) {
            LOG.info("ForeldrepengerPip personForSak fikk feil", e);
        }
        return restClient.sendReturnOptional(request, SakMedPersonerDto.class);
    }

    public Optional<String> sakIdentForSak(String saksnummer) {
        if (saksnummer == null) {
            return Optional.empty();
        }

        var uri = UriBuilder.fromUri(sakIdentEndpoint)
            .queryParam("saksnummer", saksnummer)
            .build();
        var request = RestRequest.newGET(uri, restConfig);

        try {
            return restClient.sendReturnOptional(request, String.class);
        } catch (Exception e) {
            LOG.info("ForeldrepengerPip personForSak fikk feil", e);
        }
        return restClient.sendReturnOptional(request, String.class);
    }

    public List<SakMedPersonerDto> personerForSaker(Set<String> saksnummer) {
        if (saksnummer == null || saksnummer.isEmpty()) {
            return List.of();
        }

        var request = RestRequest.newPOSTJson(saksnummer, sakFullEndpoint, restConfig);

        try {
            return restClient.sendReturnList(request, SakMedPersonerDto.class);
        } catch (Exception e) {
            LOG.info("ForeldrepengerPip personerForSaker fikk feil", e);
        }
        return restClient.sendReturnList(request, SakMedPersonerDto.class);
    }

    public Optional<String> saksnummerForBehandling(UUID behandlingUuid) {
        if (behandlingUuid == null) {
            return Optional.empty();
        }

        var uri = UriBuilder.fromUri(behSaksnummerEndpoint)
            .queryParam("behandlingUuid", behandlingUuid.toString())
            .build();
        var request = RestRequest.newGET(uri, restConfig);

        try {
            return restClient.sendReturnOptional(request, String.class);
        } catch (Exception e) {
            LOG.info("ForeldrepengerPip sakForBehandling fikk feil", e);
        }
        return restClient.sendReturnOptional(request, String.class);
    }

}

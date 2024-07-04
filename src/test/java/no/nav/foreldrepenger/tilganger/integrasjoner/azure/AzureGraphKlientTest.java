package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import no.nav.vedtak.felles.integrasjon.rest.ProxyRestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

import javax.net.ssl.SSLSession;

class AzureGraphKlientTest {

    private AzureGraphKlient azureGraphKlient;
    private Validator validator;

    // Mock RestClient to simulate REST calls
    private ProxyRestClient mockRestClient;

    @BeforeEach
    void setUp() {
        KontekstHolder.setKontekst(RequestKontekst.forRequest("uid", "kompakt", IdentType.InternBruker, null, Set.of()));
        mockRestClient = mock(ProxyRestClient.class);
        azureGraphKlient = new AzureGraphKlient(mockRestClient);

        // Set up validator using Jakarta Validation API
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterEach
    void tearDown() {
        KontekstHolder.fjernKontekst();
    }

    @Test
    void testUserReturnsFinnUserInfo() {
        // Prepare test data
        String userId = "123456";
        var expectedUser = new User(UUID.randomUUID(), "sam","display", "mail", null);
        var response = new AzureGraphKlient.UsersResponse(List.of(expectedUser));

        // Mock REST call behavior
        when(mockRestClient.sendReturnUnhandled(any(RestRequest.class))).thenReturn(opprettResponse(response, HttpStatus.Code.OK));
        // Invoke the method under test
        Optional<User> result = azureGraphKlient.finnUser(userId);

        // Verify the result
        assertThat(result).isPresent().contains(expectedUser);
    }

    @Test
    void testUserReturnsFinnUserInfo2() {
        // Prepare test data
        UUID userId = UUID.randomUUID();
        var expectedUser = new User(userId, "samAccountName", "displayName", "mail@example.com", null);
        var response = new AzureGraphKlient.UsersResponse(List.of(expectedUser));

        // Mock REST call behavior
        when(mockRestClient.sendReturnUnhandled(any(RestRequest.class))).thenReturn(opprettResponse(response, HttpStatus.Code.OK));

        // Invoke the method under test
        Optional<User> result = azureGraphKlient.finnUser(userId.toString());

        // Validate user object
        assertThat(result).isPresent();
        User actualUser = result.get();
        assertThat(actualUser).isEqualTo(expectedUser);

        // Validate user properties using Jakarta Validation API
        assertThat(validator.validate(actualUser)).isEmpty(); // No validation errors expected
    }


    @Test
    void testUserReturnsError() {
        // Prepare test data
        var userId = UUID.randomUUID().toString();
        var response = new AzureGraphKlient.ErrorResponse(new AzureGraphKlient.ErrorResponse.Error("12345", "Feilmelding"));

        // Mock REST call behavior
        when(mockRestClient.sendReturnUnhandled(any(RestRequest.class))).thenReturn(opprettResponse(response, HttpStatus.Code.BAD_REQUEST));

        // Invoke the method under test
        var error = assertThrows(IntegrasjonException.class, () -> azureGraphKlient.finnUser(userId));

        assertThat(error).isNotNull();
        assertThat(error.getMessage()).contains("12345", "Feilmelding");
    }


    private static <T> HttpResponse<String> opprettResponse(T response, HttpStatus.Code statusCode) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return statusCode.getCode();
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return null;
            }

            @Override
            public String body() {
                return DefaultJsonMapper.toJson(response);
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public HttpClient.Version version() {
                return null;
            }
        };
    }

}

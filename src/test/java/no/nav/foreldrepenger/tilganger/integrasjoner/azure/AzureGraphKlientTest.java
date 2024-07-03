package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
        var expectedUser = new User(UUID.randomUUID(), "sam","display", "mail");
        var response = new AzureGraphKlient.UsersResponse(List.of(expectedUser));

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any(Class.class))).thenReturn(response);

        // Invoke the method under test
        Optional<User> result = azureGraphKlient.finnUser(userId);

        // Verify the result
        assertThat(result).isPresent().contains(expectedUser);
    }

    @Test
    void testFinnUserReturnsEmptyOptionalForNullId() {
        // Invoke the method under test with null ID
        Optional<User> result = azureGraphKlient.finnUser("null");

        // Verify the result
        assertThat(result).isEmpty();
    }

    @Test
    void testFinnUserReturnsEmptyOptionalForNonMatchingId() {
        // Prepare test data
        String invalidUserId = "invalidId";

        // Invoke the method under test with an invalid ID
        Optional<User> result = azureGraphKlient.finnUser(invalidUserId);

        // Verify the result
        assertThat(result).isEmpty();
    }

    @Test
    void testUserReturnsFinnUserInfo2() {
        // Prepare test data
        UUID userId = UUID.randomUUID();
        var expectedUser = new User(userId, "samAccountName", "displayName", "mail@example.com");
        var response = new AzureGraphKlient.UsersResponse(List.of(expectedUser));

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any(Class.class))).thenReturn(response);

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
    void testFinnUserReturnsEmptyOptionalForNonMatchingId2() {
        // Prepare test data
        String invalidUserId = "invalidId";

        // Invoke the method under test with an invalid ID
        Optional<User> result = azureGraphKlient.finnUser(invalidUserId);

        // Verify the result
        assertThat(result).isEmpty();
    }

}

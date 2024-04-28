package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;

class AzureGraphKlientTest {

    private AzureGraphKlient azureGraphKlient;
    private Validator validator;

    // Mock RestClient to simulate REST calls
    private RestClient mockRestClient;

    @BeforeEach
    void setUp() {
        mockRestClient = mock(RestClient.class);
        azureGraphKlient = new AzureGraphKlient(mockRestClient);

        // Set up validator using Jakarta Validation API
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testUserReturnsUserInfo() {
        // Prepare test data
        String userId = "123456";
        User expectedUser = new User(UUID.randomUUID(), "sam", "odata", "display", "mail");

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any(Class.class))).thenReturn(expectedUser);

        // Invoke the method under test
        Optional<User> result = azureGraphKlient.user(userId);

        // Verify the result
        assertThat(result).isPresent().contains(expectedUser);
    }

    @Test
    void testUserReturnsEmptyOptionalForNullId() {
        // Invoke the method under test with null ID
        Optional<User> result = azureGraphKlient.user("null");

        // Verify the result
        assertThat(result).isEmpty();
    }

    @Test
    void testUserReturnsEmptyOptionalForNonMatchingId() {
        // Prepare test data
        String invalidUserId = "invalidId";

        // Invoke the method under test with an invalid ID
        Optional<User> result = azureGraphKlient.user(invalidUserId);

        // Verify the result
        assertThat(result).isEmpty();
    }

    @Test
    void testUserReturnsUserInfo2() {
        // Prepare test data
        UUID userId = UUID.randomUUID();
        User expectedUser = new User(userId, "samAccountName", "odataType", "displayName", "mail@example.com");

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any(Class.class))).thenReturn(expectedUser);

        // Invoke the method under test
        Optional<User> result = azureGraphKlient.user(userId.toString());

        // Validate user object
        assertThat(result).isPresent();
        User actualUser = result.get();
        assertThat(actualUser).isEqualTo(expectedUser);

        // Validate user properties using Jakarta Validation API
        assertThat(validator.validate(actualUser)).isEmpty(); // No validation errors expected
    }

    @Test
    void testUserReturnsEmptyOptionalForNonMatchingId2() {
        // Prepare test data
        String invalidUserId = "invalidId";

        // Invoke the method under test with an invalid ID
        Optional<User> result = azureGraphKlient.user(invalidUserId);

        // Verify the result
        assertThat(result).isEmpty();
    }

}

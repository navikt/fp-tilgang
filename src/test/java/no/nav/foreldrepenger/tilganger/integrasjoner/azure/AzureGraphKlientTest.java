package no.nav.foreldrepenger.tilganger.integrasjoner.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

class AzureGraphKlientTest {

    private AzureGraphKlient azureGraphKlient;
    private Validator validator;

    // Mock RestClient to simulate REST calls
    private RestClient mockRestClient;

    @BeforeEach
    void setUp() {
        KontekstHolder.setKontekst(RequestKontekst.forRequest("uid", "kompakt", IdentType.InternBruker, null, UUID.randomUUID(), Set.of()));
        mockRestClient = mock(RestClient.class);
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
    @DisplayName("Find user by ident OK - find 1 user")
    void testFinnUserByIdent() {
        // Prepare test data
        String userId = "123456";
        var expectedUser = new User(UUID.randomUUID(), "sam", "display", "fornavn", "etternavn", "ansattVedEnhetId");
        var response = new AzureGraphKlient.UsersResponse(List.of(expectedUser));

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any())).thenReturn(response);
        // Invoke the method under test
        var result = azureGraphKlient.finnUser(userId);

        // Verify the result
        assertThat(result).isPresent().contains(expectedUser);
        assertThat(validator.validate(result)).isEmpty(); // No validation errors expected
    }

    @Test
    @DisplayName("Find user by ident OK - find 2 user. Match by ident and return matching.")
    void testFinnUserByIdentReturnsToUsers() {
        // Prepare test data
        String userId = "123456";
        var user = new User(UUID.randomUUID(), "one", "display", "fornavn", "etternavn", "ansattVedEnhetId");
        var expectedUser = new User(UUID.randomUUID(), userId, "user", "fornavn", "etternavn", "ansattVedEnhetId");
        var response = new AzureGraphKlient.UsersResponse(List.of(user, expectedUser));

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any())).thenReturn(response);
        // Invoke the method under test
        var result = azureGraphKlient.finnUser(userId);

        // Verify the result
        assertThat(result).isPresent().contains(expectedUser);
        assertThat(validator.validate(result)).isEmpty(); // No validation errors expected
    }


    @Test
    @DisplayName("Find user by ident OK - find 2 user. Match by ident and return no match.")
    void testFinnUserByIdentReturnsNoMatchingUser() {
        // Prepare test data
        String userId = "123456";
        var user = new User(UUID.randomUUID(), "one", "display", "fornavn", "etternavn", "ansattVedEnhetId");
        var secondUser = new User(UUID.randomUUID(), "two", "user", "fornavn", "etternavn", "ansattVedEnhetId");
        var response = new AzureGraphKlient.UsersResponse(List.of(user, secondUser));

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any())).thenReturn(response);
        // Invoke the method under test
        var result = azureGraphKlient.finnUser(userId);

        // Verify the result
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Find user by ident OK - no users found.")
    void testFinnUserByIdentReturnsEmptyList() {
        // Prepare test data
        String userId = "123456";
        var response = new AzureGraphKlient.UsersResponse(List.of());

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any())).thenReturn(response);
        // Invoke the method under test
        var result = azureGraphKlient.finnUser(userId);

        // Verify the result
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Find Me user info - OK.")
    void testMe() {
        // Prepare test data
        UUID userId = UUID.randomUUID();
        var expectedUser = new User(userId, "samAccountName", "displayName", "fornavn", "etternavn", "ansattVedEnhetId");

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any())).thenReturn(expectedUser);

        // Invoke the method under test
        var result = azureGraphKlient.me();

        // Validate user object
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);

        // Validate user properties using Jakarta Validation API
        assertThat(validator.validate(result)).isEmpty(); // No validation errors expected
    }

    @Test
    @DisplayName("Find Me user info - NOK.")
    void testMeNok() {
        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any())).thenReturn(null);

        // Invoke the method under test
        var result = azureGraphKlient.me();

        // Validate user object
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Find Me memberOf user info - OK.")
    void testMeMemberOfOk() {
        // Prepare test data
        var grupper = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var response = new AzureGraphKlient.GroupsResponse(grupper.stream().map(Group::new).toList());

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any())).thenReturn(response);

        // Invoke the method under test
        var result = azureGraphKlient.memberOf(new HashSet<>(grupper));

        // Validate user object
        assertThat(result).isNotEmpty().hasSize(4);

        // Validate user properties using Jakarta Validation API
        assertThat(validator.validate(result)).isEmpty(); // No validation errors expected
    }

    @Test
    @DisplayName("Find Me memberOf user info - NOK.")
    void testMeMemberOfNok() {
        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any())).thenReturn(new AzureGraphKlient.GroupsResponse(List.of()));

        // Invoke the method under test
        var result = azureGraphKlient.memberOf(Set.of());

        // Validate user object
        assertThat(result).isEmpty();

        // Validate user properties using Jakarta Validation API
        assertThat(validator.validate(result)).isEmpty(); // No validation errors expected
    }

    @Test
    @DisplayName("Hent user by uid - OK.")
    void testHentUserByUid() {
        // Prepare test data
        UUID userId = UUID.randomUUID();
        var expectedUser = new User(userId, "samAccountName", "displayName", "fornavn", "etternavn", "ansattVedEnhetId");

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any())).thenReturn(expectedUser);

        // Invoke the method under test
        var result = azureGraphKlient.hentUser(userId);

        // Validate user object
        assertThat(result).isNotEmpty();
        assertThat(result.get()).isEqualTo(expectedUser);

        // Validate user properties using Jakarta Validation API
        assertThat(validator.validate(result)).isEmpty(); // No validation errors expected
    }

    @Test
    @DisplayName("Hent user by uid - no user.")
    void testHentUserByUidNok() {
        // Prepare test data
        UUID userId = UUID.randomUUID();

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any())).thenReturn(null);

        // Invoke the method under test
        var result = azureGraphKlient.hentUser(userId);

        // Validate user object
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Hent grupper of user - OK.")
    void testHentGrupperOk() {
        // Prepare test data
        var grupper = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var response = new AzureGraphKlient.GroupsResponse(grupper.stream().map(Group::new).toList());

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any())).thenReturn(response);

        // Invoke the method under test
        var result = azureGraphKlient.hentGrupper(UUID.randomUUID(), new HashSet<>(grupper));

        // Validate user object
        assertThat(result).isNotEmpty().hasSize(3);

        // Validate user properties using Jakarta Validation API
        assertThat(validator.validate(result)).isEmpty(); // No validation errors expected
    }

    @Test
    @DisplayName("Hent grupper of user - NOK empty response.")
    void testHentGrupperNok() {
        // Prepare test data
        var filter = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var response = new AzureGraphKlient.GroupsResponse(List.of());

        // Mock REST call behavior
        when(mockRestClient.send(any(RestRequest.class), any())).thenReturn(response);

        // Invoke the method under test
        var result = azureGraphKlient.hentGrupper(UUID.randomUUID(), new HashSet<>(filter));

        // Validate user object
        assertThat(result).isEmpty();
    }

}

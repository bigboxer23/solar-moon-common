package com.bigboxer23.solar_moon.customer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AliasExistsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

@ExtendWith(MockitoExtension.class)
public class CognitoUserComponentTest {

	@Mock
	private CognitoIdentityProviderClient mockClient;

	private TestableCognitoUserComponent component;

	private static final String USER_POOL_ID = "us-west-2_testpool";
	private static final String USERNAME = "customer-123";
	private static final String EMAIL = "new@example.com";

	private class TestableCognitoUserComponent extends CognitoUserComponent {
		@Override
		protected CognitoIdentityProviderClient getClient() {
			return mockClient;
		}

		@Override
		protected String getUserPoolId() {
			return USER_POOL_ID;
		}
	}

	@BeforeEach
	void setUp() {
		component = new TestableCognitoUserComponent();
	}

	@Test
	void testUpdateEmail_setsEmailAndMarksVerified() {
		ArgumentCaptor<AdminUpdateUserAttributesRequest> captor =
				ArgumentCaptor.forClass(AdminUpdateUserAttributesRequest.class);

		component.updateEmail(USERNAME, EMAIL);

		verify(mockClient).adminUpdateUserAttributes(captor.capture());
		AdminUpdateUserAttributesRequest request = captor.getValue();
		assertEquals(USER_POOL_ID, request.userPoolId());
		assertEquals(USERNAME, request.username());
		Map<String, String> attributes =
				request.userAttributes().stream().collect(Collectors.toMap(AttributeType::name, AttributeType::value));
		assertEquals(Map.of("email", EMAIL, "email_verified", "true"), attributes);
	}

	@Test
	void testUpdateEmail_closesClient() {
		component.updateEmail(USERNAME, EMAIL);

		verify(mockClient).close();
	}

	@Test
	void testUpdateEmail_whenCognitoRejects_propagatesExceptionAndClosesClient() {
		when(mockClient.adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class)))
				.thenThrow(AliasExistsException.builder().message("exists").build());

		assertThrows(AliasExistsException.class, () -> component.updateEmail(USERNAME, EMAIL));
		verify(mockClient).close();
	}
}

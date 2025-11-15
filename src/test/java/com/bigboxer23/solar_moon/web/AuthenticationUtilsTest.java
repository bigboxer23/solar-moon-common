package com.bigboxer23.solar_moon.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.customer.CustomerComponent;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.lambda.data.*;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
public class AuthenticationUtilsTest {

	@Mock
	private CustomerComponent customerComponent;

	@AfterEach
	public void tearDown() {
		MDC.clear();
	}

	@Test
	public void testAuthenticateRequest_withValidBasicAuth() {
		String accessKey = "test-access-key";
		String encodedAuth = Base64.getEncoder().encodeToString(("user:" + accessKey).getBytes());
		String authHeader = "Basic " + encodedAuth;

		Customer customer = new Customer("customer-123");
		when(customerComponent.findCustomerIdByAccessKey(accessKey)).thenReturn(Optional.of(customer));

		String result = AuthenticationUtils.authenticateRequest(authHeader, customerComponent);

		assertEquals("customer-123", result);
		assertEquals("customer-123", MDC.get("customer.id"));
		verify(customerComponent).findCustomerIdByAccessKey(accessKey);
	}

	@Test
	public void testAuthenticateRequest_withNullAuthHeader() {
		String result = AuthenticationUtils.authenticateRequest((String) null, customerComponent);

		assertNull(result);
		verifyNoInteractions(customerComponent);
	}

	@Test
	public void testAuthenticateRequest_withInvalidAuthHeaderFormat() {
		String result = AuthenticationUtils.authenticateRequest("Bearer token123", customerComponent);

		assertNull(result);
		verifyNoInteractions(customerComponent);
	}

	@Test
	public void testAuthenticateRequest_withInvalidBase64Content() {
		String authHeader = "Basic " + Base64.getEncoder().encodeToString("invalidformat".getBytes());

		String result = AuthenticationUtils.authenticateRequest(authHeader, customerComponent);

		assertNull(result);
		verifyNoInteractions(customerComponent);
	}

	@Test
	public void testAuthenticateRequest_withInvalidAccessKey() {
		String accessKey = "invalid-key";
		String encodedAuth = Base64.getEncoder().encodeToString(("user:" + accessKey).getBytes());
		String authHeader = "Basic " + encodedAuth;

		when(customerComponent.findCustomerIdByAccessKey(accessKey)).thenReturn(Optional.empty());

		String result = AuthenticationUtils.authenticateRequest(authHeader, customerComponent);

		assertNull(result);
		verify(customerComponent).findCustomerIdByAccessKey(accessKey);
	}

	@Test
	public void testAuthenticateRequest_withEmptyPassword() {
		String encodedAuth = Base64.getEncoder().encodeToString("user:".getBytes());
		String authHeader = "Basic " + encodedAuth;

		String result = AuthenticationUtils.authenticateRequest(authHeader, customerComponent);

		assertNull(result);
		verifyNoInteractions(customerComponent);
	}

	@Test
	public void testAuthenticateRequest_withMultipleColons() {
		String accessKey = "key:with:colons";
		String encodedAuth = Base64.getEncoder().encodeToString(("user:" + accessKey).getBytes());
		String authHeader = "Basic " + encodedAuth;

		String result = AuthenticationUtils.authenticateRequest(authHeader, customerComponent);

		assertNull(result);
		verifyNoInteractions(customerComponent);
	}

	@Test
	public void testAuthenticateRequest_withLambdaRequest() {
		String accessKey = "lambda-access-key";
		String encodedAuth = Base64.getEncoder().encodeToString(("user:" + accessKey).getBytes());

		LambdaRequest request = new LambdaRequest();
		LambdaRequestHeaders headers = new LambdaRequestHeaders();
		headers.setAuthorization("Basic " + encodedAuth);
		request.setHeaders(headers);

		Customer customer = new Customer("customer-789");
		when(customerComponent.findCustomerIdByAccessKey(accessKey)).thenReturn(Optional.of(customer));

		String result = AuthenticationUtils.authenticateRequest(request, customerComponent);

		assertEquals("customer-789", result);
		verify(customerComponent).findCustomerIdByAccessKey(accessKey);
	}

	@Test
	public void testAuthenticateRequest_withNullLambdaRequest() {
		String result = AuthenticationUtils.authenticateRequest((LambdaRequest) null, customerComponent);

		assertNull(result);
		verifyNoInteractions(customerComponent);
	}

	@Test
	public void testAuthenticateRequest_withNullHeaders() {
		LambdaRequest request = new LambdaRequest();
		request.setHeaders(null);

		String result = AuthenticationUtils.authenticateRequest(request, customerComponent);

		assertNull(result);
		verifyNoInteractions(customerComponent);
	}

	@Test
	public void testGetCustomerIdFromRequest_withValidAuthorizer() {
		LambdaRequest request = new LambdaRequest();
		LambdaRequestContext context = new LambdaRequestContext();
		LambdaAuthorizer authorizer = new LambdaAuthorizer();
		LambdaClaims claims = new LambdaClaims();
		claims.setUsername("customer-id-from-claims");

		authorizer.setClaims(claims);
		context.setAuthorizer(authorizer);
		request.setRequestContext(context);

		String result = AuthenticationUtils.getCustomerIdFromRequest(request);

		assertEquals("customer-id-from-claims", result);
	}

	@Test
	public void testGetCustomerIdFromRequest_withNullAuthorizer() {
		LambdaRequest request = new LambdaRequest();
		LambdaRequestContext context = new LambdaRequestContext();
		context.setAuthorizer(null);
		request.setRequestContext(context);

		String result = AuthenticationUtils.getCustomerIdFromRequest(request);

		assertNull(result);
	}

	@Test
	public void testGetCustomerIdFromRequest_withNullClaims() {
		LambdaRequest request = new LambdaRequest();
		LambdaRequestContext context = new LambdaRequestContext();
		LambdaAuthorizer authorizer = new LambdaAuthorizer();
		authorizer.setClaims(null);
		context.setAuthorizer(authorizer);
		request.setRequestContext(context);

		assertThrows(NullPointerException.class, () -> {
			AuthenticationUtils.getCustomerIdFromRequest(request);
		});
	}

	@Test
	public void testGetCustomerIdFromRequest_withNullUsername() {
		LambdaRequest request = new LambdaRequest();
		LambdaRequestContext context = new LambdaRequestContext();
		LambdaAuthorizer authorizer = new LambdaAuthorizer();
		LambdaClaims claims = new LambdaClaims();
		claims.setUsername(null);

		authorizer.setClaims(claims);
		context.setAuthorizer(authorizer);
		request.setRequestContext(context);

		String result = AuthenticationUtils.getCustomerIdFromRequest(request);

		assertNull(result);
	}

	@Test
	public void testAuthenticateRequest_updatesTransactionUtilCustomerId() {
		String accessKey = "test-key";
		String encodedAuth = Base64.getEncoder().encodeToString(("user:" + accessKey).getBytes());
		String authHeader = "Basic " + encodedAuth;

		Customer customer = new Customer("customer-mdc-test");
		when(customerComponent.findCustomerIdByAccessKey(accessKey)).thenReturn(Optional.of(customer));

		AuthenticationUtils.authenticateRequest(authHeader, customerComponent);

		assertEquals("customer-mdc-test", MDC.get("customer.id"));
	}

	@Test
	public void testAuthenticateRequest_withWhitespaceInCredentials() {
		String encodedAuth = Base64.getEncoder().encodeToString("user : key".getBytes());
		String authHeader = "Basic " + encodedAuth;

		Customer customer = new Customer("customer-whitespace");
		when(customerComponent.findCustomerIdByAccessKey(" key")).thenReturn(Optional.of(customer));

		String result = AuthenticationUtils.authenticateRequest(authHeader, customerComponent);

		assertEquals("customer-whitespace", result);
	}

	@Test
	public void testAuthenticateRequest_withNoColon() {
		String encodedAuth = Base64.getEncoder().encodeToString("nocolonhere".getBytes());
		String authHeader = "Basic " + encodedAuth;

		String result = AuthenticationUtils.authenticateRequest(authHeader, customerComponent);

		assertNull(result);
		verifyNoInteractions(customerComponent);
	}
}

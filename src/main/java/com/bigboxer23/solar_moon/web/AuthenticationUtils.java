package com.bigboxer23.solar_moon.web;

import com.bigboxer23.solar_moon.CustomerComponent;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.lambda.data.LambdaRequest;
import java.util.Base64;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class AuthenticationUtils {
	private static final Logger logger = LoggerFactory.getLogger(AuthenticationUtils.class);

	public static String authenticateRequest(String authHeader, CustomerComponent customerComponent) {
		if (authHeader == null || !authHeader.startsWith("Basic ")) {
			logger.warn("Missing authorization token.");
			return null;
		}
		String usernameAndPassword = authHeader.substring(6);
		String decoded = new String(Base64.getDecoder().decode(usernameAndPassword));
		String[] parts = decoded.split(":");
		if (parts.length != 2) {
			logger.warn("Invalid auth, returning unauthorized: " + parts[0]);
			return null;
		}
		String customerId = Optional.ofNullable(customerComponent.findCustomerIdByAccessKey(parts[1]))
				.map(Customer::getCustomerId)
				.orElse(null);
		if (customerId != null) {
			TransactionUtil.updateCustomerId(customerId);
			return customerId;
		}
		logger.warn("Invalid token, returning unauthorized: " + parts[1]);
		return null;
	}

	public static String authenticateRequest(LambdaRequest request, CustomerComponent customerComponent) {
		return request != null && request.getHeaders() != null
				? authenticateRequest(request.getHeaders().getAuthorization(), customerComponent)
				: null;
	}

	public static String getCustomerIdFromRequest(LambdaRequest request) {
		return Optional.ofNullable(request.getRequestContext().getAuthorizer())
				.map(auth -> auth.getClaims().getUsername())
				.orElse(null);
	}
}

package com.bigboxer23.solar_moon.web;

import com.bigboxer23.solar_moon.customer.CustomerComponent;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.lambda.data.LambdaRequest;
import java.util.Base64;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import software.amazon.awssdk.utils.StringUtils;

/** */
@Slf4j
public class AuthenticationUtils {
	private static final String sourceUserId = System.getenv("SRC_USER");
	private static final String destinationUserId = System.getenv("DEST_USER");

	public static String authenticateRequest(String authHeader, CustomerComponent customerComponent) {
		if (authHeader == null || !authHeader.startsWith("Basic ")) {
			log.warn("Missing authorization token.");
			return null;
		}
		String usernameAndPassword = authHeader.substring(6);
		String decoded = new String(Base64.getDecoder().decode(usernameAndPassword));
		String[] parts = decoded.split(":");
		if (parts.length != 2) {
			log.warn("Invalid auth, returning unauthorized: " + parts[0]);
			return null;
		}
		String customerId = customerComponent
				.findCustomerIdByAccessKey(parts[1])
				.map(Customer::getCustomerId)
				.orElse(null);
		if (customerId != null) {
			TransactionUtil.updateCustomerId(customerId);
			return customerId;
		}
		log.warn("Invalid token, returning unauthorized: " + parts[1]);
		return null;
	}

	public static String authenticateRequest(LambdaRequest request, CustomerComponent customerComponent) {
		return request != null && request.getHeaders() != null
				? authenticateRequest(request.getHeaders().getAuthorization(), customerComponent)
				: null;
	}

	public static String getCustomerIdFromRequest(LambdaRequest request) {
		String customerId = Optional.ofNullable(request.getRequestContext().getAuthorizer())
				.map(auth -> auth.getClaims().getUsername())
				.orElse(null);
		if (!StringUtils.isEmpty(sourceUserId)
				&& !StringUtils.isEmpty(destinationUserId)
				&& sourceUserId.equalsIgnoreCase(customerId)) {
			MDC.put("customer.src", sourceUserId);
			return destinationUserId;
		}
		return customerId;
	}
}

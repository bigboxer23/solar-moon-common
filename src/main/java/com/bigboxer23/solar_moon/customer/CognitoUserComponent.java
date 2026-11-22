package com.bigboxer23.solar_moon.customer;

import static com.bigboxer23.solar_moon.util.PropertyConstants.AWS_REGION;
import static com.bigboxer23.solar_moon.util.PropertyConstants.COGNITO_USER_POOL_ID;

import com.bigboxer23.utils.properties.PropertyUtils;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

/**
 * Admin operations against the Cognito user pool. Usernames are the Cognito sub, i.e. customerId.
 */
@Slf4j
public class CognitoUserComponent {

	protected CognitoIdentityProviderClient getClient() {
		return CognitoIdentityProviderClient.builder()
				.region(Region.of(PropertyUtils.getProperty(AWS_REGION)))
				.credentialsProvider(DefaultCredentialsProvider.builder().build())
				.build();
	}

	protected String getUserPoolId() {
		return PropertyUtils.getProperty(COGNITO_USER_POOL_ID);
	}

	/**
	 * Sets the user's email and marks it verified, which also suppresses Cognito's verification
	 * code email. Only call this for an address that has already been verified (by the user
	 * confirming it in the app, or by support confirming it out of band).
	 *
	 * @throws
	 *     software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException
	 *     on failure, including AliasExistsException when another user already has the email
	 */
	public void updateEmail(String username, String email) {
		log.info("updating cognito email for " + username);
		try (CognitoIdentityProviderClient client = getClient()) {
			client.adminUpdateUserAttributes(AdminUpdateUserAttributesRequest.builder()
					.userPoolId(getUserPoolId())
					.username(username)
					.userAttributes(
							AttributeType.builder().name("email").value(email).build(),
							AttributeType.builder()
									.name("email_verified")
									.value("true")
									.build())
					.build());
		}
	}
}

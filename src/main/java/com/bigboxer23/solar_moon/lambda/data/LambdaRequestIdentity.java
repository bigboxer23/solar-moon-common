package com.bigboxer23.solar_moon.lambda.data;

import lombok.Data;

/** */
@Data
public class LambdaRequestIdentity {
	private String cognitoIdentityPoolId;
	private String accountId;
	private String cognitoIdentityId;
	private String caller;
	private String sourceIp;
	private String principalOrgId;
	private String accessKey;
	private String cognitoAuthenticationType;
	private String cognitoAuthenticationProvider;
	private String userArn;
	private String userAgent;
	private String user;
}

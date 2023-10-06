package com.bigboxer23.solar_moon.lambda.data;

import lombok.Data;

/** */
@Data
public class LambdaRequestContext {
	private LambdaRequestIdentity identity;
	private LambdaAuthorizer authorizer;
	private String resourcePath;
	private String httpMethod;
	private String extendedRequestId;
	private String requestTime;
	private String path;
	private String accountId;
	private String protocol;
	private String stage;
	private String domainPrefix;
	private long requestTimeEpoch;
	private String requestId;
	private String domainName;
	private String apiId;
}

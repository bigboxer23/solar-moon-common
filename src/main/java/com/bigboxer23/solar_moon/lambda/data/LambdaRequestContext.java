package com.bigboxer23.solar_moon.lambda.data;

import lombok.Data;

/** */
@Data
public class LambdaRequestContext {
	private LambdaRequestIdentity identity;
	private LambdaAuthorizer authorizer;
}

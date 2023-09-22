package com.bigboxer23.solar_moon.lambda.data;

import lombok.Data;

/** */
@Data
public class LambdaRequest {
	private String path;

	private LambdaRequestHeaders headers;

	private String resource;

	private String httpMethod;

	private String queryStringParameters;

	private String stageVariables;

	private String body;

	private LambdaRequestContext requestContext;
}

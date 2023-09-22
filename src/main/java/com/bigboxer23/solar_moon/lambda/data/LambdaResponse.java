package com.bigboxer23.solar_moon.lambda.data;

import lombok.Data;

/** */
@Data
public class LambdaResponse {
	private int statusCode;

	private boolean isBase64Encoded = false;

	private String body;

	private LambdaResponseHeaders headers;

	public LambdaResponse() {
		headers = new LambdaResponseHeaders();
	}

	public LambdaResponse(int statusCode, String body) {
		this(statusCode, body, null);
	}

	public LambdaResponse(int statusCode, String body, String contentType) {
		this();
		this.statusCode = statusCode;
		this.body = body;
		if (contentType != null) {
			headers.setContentType(contentType);
		}
	}
}

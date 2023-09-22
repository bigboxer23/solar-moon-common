package com.bigboxer23.solar_moon.lambda.data;

import com.squareup.moshi.Json;
import lombok.Data;

/** */
@Data
public class LambdaRequestHeaders {
	private String Authorization;

	@Json(name = "X-Forwarded-Proto")
	private String XForwardedProtocol;

	@Json(name = "X-Forwarded-For")
	private String XForwardedFor;

	@Json(name = "X-Forwarded-Port")
	private String XForwardedPort;

	@Json(name = "Host")
	private String host;

	@Json(name = "X-Amzn-Trace-Id")
	private String amazonTraceId;
}

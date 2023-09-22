package com.bigboxer23.solar_moon.lambda.data;

import com.squareup.moshi.Json;
import lombok.Data;

/** */
@Data
public class LambdaResponseHeaders {
	@Json(name = "Content-Type")
	private String contentType;
}

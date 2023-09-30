package com.bigboxer23.solar_moon.lambda.data;

import com.squareup.moshi.Json;
import lombok.Data;

/** */
@Data
public class CognitoUserAttributes {
	private String sub;
	private String email_verified;

	@Json(name = "cognito:user_status")
	private String cognito_user_status;

	@Json(name = "cognito:email_alias")
	private String cognito_email_alias;

	private String name;
	private String email;
}

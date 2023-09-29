package com.bigboxer23.solar_moon.lambda.data;

import lombok.Data;

/** */
@Data
public class LambdaClaims {
	private String origin_jti;
	private String sub;
	private String event_id;
	private String token_use;
	private String scope;
	private String auth_time;
	private String iss;
	private String exp;
	private String iat;
	private String client_id;
	private String jti;
	private String username;
}

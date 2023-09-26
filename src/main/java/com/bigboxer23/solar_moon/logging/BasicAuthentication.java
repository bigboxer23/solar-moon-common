package com.bigboxer23.solar_moon.logging;

import com.internetitem.logback.elasticsearch.config.Authentication;
import com.internetitem.logback.elasticsearch.util.Base64;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;

/** */
public class BasicAuthentication implements Authentication {
	@SneakyThrows
	@Override
	public void addAuth(HttpURLConnection urlConnection, String body) {
		String userInfo = urlConnection.getURL().getUserInfo();
		if (userInfo != null) {
			String basicAuth = "Basic "
					+ Base64.encode(
							URLDecoder.decode(userInfo, StandardCharsets.UTF_8).getBytes());
			urlConnection.setRequestProperty("Authorization", basicAuth);
		}
	}
}

package com.bigboxer23.solar_moon.logging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

public class BasicAuthenticationTest {

	@Test
	void testAddAuth_withUserInfo_setsBasicAuthorizationHeader() throws Exception {
		HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getURL()).thenReturn(url("https://user:password@opensearch.example.com/index"));

		new BasicAuthentication().addAuth(connection, "body");

		verify(connection)
				.setRequestProperty(
						"Authorization",
						"Basic "
								+ Base64.getEncoder().encodeToString("user:password".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void testAddAuth_withoutUserInfo_setsNoHeader() throws Exception {
		HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getURL()).thenReturn(url("https://opensearch.example.com/index"));

		new BasicAuthentication().addAuth(connection, "body");

		verify(connection, never()).setRequestProperty(eq("Authorization"), anyString());
	}

	@Test
	void testAddAuth_urlDecodesEscapedCredentials() throws Exception {
		HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getURL()).thenReturn(url("https://user:p%40ssword@opensearch.example.com/index"));

		new BasicAuthentication().addAuth(connection, "body");

		verify(connection)
				.setRequestProperty(
						"Authorization",
						"Basic "
								+ Base64.getEncoder().encodeToString("user:p@ssword".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void testAddAuth_ignoresBodyArgument() throws Exception {
		HttpURLConnection connection = mock(HttpURLConnection.class);
		when(connection.getURL()).thenReturn(url("https://user:password@opensearch.example.com/index"));

		assertDoesNotThrow(() -> new BasicAuthentication().addAuth(connection, null));

		verify(connection).setRequestProperty(eq("Authorization"), anyString());
	}

	private URL url(String spec) throws Exception {
		return URI.create(spec).toURL();
	}
}

package com.bigboxer23.solar_moon.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class TokenGeneratorTest {

	@Test
	public void testGenerateNewToken_returnsNonNullToken() {
		String token = TokenGenerator.generateNewToken();

		assertNotNull(token);
	}

	@Test
	public void testGenerateNewToken_returnsNonEmptyToken() {
		String token = TokenGenerator.generateNewToken();

		assertFalse(token.isEmpty());
	}

	@Test
	public void testGenerateNewToken_hasExpectedLength() {
		String token = TokenGenerator.generateNewToken();

		assertEquals(32, token.length());
	}

	@Test
	public void testGenerateNewToken_generatesUniqueTokens() {
		String token1 = TokenGenerator.generateNewToken();
		String token2 = TokenGenerator.generateNewToken();

		assertNotEquals(token1, token2);
	}

	@Test
	public void testGenerateNewToken_generatesMultipleUniqueTokens() {
		Set<String> tokens = new HashSet<>();
		int iterations = 100;

		for (int i = 0; i < iterations; i++) {
			tokens.add(TokenGenerator.generateNewToken());
		}

		assertEquals(iterations, tokens.size());
	}

	@Test
	public void testGenerateNewToken_isUrlSafeBase64() {
		String token = TokenGenerator.generateNewToken();

		assertFalse(token.contains("+"));
		assertFalse(token.contains("/"));
	}

	@Test
	public void testGenerateNewToken_canBeDecoded() {
		String token = TokenGenerator.generateNewToken();

		assertDoesNotThrow(() -> {
			byte[] decoded = Base64.getUrlDecoder().decode(token);
			assertEquals(24, decoded.length);
		});
	}

	@Test
	public void testGenerateNewToken_decodesToExpectedByteLength() {
		String token = TokenGenerator.generateNewToken();

		byte[] decoded = Base64.getUrlDecoder().decode(token);

		assertEquals(24, decoded.length);
	}

	@Test
	public void testGenerateNewToken_containsOnlyValidBase64UrlCharacters() {
		String token = TokenGenerator.generateNewToken();

		assertTrue(token.matches("^[A-Za-z0-9_-]+$"));
	}

	@Test
	public void testGenerateNewToken_multipleCallsProduceDifferentResults() {
		String token1 = TokenGenerator.generateNewToken();
		String token2 = TokenGenerator.generateNewToken();
		String token3 = TokenGenerator.generateNewToken();

		assertNotEquals(token1, token2);
		assertNotEquals(token2, token3);
		assertNotEquals(token1, token3);
	}

	@Test
	public void testGenerateNewToken_producesNoPadding() {
		String token = TokenGenerator.generateNewToken();

		assertFalse(token.contains("="));
	}

	@Test
	public void testGenerateNewToken_consistentLength() {
		for (int i = 0; i < 50; i++) {
			String token = TokenGenerator.generateNewToken();
			assertEquals(32, token.length());
		}
	}

	@Test
	public void testGenerateNewToken_decodedBytesAreNonZero() {
		String token = TokenGenerator.generateNewToken();
		byte[] decoded = Base64.getUrlDecoder().decode(token);

		boolean hasNonZeroByte = false;
		for (byte b : decoded) {
			if (b != 0) {
				hasNonZeroByte = true;
				break;
			}
		}

		assertTrue(hasNonZeroByte);
	}

	@Test
	public void testGenerateNewToken_randomnessDistribution() {
		Set<Character> firstChars = new HashSet<>();
		int iterations = 100;

		for (int i = 0; i < iterations; i++) {
			String token = TokenGenerator.generateNewToken();
			firstChars.add(token.charAt(0));
		}

		assertTrue(firstChars.size() > 10);
	}
}

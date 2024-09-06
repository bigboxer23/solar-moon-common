package com.bigboxer23.solar_moon.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TestSolectriaErrorOracle implements ISolectriaConstants {
	@Test
	public void rawErrorToCode() {
		assertEquals(0, SolectriaErrorOracle.rawErrorToCode(null));
		assertEquals(0, SolectriaErrorOracle.rawErrorToCode("null"));
		assertEquals(0, SolectriaErrorOracle.rawErrorToCode("NULL"));
		assertEquals(128, SolectriaErrorOracle.rawErrorToCode("128"));
		assertEquals(128, SolectriaErrorOracle.rawErrorToCode("128.000"));
	}

	@Test
	public void translateError() {
		// Various errors seen
		// 1088.000      (1024, 64)
		// 256.000       (256)
		// 2048          (2048)
		// 2304          (2048, 256)
		validateErrors(SolectriaErrorOracle.translateError("NULL", false), Collections.emptyList());
		validateErrors(SolectriaErrorOracle.translateError("0", false), Collections.emptyList());
		validateErrors(SolectriaErrorOracle.translateError("2048.000", false), List.of(Fan_Life_Reached));
		validateErrors(
				SolectriaErrorOracle.translateError("2304.000", false),
				Arrays.asList(Fan_Life_Reached, Power_Conversion_Current_Limit));
		validateErrors(SolectriaErrorOracle.translateError("256.000", false), List.of(Power_Conversion_Current_Limit));
		validateErrors(
				SolectriaErrorOracle.translateError("1088.000", false),
				Arrays.asList(Waiting_for_More_DC_Power, Low_DC_Power_Condition));
		validateErrors(
				SolectriaErrorOracle.translateError("1088", false),
				Arrays.asList(Waiting_for_More_DC_Power, Low_DC_Power_Condition));
	}

	private void validateErrors(String errorString, Collection<Integer> errors) {
		if (!errors.isEmpty()) {
			assertEquals(errors.size(), errorString.split("\n").length);
		}
		errors.forEach(code -> assertTrue(errorString.contains(INFORMATIVE_ERROR_CODES.get(code))));
	}
}

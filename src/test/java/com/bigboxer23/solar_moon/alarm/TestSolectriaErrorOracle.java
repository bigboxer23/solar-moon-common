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
		validateErrors("NULL", Collections.emptyList(), false);
		validateErrors("0", Collections.emptyList(), false);
		validateErrors("2048.000", List.of(Fan_Life_Reached), false);
		validateErrors("2304.000", Arrays.asList(Fan_Life_Reached, Power_Conversion_Current_Limit), false);
		validateErrors("256.000", List.of(Power_Conversion_Current_Limit), false);
		validateErrors("1088.000", Arrays.asList(Waiting_for_More_DC_Power, Low_DC_Power_Condition), false);
		validateErrors("1088", Arrays.asList(Waiting_for_More_DC_Power, Low_DC_Power_Condition), false);

		validateErrors("NULL", Collections.emptyList(), true);
		validateErrors("0", Collections.emptyList(), true);
		validateErrors("1024.000", List.of(MAG_Failure), true);
		validateErrors("1280.000", Arrays.asList(MAG_Failure, VAC_Sense_Circuit_Failure), true);
		validateErrors("256.000", List.of(Power_Conversion_Current_Limit), true);
		validateErrors("1088.000", Arrays.asList(Waiting_for_More_DC_Power, Low_DC_Power_Condition), true);
		validateErrors("1088", Arrays.asList(Waiting_for_More_DC_Power, Low_DC_Power_Condition), true);
	}

	private void validateErrors(String rawError, Collection<Integer> errors, boolean criticalError) {
		String errorString = SolectriaErrorOracle.translateError(rawError, criticalError);
		if (!errors.isEmpty()) {
			assertEquals(errors.size(), errorString.split("\n").length);
		}
		errors.forEach(code -> assertTrue(
				errorString.contains((criticalError ? CRITICAL_ERROR_CODES : INFORMATIVE_ERROR_CODES).get(code))));
	}
}

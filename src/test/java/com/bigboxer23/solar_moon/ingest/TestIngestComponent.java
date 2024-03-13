package com.bigboxer23.solar_moon.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bigboxer23.solar_moon.IComponentRegistry;
import org.junit.jupiter.api.Test;

/** */
public class TestIngestComponent implements IComponentRegistry, MeterConstants {
	@Test
	public void maybeCorrectForRollover() {
		assertEquals(
				OBVIOUS_ROLLOVER_MARGIN, generationComponent.maybeCorrectForRollover(10000, OBVIOUS_ROLLOVER_MARGIN));
		assertEquals(OBVIOUS_ROLLOVER_MARGIN, generationComponent.maybeCorrectForRollover(0, OBVIOUS_ROLLOVER_MARGIN));
		assertEquals(
				OBVIOUS_ROLLOVER_MARGIN, generationComponent.maybeCorrectForRollover(999, OBVIOUS_ROLLOVER_MARGIN));
		assertEquals(
				OBVIOUS_ROLLOVER_MARGIN, generationComponent.maybeCorrectForRollover(999, OBVIOUS_ROLLOVER_MARGIN));
		assertEquals(
				OBVIOUS_ROLLOVER + OBVIOUS_ROLLOVER_MARGIN,
				generationComponent.maybeCorrectForRollover(
						OBVIOUS_ROLLOVER, OBVIOUS_ROLLOVER + OBVIOUS_ROLLOVER_MARGIN));
		assertEquals(
				OBVIOUS_ROLLOVER_MARGIN,
				generationComponent.maybeCorrectForRollover(OBVIOUS_ROLLOVER - 1, OBVIOUS_ROLLOVER_MARGIN));
		assertEquals(
				OBVIOUS_ROLLOVER + OBVIOUS_ROLLOVER_MARGIN - 1,
				generationComponent.maybeCorrectForRollover(OBVIOUS_ROLLOVER - 1, OBVIOUS_ROLLOVER_MARGIN - 1));
		assertEquals(
				OBVIOUS_ROLLOVER + 10,
				generationComponent.maybeCorrectForRollover(OBVIOUS_ROLLOVER - (OBVIOUS_ROLLOVER_MARGIN - 1), 10));
		assertEquals(
				OBVIOUS_ROLLOVER + 10,
				generationComponent.maybeCorrectForRollover(OBVIOUS_ROLLOVER - (OBVIOUS_ROLLOVER_MARGIN - 1), 10));
		assertEquals(10, generationComponent.maybeCorrectForRollover(OBVIOUS_ROLLOVER - OBVIOUS_ROLLOVER_MARGIN, 10));
	}
}

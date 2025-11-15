package com.bigboxer23.solar_moon.search.status;

import java.util.Optional;

public interface OpenSearchStatusRepository {
	void storeFailure();

	boolean hasFailureWithinLastThirtyMinutes();

	Optional<OpenSearchStatus> getMostRecentStatus();
}

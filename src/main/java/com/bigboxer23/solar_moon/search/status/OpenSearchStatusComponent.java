package com.bigboxer23.solar_moon.search.status;

import com.bigboxer23.solar_moon.IComponentRegistry;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenSearchStatusComponent {

	private OpenSearchStatusRepository repository;

	protected OpenSearchStatusRepository getRepository() {
		if (repository == null) {
			repository = new DynamoDbOpenSearchStatusRepository();
		}
		return repository;
	}

	public void storeFailure() {
		getRepository().storeFailure();
	}

	public boolean hasFailureWithinLastThirtyMinutes() {
		return getRepository().hasFailureWithinLastThirtyMinutes();
	}

	public Optional<OpenSearchStatus> getMostRecentStatus() {
		return getRepository().getMostRecentStatus();
	}

	public void checkAvailability() {
		if (!IComponentRegistry.OSComponent.isOpenSearchAvailable()) {
			storeFailure();
		}
	}
}

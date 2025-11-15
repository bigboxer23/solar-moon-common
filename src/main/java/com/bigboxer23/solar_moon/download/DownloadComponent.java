package com.bigboxer23.solar_moon.download;

import com.bigboxer23.solar_moon.data.DownloadRequest;
import java.util.Optional;

public class DownloadComponent {

	private static final int recordsPerDayPerDevice = 96;
	private static final int maxOpenSearchPageSize = 10000;

	private final DownloadRepository repository;

	public DownloadComponent() {
		this(new DynamoDbDownloadRepository());
	}

	protected DownloadComponent(DownloadRepository repository) {
		this.repository = repository;
	}

	public int getPageSizeDays(int deviceCount) {
		return deviceCount == 0
				? 0
				: Double.valueOf((double) maxOpenSearchPageSize / (recordsPerDayPerDevice * deviceCount))
						.intValue();
	}

	public Optional<DownloadRequest> findByRequestId(String requestId) {
		return repository.findByRequestId(requestId);
	}

	public DownloadRequest add(DownloadRequest downloadRequest) {
		return repository.add(downloadRequest);
	}

	public Optional<DownloadRequest> update(DownloadRequest downloadRequest) {
		return repository.update(downloadRequest);
	}

	public void delete(DownloadRequest downloadRequest) {
		repository.delete(downloadRequest);
	}

	protected DownloadRepository getRepository() {
		return repository;
	}
}

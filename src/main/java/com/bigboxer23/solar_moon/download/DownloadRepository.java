package com.bigboxer23.solar_moon.download;

import com.bigboxer23.solar_moon.data.DownloadRequest;
import java.util.Optional;

public interface DownloadRepository {

	Optional<DownloadRequest> findByRequestId(String requestId);

	DownloadRequest add(DownloadRequest downloadRequest);

	Optional<DownloadRequest> update(DownloadRequest downloadRequest);

	void delete(DownloadRequest downloadRequest);
}

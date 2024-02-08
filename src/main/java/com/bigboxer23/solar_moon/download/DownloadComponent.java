package com.bigboxer23.solar_moon.download;

import com.bigboxer23.solar_moon.data.DownloadRequest;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class DownloadComponent extends AbstractDynamodbComponent<DownloadRequest> {
	private static final Logger logger = LoggerFactory.getLogger(DownloadComponent.class);

	private static final int recordsPerDayPerDevice = 96;
	private static final int maxOpenSearchPageSize = 10000;

	public int getPageSizeDays(int deviceCount) {
		return deviceCount == 0
				? 0
				: Double.valueOf((double) maxOpenSearchPageSize / (recordsPerDayPerDevice * deviceCount))
						.intValue();
	}

	@Override
	protected String getTableName() {
		return "downloadRequest";
	}

	@Override
	protected Class<DownloadRequest> getObjectClass() {
		return DownloadRequest.class;
	}
}

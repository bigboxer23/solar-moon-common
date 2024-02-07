package com.bigboxer23.solar_moon.download;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.DownloadRequest;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import com.bigboxer23.solar_moon.ingest.MeterConstants;
import com.bigboxer23.solar_moon.lambda.utils.PropertyUtils;
import com.bigboxer23.solar_moon.search.OpenSearchConstants;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** */
public class DownloadComponent extends AbstractDynamodbComponent<DownloadRequest> {
	private static final Logger logger = LoggerFactory.getLogger(DownloadComponent.class);

	private final String[] HEADERS = {
		"Time",
		"Site",
		"Device Name",
		"Total Energy Consumption (kWH)",
		"Total Energy Consumption (kWH)",
		"Energy Consumed (kWH)"
	};

	private final CSVFormat csvFormat =
			CSVFormat.DEFAULT.builder().setHeader(HEADERS).build();

	private static final int recordsPerDayPerDevice = 96;
	private static final int maxOpenSearchPageSize = 10000;

	private static S3Client s3;

	protected int getPageSizeDays(int deviceCount) {
		return deviceCount == 0
				? 0
				: Double.valueOf((double) maxOpenSearchPageSize / (recordsPerDayPerDevice * deviceCount))
						.intValue();
	}

	public void download(SearchJSON searchJSON) throws IOException {
		getS3Client()
				.putObject(
						PutObjectRequest.builder()
								.bucket(PropertyUtils.getProperty("download.s3.bucket"))
								.key(getNameFromSearch(searchJSON))
								.build(),
						RequestBody.fromString(getCSV(searchJSON)));
	}

	private String getCSV(SearchJSON searchJSON) throws IOException {
		int deviceCount =
				IComponentRegistry.OSComponent.getDevicesFacet(searchJSON).size();
		long interval = getPageSizeDays(deviceCount);
		StringBuilder csv = new StringBuilder();
		long time = System.currentTimeMillis();
		long cumulative = 0;

		logger.debug("device count: " + deviceCount);
		logger.debug("interval: " + interval);
		interval = interval * TimeConstants.DAY;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		logger.debug(format.format(searchJSON.getJavaStartDate())
				+ " - "
				+ format.format(searchJSON.getJavaEndDate())
				+ "\n\n");
		try (final CSVPrinter printer = new CSVPrinter(csv, csvFormat)) {
			for (long end = searchJSON.getEndDate(); searchJSON.getStartDate() < end; end -= interval) {
				SearchJSON search = new SearchJSON(searchJSON);
				search.setEndDate(end);
				search.setStartDate(Math.max(searchJSON.getStartDate(), end - interval + 1));

				SearchResponse response = IComponentRegistry.OSComponent.search(search);
				cumulative += response.took();
				logger.debug("took:" + response.took() + " cum:" + cumulative);
				if (response.hits().total().value() == 0) {
					printer.flush();
					logger.debug("time " + (System.currentTimeMillis() - time));
					return csv.toString();
				}
				logger.debug("---: "
						+ format.format(search.getJavaStartDate())
						+ " - "
						+ format.format(search.getJavaEndDate())
						+ "  hits: "
						+ response.hits().hits().size());
				response.hits().hits().forEach(h -> {
					Map<String, Object> fields = (Map<String, Object>) ((Hit) h).source();

					try {
						printer.printRecord(
								TimeUtils.getFormattedZonedTime(
										(Long) fields.get(OpenSearchConstants.TIMESTAMP), searchJSON.getTimeZone()),
								fields.get(MeterConstants.SITE),
								fields.get(MeterConstants.DEVICE_NAME),
								fields.get(MeterConstants.TOTAL_ENG_CONS),
								fields.get(MeterConstants.TOTAL_REAL_POWER),
								fields.get(MeterConstants.ENG_CONS));
					} catch (IOException theE) {

					}
				});
			}
		}
		logger.info("time " + (System.currentTimeMillis() - time) / 1000);
		return csv.toString();
	}

	private S3Client getS3Client() {
		if (s3 == null) {
			s3 = S3Client.builder()
					.region(Region.of(PropertyUtils.getProperty("aws.region")))
					.build();
		}
		return s3;
	}

	private String getNameFromSearch(SearchJSON searchJSON) {
		String fileName = TimeUtils.getFormattedZonedTime(searchJSON.getStartDate(), searchJSON.getTimeZone())
				+ " - "
				+ TimeUtils.getFormattedZonedTime(searchJSON.getEndDate(), searchJSON.getTimeZone());

		if (searchJSON.getSite() != null) {
			fileName += " - " + searchJSON.getSite();
		}
		if (searchJSON.getDeviceName() != null) {
			fileName += " - " + fileName + " - " + searchJSON.getDeviceName();
		}
		return fileName + ".csv";
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

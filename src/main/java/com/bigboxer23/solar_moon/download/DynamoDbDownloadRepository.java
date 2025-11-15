package com.bigboxer23.solar_moon.download;

import com.bigboxer23.solar_moon.data.DownloadRequest;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.utils.StringUtils;

@Slf4j
public class DynamoDbDownloadRepository extends AbstractDynamodbComponent<DownloadRequest>
		implements DownloadRepository {

	@Override
	public Optional<DownloadRequest> findByRequestId(String requestId) {
		if (StringUtils.isBlank(requestId)) {
			return Optional.empty();
		}
		return Optional.ofNullable(getTable().getItem(new DownloadRequest() {
			{
				setRequestId(requestId);
			}
		}));
	}

	@Override
	public DownloadRequest add(DownloadRequest downloadRequest) {
		getTable().putItem(downloadRequest);
		return downloadRequest;
	}

	@Override
	public Optional<DownloadRequest> update(DownloadRequest downloadRequest) {
		return Optional.ofNullable(getTable().updateItem(builder -> builder.item(downloadRequest)));
	}

	@Override
	public void delete(DownloadRequest downloadRequest) {
		getTable().deleteItem(downloadRequest);
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

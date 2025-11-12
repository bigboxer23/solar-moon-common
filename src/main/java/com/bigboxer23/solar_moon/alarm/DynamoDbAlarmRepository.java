package com.bigboxer23.solar_moon.alarm;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.dynamodb.AuditableAbstractDynamodbRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

@Slf4j
public class DynamoDbAlarmRepository extends AuditableAbstractDynamodbRepository<Alarm>
		implements AlarmRepository, IAlarmConstants {

	@Override
	public Optional<Alarm> findMostRecentAlarm(String deviceId) {
		List<Alarm> alarms = getTable()
				.index(Alarm.DEVICEID_STARTDATE_INDEX)
				.query(theBuilder -> theBuilder
						.limit(1)
						.scanIndexForward(false)
						.queryConditional(QueryConditional.keyEqualTo(builder -> builder.partitionValue(deviceId))))
				.stream()
				.findFirst()
				.map(Page::items)
				.orElse(Collections.emptyList());
		return !alarms.isEmpty() ? Optional.ofNullable(alarms.getFirst()) : Optional.empty();
	}

	@Override
	public List<Alarm> findAlarmsByDevice(String customerId, String deviceId) {
		return findAlarmsByIndex(Alarm.DEVICE_CUSTOMER_INDEX, deviceId, customerId);
	}

	@Override
	public List<Alarm> findAlarmsBySite(String customerId, String siteId) {
		return findAlarmsByIndex(Alarm.SITE_CUSTOMER_INDEX, siteId, customerId);
	}

	@Override
	public List<Alarm> findNonEmailedAlarms(String customerId) {
		return getTable()
				.index(Alarm.EMAILED_CUSTOMER_INDEX)
				.query(QueryConditional.keyEqualTo(
						builder -> builder.partitionValue(NEEDS_EMAIL).sortValue(customerId)))
				.stream()
				.flatMap(page -> page.items().stream())
				.toList();
	}

	@Override
	public List<Alarm> findNonEmailedActiveAlarms() {
		return findNonEmailed(Alarm.EMAILED_CUSTOMER_INDEX);
	}

	@Override
	public List<Alarm> findNonEmailedResolvedAlarms() {
		return findNonEmailed(Alarm.RESOLVED_EMAILED_INDEX);
	}

	@Override
	public List<Alarm> findAlarms(String customerId) {
		if (StringUtils.isBlank(customerId)) {
			return Collections.emptyList();
		}
		return getTable()
				.index(Alarm.CUSTOMER_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(customerId)))
				.stream()
				.flatMap(page -> page.items().stream())
				.toList();
	}

	@Override
	public Optional<Alarm> findAlarmByAlarmId(String alarmId, String customerId) {
		return !StringUtils.isBlank(alarmId) && !StringUtils.isBlank(customerId)
				? Optional.ofNullable(this.getTable().getItem(new Alarm(alarmId, customerId)))
				: Optional.empty();
	}

	@Override
	public List<Alarm> findActiveAlarms() {
		return getTable()
				.index(Alarm.STATE_CUSTOMER_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(ACTIVE)))
				.stream()
				.flatMap(page -> page.items().stream())
				.toList();
	}

	@Override
	public List<Alarm> findAlarmsByStateAndDateLessThan(int state, long deleteOlderThan) {
		return getTable()
				.index(Alarm.STATE_STARTDATE_INDEX)
				.query(QueryConditional.sortLessThan(
						builder -> builder.partitionValue(state).sortValue(deleteOlderThan)))
				.stream()
				.flatMap(page -> page.items().stream())
				.toList();
	}

	@Override
	public void delete(Alarm alarm) {
		getTable().deleteItem(alarm);
	}

	private List<Alarm> findNonEmailed(String indexName) {
		return getTable()
				.index(indexName)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(NEEDS_EMAIL)))
				.stream()
				.flatMap(page -> page.items().stream())
				.toList();
	}

	private List<Alarm> findAlarmsByIndex(String indexName, String partitionId, String sort) {
		return getTable()
				.index(indexName)
				.query(QueryConditional.keyEqualTo(
						builder -> builder.partitionValue(partitionId).sortValue(sort)))
				.stream()
				.flatMap(page -> page.items().stream())
				.toList();
	}

	@Override
	protected String getTableName() {
		return "alarms";
	}

	@Override
	protected Class<Alarm> getObjectClass() {
		return Alarm.class;
	}
}

package com.bigboxer23.solar_moon.alarm;

import com.bigboxer23.solar_moon.data.Alarm;
import java.util.List;
import java.util.Optional;

public interface AlarmRepository {

	Optional<Alarm> findMostRecentAlarm(String deviceId);

	List<Alarm> findAlarmsByDevice(String customerId, String deviceId);

	List<Alarm> findAlarmsBySite(String customerId, String siteId);

	List<Alarm> findNonEmailedAlarms(String customerId);

	List<Alarm> findNonEmailedActiveAlarms();

	List<Alarm> findNonEmailedResolvedAlarms();

	List<Alarm> findAlarms(String customerId);

	Optional<Alarm> findAlarmByAlarmId(String alarmId, String customerId);

	List<Alarm> findActiveAlarms();

	List<Alarm> findAlarmsByStateAndDateLessThan(int state, long deleteOlderThan);

	Alarm add(Alarm alarm);

	Optional<Alarm> update(Alarm alarm);

	void delete(Alarm alarm);
}

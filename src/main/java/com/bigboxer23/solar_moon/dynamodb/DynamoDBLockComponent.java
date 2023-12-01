package com.bigboxer23.solar_moon.dynamodb;

import com.bigboxer23.solar_moon.data.DynamoDBLock;
import com.bigboxer23.solar_moon.util.TableCreationUtils;

/** */
public class DynamoDBLockComponent extends AbstractDynamodbComponent<DynamoDBLock> {
	public static final String LOCK_TABLE = "lock_table";

	@Override
	protected String getTableName() {
		return LOCK_TABLE;
	}

	@Override
	protected Class<DynamoDBLock> getObjectClass() {
		return DynamoDBLock.class;
	}

	public void createDBLockTable() {
		TableCreationUtils.createTable(null, getTable());
	}
}

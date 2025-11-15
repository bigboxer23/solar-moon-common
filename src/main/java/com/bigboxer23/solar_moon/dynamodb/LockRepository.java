package com.bigboxer23.solar_moon.dynamodb;

import com.bigboxer23.solar_moon.data.DynamoDBLock;
import java.util.Optional;

public interface LockRepository {

	Optional<DynamoDBLock> findByKey(String key);

	DynamoDBLock add(DynamoDBLock lock);

	Optional<DynamoDBLock> update(DynamoDBLock lock);

	void delete(DynamoDBLock lock);

	void createTable();
}

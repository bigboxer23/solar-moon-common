package com.bigboxer23.solar_moon.dynamodb;

import com.bigboxer23.solar_moon.data.DynamoDBLock;
import com.bigboxer23.solar_moon.util.TableCreationUtils;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

@Slf4j
public class DynamoDbLockRepository extends AuditableAbstractDynamodbRepository<DynamoDBLock>
		implements LockRepository {

	public static final String LOCK_TABLE = "lock_table";

	@Override
	public Optional<DynamoDBLock> findByKey(String key) {
		return !StringUtils.isBlank(key)
				? this.getTable().query(QueryConditional.keyEqualTo((builder) -> builder.partitionValue(key))).stream()
						.findFirst()
						.flatMap((page) -> page.items().stream().findFirst())
				: Optional.empty();
	}

	@Override
	public void delete(DynamoDBLock lock) {
		getTable().deleteItem(lock);
	}

	@Override
	public void createTable() {
		TableCreationUtils.createTable(null, getTable());
	}

	@Override
	protected String getTableName() {
		return LOCK_TABLE;
	}

	@Override
	protected Class<DynamoDBLock> getObjectClass() {
		return DynamoDBLock.class;
	}
}

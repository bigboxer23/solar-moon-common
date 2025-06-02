package com.bigboxer23.solar_moon.dynamodb;

import com.bigboxer23.solar_moon.data.AuditableEntity;

import java.util.Optional;

/** */
public abstract class AuditableAbstractDynamodbComponent<T extends AuditableEntity> extends AbstractDynamodbComponent<T>
{
	public T add(T entity) {
		entity.markCreated();
		getTable().putItem(entity);
		return entity;
	}

	public Optional<T> update(T entity) {
		entity.markUpdated();
		return Optional.ofNullable(getTable().updateItem(builder -> builder.item(entity)));
	}
}

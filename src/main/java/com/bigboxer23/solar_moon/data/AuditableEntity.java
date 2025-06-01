package com.bigboxer23.solar_moon.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** */
@Data
public abstract class AuditableEntity {

	@Schema(description = "timestamp when the record was created")
	private long createdAt;

	@Schema(description = "timestamp when the record was updated")
	private long updatedAt;

	public void markCreated() {
		setCreatedAt(System.currentTimeMillis());
		markUpdated();
	}

	public void markUpdated() {
		setUpdatedAt(System.currentTimeMillis());
	}
}

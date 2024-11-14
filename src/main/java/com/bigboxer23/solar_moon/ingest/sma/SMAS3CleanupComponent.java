package com.bigboxer23.solar_moon.ingest.sma;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.utils.properties.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/** Contains logic to look through S3 bucket and cleanup/remove any empty directory structure */
public class SMAS3CleanupComponent {
	private static final Logger logger = LoggerFactory.getLogger(SMAS3CleanupComponent.class);

	private final String bucket = PropertyUtils.getProperty("ftp.s3.bucket");

	// TODO: look at logic to deal w/stale data

	public void cleanupEmptyFTPS3Folders() {

		int count = 0;
		String continuationToken = null;
		do {
			ListObjectsV2Response response = IComponentRegistry.smaIngestComponent
					.getS3Client()
					.listObjectsV2(ListObjectsV2Request.builder()
							.bucket(bucket)
							.continuationToken(continuationToken)
							.build());
			for (S3Object object : response.contents()) {
				if (isEmptyFolder(object.key())) {
					logger.info(count + " deleting " + object.key());
					IComponentRegistry.smaIngestComponent
							.getS3Client()
							.deleteObject(DeleteObjectRequest.builder()
									.bucket(bucket)
									.key(object.key())
									.build());
				}
				count++;
			}
			continuationToken = response.nextContinuationToken();
		} while (continuationToken != null);
	}

	public boolean isEmptyFolder(String folderKey) {
		if (!folderKey.endsWith("/")) {
			return false;
		}
		if (folderKey.split("/").length < 2) {
			return false; // Root folder
		}
		// Check if more than 1 object is found
		for (S3Object s3Object : IComponentRegistry.smaIngestComponent
				.getS3Client()
				.listObjectsV2(ListObjectsV2Request.builder()
						.bucket(bucket)
						.prefix(folderKey)
						.maxKeys(2)
						.build())
				.contents()) {
			if (!s3Object.key().equals(folderKey)) {
				return false; // Folder has contents other than itself
			}
		}
		// Only the folder key itself is found, hence it's empty
		return true;
	}
}

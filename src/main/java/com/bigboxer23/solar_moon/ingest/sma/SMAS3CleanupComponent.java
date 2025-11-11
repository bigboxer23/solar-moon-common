package com.bigboxer23.solar_moon.ingest.sma;

import static com.bigboxer23.solar_moon.util.PropertyConstants.FTP_S3_BUCKET;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.utils.properties.PropertyUtils;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/** Contains logic to look through S3 bucket and cleanup/remove any empty directory structure */
@Slf4j
public class SMAS3CleanupComponent {
	protected String getBucket() {
		return PropertyUtils.getProperty(FTP_S3_BUCKET);
	}

	protected S3Client getS3Client() {
		return IComponentRegistry.smaIngestComponent.getS3Client();
	}

	public void cleanupEmptyFTPS3Folders() {

		int count = 0;
		String continuationToken = null;
		do {
			ListObjectsV2Response response = getS3Client()
					.listObjectsV2(ListObjectsV2Request.builder()
							.bucket(getBucket())
							.continuationToken(continuationToken)
							.build());
			for (S3Object object : response.contents()) {
				log.info(count + " checking " + object.key());
				if (isEmptyFolder(object.key())) {
					log.info(count + " deleting " + object.key());
					getS3Client()
							.deleteObject(DeleteObjectRequest.builder()
									.bucket(getBucket())
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
		for (S3Object s3Object : getS3Client()
				.listObjectsV2(ListObjectsV2Request.builder()
						.bucket(getBucket())
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

	/**
	 * Deletes a non-empty S3 folder and all its contents.
	 *
	 * @param folderPrefix the folder prefix to delete (will automatically append "/" if missing)
	 */
	public void deleteS3Folder(String folderPrefix) {
		if (!folderPrefix.endsWith("/")) {
			folderPrefix += "/";
		}

		log.info("Deleting S3 folder: {}", folderPrefix);

		ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
				.bucket(getBucket())
				.prefix(folderPrefix)
				.build();

		ListObjectsV2Response listResponse;
		int totalDeleted = 0;

		do {
			listResponse = getS3Client().listObjectsV2(listRequest);

			if (!listResponse.contents().isEmpty()) {
				List<ObjectIdentifier> keys = listResponse.contents().stream()
						.map(s3Object ->
								ObjectIdentifier.builder().key(s3Object.key()).build())
						.collect(Collectors.toList());

				DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
						.bucket(getBucket())
						.delete(Delete.builder().objects(keys).build())
						.build();

				DeleteObjectsResponse deleteResponse = getS3Client().deleteObjects(deleteRequest);

				totalDeleted += deleteResponse.deleted().size();
				log.info("Deleted {} objects from {}", deleteResponse.deleted().size(), folderPrefix);

				if (!deleteResponse.errors().isEmpty()) {
					deleteResponse
							.errors()
							.forEach(error -> log.error("Failed to delete {}: {}", error.key(), error.message()));
				}
			}

			listRequest = listRequest.toBuilder()
					.continuationToken(listResponse.nextContinuationToken())
					.build();

		} while (listResponse.isTruncated());

		log.info("Completed deletion of folder {}. Total objects deleted: {}", folderPrefix, totalDeleted);
	}
}

package com.bigboxer23.solar_moon.ingest.sma;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@ExtendWith(MockitoExtension.class)
class SMAS3CleanupComponentTest {

	private SMAS3CleanupComponent component;
	private S3Client mockS3Client;

	@BeforeEach
	void setUp() {
		mockS3Client = mock(S3Client.class);

		component = new SMAS3CleanupComponent() {
			@Override
			protected S3Client getS3Client() {
				return mockS3Client;
			}
		};
	}

	@Test
	void testDeleteS3Folder_withoutTrailingSlash() {
		String folderPrefix = "test-folder";
		String expectedPrefix = "test-folder/";

		S3Object obj1 = S3Object.builder().key("test-folder/file1.txt").build();
		S3Object obj2 = S3Object.builder().key("test-folder/file2.txt").build();

		ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
				.contents(obj1, obj2)
				.isTruncated(false)
				.build();

		when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);

		DeletedObject deleted1 =
				DeletedObject.builder().key("test-folder/file1.txt").build();
		DeletedObject deleted2 =
				DeletedObject.builder().key("test-folder/file2.txt").build();

		DeleteObjectsResponse deleteResponse =
				DeleteObjectsResponse.builder().deleted(deleted1, deleted2).build();

		when(mockS3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteResponse);

		component.deleteS3Folder(folderPrefix);

		ArgumentCaptor<ListObjectsV2Request> listCaptor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
		verify(mockS3Client).listObjectsV2(listCaptor.capture());
		assertEquals(expectedPrefix, listCaptor.getValue().prefix());

		verify(mockS3Client).deleteObjects(any(DeleteObjectsRequest.class));
	}

	@Test
	void testDeleteS3Folder_withTrailingSlash() {
		String folderPrefix = "test-folder/";

		S3Object obj1 = S3Object.builder().key("test-folder/file1.txt").build();

		ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
				.contents(obj1)
				.isTruncated(false)
				.build();

		when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);

		DeletedObject deleted1 =
				DeletedObject.builder().key("test-folder/file1.txt").build();

		DeleteObjectsResponse deleteResponse =
				DeleteObjectsResponse.builder().deleted(deleted1).build();

		when(mockS3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteResponse);

		component.deleteS3Folder(folderPrefix);

		ArgumentCaptor<ListObjectsV2Request> listCaptor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
		verify(mockS3Client).listObjectsV2(listCaptor.capture());
		assertEquals(folderPrefix, listCaptor.getValue().prefix());

		verify(mockS3Client).deleteObjects(any(DeleteObjectsRequest.class));
	}

	@Test
	void testDeleteS3Folder_emptyFolder() {
		String folderPrefix = "empty-folder/";

		ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
				.contents(List.of())
				.isTruncated(false)
				.build();

		when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);

		component.deleteS3Folder(folderPrefix);

		verify(mockS3Client).listObjectsV2(any(ListObjectsV2Request.class));

		verify(mockS3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
	}

	@Test
	void testDeleteS3Folder_paginatedResults() {
		String folderPrefix = "large-folder/";

		S3Object obj1 = S3Object.builder().key("large-folder/file1.txt").build();
		S3Object obj2 = S3Object.builder().key("large-folder/file2.txt").build();

		ListObjectsV2Response listResponse1 = ListObjectsV2Response.builder()
				.contents(obj1, obj2)
				.isTruncated(true)
				.nextContinuationToken("token123")
				.build();

		S3Object obj3 = S3Object.builder().key("large-folder/file3.txt").build();

		ListObjectsV2Response listResponse2 = ListObjectsV2Response.builder()
				.contents(obj3)
				.isTruncated(false)
				.build();

		when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse1, listResponse2);

		DeletedObject deleted1 =
				DeletedObject.builder().key("large-folder/file1.txt").build();
		DeletedObject deleted2 =
				DeletedObject.builder().key("large-folder/file2.txt").build();
		DeletedObject deleted3 =
				DeletedObject.builder().key("large-folder/file3.txt").build();

		DeleteObjectsResponse deleteResponse1 =
				DeleteObjectsResponse.builder().deleted(deleted1, deleted2).build();
		DeleteObjectsResponse deleteResponse2 =
				DeleteObjectsResponse.builder().deleted(deleted3).build();

		when(mockS3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteResponse1, deleteResponse2);

		component.deleteS3Folder(folderPrefix);

		verify(mockS3Client, times(2)).listObjectsV2(any(ListObjectsV2Request.class));

		verify(mockS3Client, times(2)).deleteObjects(any(DeleteObjectsRequest.class));
	}

	@Test
	void testDeleteS3Folder_withErrors() {
		String folderPrefix = "problem-folder/";

		S3Object obj1 = S3Object.builder().key("problem-folder/file1.txt").build();
		S3Object obj2 = S3Object.builder().key("problem-folder/file2.txt").build();

		ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
				.contents(obj1, obj2)
				.isTruncated(false)
				.build();

		when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);

		DeletedObject deleted1 =
				DeletedObject.builder().key("problem-folder/file1.txt").build();

		S3Error error = S3Error.builder()
				.key("problem-folder/file2.txt")
				.code("AccessDenied")
				.message("Access Denied")
				.build();

		DeleteObjectsResponse deleteResponse =
				DeleteObjectsResponse.builder().deleted(deleted1).errors(error).build();

		when(mockS3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteResponse);

		assertDoesNotThrow(() -> component.deleteS3Folder(folderPrefix));

		verify(mockS3Client).listObjectsV2(any(ListObjectsV2Request.class));
		verify(mockS3Client).deleteObjects(any(DeleteObjectsRequest.class));
	}

	@Test
	void testDeleteS3Folder_verifiesBatchDeletion() {
		String folderPrefix = "batch-folder/";

		S3Object obj1 = S3Object.builder().key("batch-folder/file1.txt").build();
		S3Object obj2 = S3Object.builder().key("batch-folder/file2.txt").build();
		S3Object obj3 = S3Object.builder().key("batch-folder/file3.txt").build();

		ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
				.contents(obj1, obj2, obj3)
				.isTruncated(false)
				.build();

		when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);

		DeletedObject deleted1 =
				DeletedObject.builder().key("batch-folder/file1.txt").build();
		DeletedObject deleted2 =
				DeletedObject.builder().key("batch-folder/file2.txt").build();
		DeletedObject deleted3 =
				DeletedObject.builder().key("batch-folder/file3.txt").build();

		DeleteObjectsResponse deleteResponse = DeleteObjectsResponse.builder()
				.deleted(deleted1, deleted2, deleted3)
				.build();

		when(mockS3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteResponse);

		component.deleteS3Folder(folderPrefix);

		ArgumentCaptor<DeleteObjectsRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
		verify(mockS3Client).deleteObjects(deleteCaptor.capture());

		DeleteObjectsRequest capturedRequest = deleteCaptor.getValue();
		assertEquals(3, capturedRequest.delete().objects().size());
	}
}

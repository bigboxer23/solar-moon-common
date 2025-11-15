package com.bigboxer23.solar_moon.download;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.DownloadRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DownloadComponentTest {

	@Mock
	private DownloadRepository mockRepository;

	private TestableDownloadComponent downloadComponent;

	private static class TestableDownloadComponent extends DownloadComponent {
		private final DownloadRepository repository;

		public TestableDownloadComponent(DownloadRepository repository) {
			super(repository);
			this.repository = repository;
		}

		@Override
		protected DownloadRepository getRepository() {
			return repository;
		}
	}

	@BeforeEach
	void setUp() {
		downloadComponent = new TestableDownloadComponent(mockRepository);
	}

	@Test
	void testGetPageSizeDays_withOneDevice_returns104() {
		assertEquals(104, downloadComponent.getPageSizeDays(1));
	}

	@Test
	void testGetPageSizeDays_with16Devices_returns6() {
		assertEquals(6, downloadComponent.getPageSizeDays(16));
	}

	@Test
	void testGetPageSizeDays_with104Devices_returns1() {
		assertEquals(1, downloadComponent.getPageSizeDays(104));
	}

	@Test
	void testGetPageSizeDays_with105Devices_returns0() {
		assertEquals(0, downloadComponent.getPageSizeDays(105));
	}

	@Test
	void testGetPageSizeDays_withZeroDevices_returns0() {
		assertEquals(0, downloadComponent.getPageSizeDays(0));
	}

	@Test
	void testFindByRequestId_whenRequestExists_returnsDownloadRequest() {
		String requestId = "test-request-123";
		DownloadRequest expectedRequest = new DownloadRequest("customer-123");
		expectedRequest.setRequestId(requestId);
		when(mockRepository.findByRequestId(requestId)).thenReturn(Optional.of(expectedRequest));

		Optional<DownloadRequest> result = downloadComponent.findByRequestId(requestId);

		assertTrue(result.isPresent());
		assertEquals(expectedRequest, result.get());
		verify(mockRepository).findByRequestId(requestId);
	}

	@Test
	void testFindByRequestId_whenRequestDoesNotExist_returnsEmpty() {
		String requestId = "non-existent-request";
		when(mockRepository.findByRequestId(requestId)).thenReturn(Optional.empty());

		Optional<DownloadRequest> result = downloadComponent.findByRequestId(requestId);

		assertFalse(result.isPresent());
		verify(mockRepository).findByRequestId(requestId);
	}

	@Test
	void testAdd_callsRepositoryAdd() {
		DownloadRequest request = new DownloadRequest("customer-123");
		when(mockRepository.add(request)).thenReturn(request);

		DownloadRequest result = downloadComponent.add(request);

		assertEquals(request, result);
		verify(mockRepository).add(request);
	}

	@Test
	void testUpdate_whenRequestExists_returnsUpdatedRequest() {
		DownloadRequest request = new DownloadRequest("customer-123");
		when(mockRepository.update(request)).thenReturn(Optional.of(request));

		Optional<DownloadRequest> result = downloadComponent.update(request);

		assertTrue(result.isPresent());
		assertEquals(request, result.get());
		verify(mockRepository).update(request);
	}

	@Test
	void testUpdate_whenRequestDoesNotExist_returnsEmpty() {
		DownloadRequest request = new DownloadRequest("customer-123");
		when(mockRepository.update(request)).thenReturn(Optional.empty());

		Optional<DownloadRequest> result = downloadComponent.update(request);

		assertFalse(result.isPresent());
		verify(mockRepository).update(request);
	}

	@Test
	void testDelete_callsRepositoryDelete() {
		DownloadRequest request = new DownloadRequest("customer-123");

		downloadComponent.delete(request);

		verify(mockRepository).delete(request);
	}
}

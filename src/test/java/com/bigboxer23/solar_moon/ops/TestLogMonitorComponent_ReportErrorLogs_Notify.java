package com.bigboxer23.solar_moon.ops;

import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.notifications.SupportEmailTemplateContent;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TestLogMonitorComponent_ReportErrorLogs_Notify {

	/** Testable subclass so we can spy on sendSupportEmail(). */
	static class TestableLogMonitorComponent extends LogMonitorComponent {
		@Override
		protected void sendSupportEmail(SupportEmailTemplateContent email) {
			super.sendSupportEmail(email);
		}
	}

	@Test
	public void reportErrorLogs_sendsEmail_whenNotEmpty() {
		TestableLogMonitorComponent component = spy(new TestableLogMonitorComponent());

		LogEntry entry = new LogEntry();
		entry.setDate(new Date());
		entry.setLevel("ERROR");
		entry.setMessage("Boom!");

		doReturn("<html>body</html>").when(component).generateBody(anyList());

		component.reportErrorLogs(List.of(entry));

		ArgumentCaptor<SupportEmailTemplateContent> captor = ArgumentCaptor.forClass(SupportEmailTemplateContent.class);
		verify(component, times(1)).sendSupportEmail(captor.capture());

		SupportEmailTemplateContent emailSent = captor.getValue();
		org.junit.jupiter.api.Assertions.assertEquals("Some Errors have occurred!", emailSent.getTitle());
		org.junit.jupiter.api.Assertions.assertEquals("<html>body</html>", emailSent.getBodyContent1());
	}

	@Test
	public void reportErrorLogs_doesNotSend_whenEmpty() {
		TestableLogMonitorComponent component = spy(new TestableLogMonitorComponent());
		component.reportErrorLogs(List.of());
		verify(component, never()).sendSupportEmail(any());
	}
}

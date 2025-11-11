package com.bigboxer23.solar_moon.ops;

import com.bigboxer23.solar_moon.notifications.SupportEmailTemplateContent;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LogMonitorComponent_ReportErrorLogs_NotifyTest {

	static class TestableLogMonitorComponent extends LogMonitorComponent {
		private SupportEmailTemplateContent capturedEmail;
		private int sendEmailCallCount = 0;

		@Override
		protected void sendSupportEmail(SupportEmailTemplateContent email) {
			this.capturedEmail = email;
			this.sendEmailCallCount++;
		}

		@Override
		protected String generateBody(List<LogEntry> errorLogs) {
			return "<html>body</html>";
		}

		@Override
		protected String getSupportEmail() {
			return "test@example.com";
		}

		public SupportEmailTemplateContent getCapturedEmail() {
			return capturedEmail;
		}

		public int getSendEmailCallCount() {
			return sendEmailCallCount;
		}
	}

	@Test
	public void reportErrorLogs_sendsEmail_whenNotEmpty() {
		TestableLogMonitorComponent component = new TestableLogMonitorComponent();

		LogEntry entry = new LogEntry();
		entry.setDate(new Date());
		entry.setLevel("ERROR");
		entry.setMessage("Boom!");

		component.reportErrorLogs(List.of(entry));

		Assertions.assertEquals(1, component.getSendEmailCallCount());
		SupportEmailTemplateContent emailSent = component.getCapturedEmail();
		Assertions.assertNotNull(emailSent);
		Assertions.assertEquals("Please review these errors.", emailSent.getTitle());
		Assertions.assertEquals("<html>body</html>", emailSent.getBodyContent1());
	}

	@Test
	public void reportErrorLogs_doesNotSend_whenEmpty() {
		TestableLogMonitorComponent component = new TestableLogMonitorComponent();
		component.reportErrorLogs(List.of());
		Assertions.assertEquals(0, component.getSendEmailCallCount());
	}
}

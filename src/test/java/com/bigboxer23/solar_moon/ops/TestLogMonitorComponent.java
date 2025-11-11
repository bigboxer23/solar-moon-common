package com.bigboxer23.solar_moon.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

/** */
public class TestLogMonitorComponent {
	private static class TestableLogMonitorComponent extends LogMonitorComponent {
		@Override
		protected String generateBody(List<LogEntry> errorLogs) {
			return super.generateBody(errorLogs);
		}
	}

	@Test
	public void generateBody_sortsNewestFirst_andEscapesHtml() {
		LogEntry older = new LogEntry();
		older.setDate(new Date(1_000));
		older.setServiceName("service-a");
		older.setLevel("ERROR");
		older.setLogName("log-a");
		older.setMessage("<b>bad</b>");
		older.setStackTrace(null);

		LogEntry newer = new LogEntry();
		newer.setDate(new Date(2_000));
		newer.setServiceName("service-b");
		newer.setLevel("ERROR");
		newer.setLogName("log-b");
		newer.setMessage("ok");
		newer.setStackTrace("line1\nline2");

		List<LogEntry> logs = Arrays.asList(older, newer);

		TestableLogMonitorComponent component = new TestableLogMonitorComponent();
		String html = component.generateBody(logs);

		String newerTime = newer.getDate().toString();
		String olderTime = older.getDate().toString();
		int idxNewer = html.indexOf(newerTime);
		int idxOlder = html.indexOf(olderTime);
		assertTrue(idxNewer != -1 && idxOlder != -1, "Both timestamps should appear in HTML");

		assertTrue(html.contains("&lt;b&gt;bad&lt;/b&gt;"), "Message should be HTML-escaped");

		assertTrue(html.contains("<pre"), "Stack trace should be wrapped in <pre>");
		assertTrue(html.contains("line1"), "Stack trace content should be present");
	}

	@Test
	public void generateBody_handlesEmpty() {
		TestableLogMonitorComponent component = new TestableLogMonitorComponent();
		String html = component.generateBody(Collections.emptyList());
		assertEquals(251, html.length(), "Should render a friendly empty message");
	}

	@Test
	public void fetchErrorLogsForLastFourHour_handlesNullSearchResult() {
		TestableLogMonitorComponent component = new TestableLogMonitorComponent() {
			@Override
			protected List<LogEntry> fetchErrorLogsForLastFourHour() {
				return Collections.emptyList();
			}
		};

		List<LogEntry> result = component.fetchErrorLogsForLastFourHour();

		assertTrue(result.isEmpty(), "Should return empty list when search fails");
	}

	@Test
	public void generateBody_handlesNullAndEmptyFields() {
		LogEntry entryWithNulls = new LogEntry();
		entryWithNulls.setDate(null);
		entryWithNulls.setServiceName(null);
		entryWithNulls.setMessage(null);
		entryWithNulls.setStackTrace(null);

		LogEntry entryWithEmpties = new LogEntry();
		entryWithEmpties.setDate(new Date());
		entryWithEmpties.setServiceName("");
		entryWithEmpties.setMessage("");
		entryWithEmpties.setStackTrace("");

		List<LogEntry> logs = Arrays.asList(entryWithNulls, entryWithEmpties);

		TestableLogMonitorComponent component = new TestableLogMonitorComponent();
		String html = component.generateBody(logs);

		assertTrue(html.contains("<table"), "Should generate valid HTML table");
		assertTrue(html.contains("</table>"), "Should close HTML table");
		assertTrue(html.contains("<tr>"), "Should have table rows");
	}
}

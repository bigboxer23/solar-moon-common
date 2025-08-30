package com.bigboxer23.solar_moon.ops;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.notifications.SupportEmailTemplateContent;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.utils.properties.PropertyUtils;
import java.util.Collections;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.opensearch.client.opensearch.core.search.Hit;

/** */
@Slf4j
public class LogMonitorComponent implements IComponentRegistry {
	public void findAndReportErrorLogging() {
		reportErrorLogs(fetchErrorLogsForLastFourHour());
	}

	protected List<LogEntry> fetchErrorLogsForLastFourHour() {
		SearchJSON search = new SearchJSON();
		search.setEndDate(System.currentTimeMillis());
		search.setStartDate(System.currentTimeMillis() - (4 * TimeConstants.HOUR));
		try {
			var searchResult = OSComponent.searchLogs(search);
			if (searchResult == null
					|| searchResult.hits() == null
					|| searchResult.hits().hits() == null) {
				return Collections.emptyList();
			}
			return searchResult.hits().hits().stream().map(Hit::source).toList();
		} catch (Exception e) {
			log.error("Error fetching error logs", e);
			return Collections.emptyList();
		}
	}

	protected void sendSupportEmail(SupportEmailTemplateContent email) {
		IComponentRegistry.notificationComponent.sendNotification(email.getCustomerName(), email.getTitle(), email);
	}

	protected void reportErrorLogs(List<LogEntry> errorLogs) {
		if (errorLogs.isEmpty()) {
			return;
		}
		SupportEmailTemplateContent email = new SupportEmailTemplateContent(
				"Please review these errors.",
				PropertyUtils.getProperty("emailer.support"),
				generateBody(errorLogs),
				"");
		sendSupportEmail(email); // <— call the seam
	}

	protected String generateBody(List<LogEntry> errorLogs) {
		StringBuilder body = new StringBuilder();
		body.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:"
				+ " collapse; font-family: Arial, sans-serif; font-size: 14px;'>");
		body.append("<tr style='background-color: #f2f2f2;'>")
				.append("<th>Time</th>")
				.append("<th>Service</th>")
				.append("<th>Message</th>")
				.append("<th>Stack Trace</th>")
				.append("</tr>");

		for (LogEntry log : errorLogs) {
			body.append("<tr>")
					.append("<td>")
					.append(StringEscapeUtils.escapeHtml4(
							log.getDate() != null ? log.getDate().toString() : ""))
					.append("</td>")
					.append("<td>")
					.append(StringEscapeUtils.escapeHtml4(log.getServiceName() != null ? log.getServiceName() : ""))
					.append("</td>")
					.append("<td>")
					.append(StringEscapeUtils.escapeHtml4(log.getMessage() != null ? log.getMessage() : ""))
					.append("</td>")
					.append("<td>")
					.append(
							log.getStackTrace() != null
									? "<pre style='white-space: pre-wrap; font-size: 12px;'>"
											+ StringEscapeUtils.escapeHtml4(log.getStackTrace())
											+ "</pre>"
									: "")
					.append("</td>")
					.append("</tr>");
		}

		body.append("</table>");
		return body.toString();
	}
}

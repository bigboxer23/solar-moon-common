package com.bigboxer23.solar_moon.ingest.sma;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.util.XMLUtil;
import java.io.StringReader;
import java.util.*;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.opensearch.client.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class SMAIngestComponent implements ISMAIngestConstants {
	private static final Logger logger = LoggerFactory.getLogger(SMAIngestComponent.class);

	public void ingestXMLFile(String xml, String customerId) throws XPathExpressionException {
		if (StringUtils.isEmpty(xml)) {
			logger.warn("empty xml file, not doing anything");
			return;
		}
		if (StringUtils.isEmpty(customerId)) {
			logger.error("no customer id, not doing anything.");
			return;
		}
		if (StringUtils.isEmpty(xml)) {
			logger.error("no xml body, not doing anything.");
			return;
		}
		NodeList nodelist = (NodeList) XPathFactory.newInstance()
				.newXPath()
				.compile(CURRENT_PUBLIC)
				.evaluate(new InputSource(new StringReader(xml.substring(1))), XPathConstants.NODESET);
		Map<String, SMADevice> devices = new HashMap<>();
		XMLUtil.iterableNodeList(nodelist).forEach(node -> {
			SMARecord record = processChildNode(node);
			devices.computeIfAbsent(record.getDevice(), k -> new SMADevice(customerId))
					.addRecord(record);
		});
		nodelist = (NodeList) XPathFactory.newInstance()
				.newXPath()
				.compile(MEAN_PUBLIC)
				.evaluate(new InputSource(new StringReader(xml.substring(1))), XPathConstants.NODESET);
		XMLUtil.iterableNodeList(nodelist).forEach(node -> {
			SMARecord record = processChildNode(node);
			devices.computeIfAbsent(record.getDevice(), k -> new SMADevice(customerId))
					.addRecord(record);
		});
		devices.forEach((key, smaDevice) -> {
			try {
				IComponentRegistry.generationComponent.handleDevice(
						smaDevice.getDevice(), translateToDeviceData(smaDevice));
			} catch (ResponseException e) {
				logger.error("ingestXMLFile", e);
			}
		});
	}

	private DeviceData translateToDeviceData(SMADevice smaDevice) {
		DeviceData data = new DeviceData(
				smaDevice.getDevice().getSiteId(),
				smaDevice.getCustomerId(),
				smaDevice.getDevice().getId());
		data.setDate(smaDevice.getTimestamp());
		data.setPowerFactor(1);
		data.setAverageCurrent(0);
		smaDevice.getRecords().forEach(record -> {
			switch (record.getAttributeName()) {
				case TOTAL_YIELD:
					data.setTotalEnergyConsumed(Float.parseFloat(record.getValue()));
					return;
				case POWER:
					data.setTotalRealPower(Float.parseFloat(record.getValue()) / 1000);
					return;
				case PHASE_VOLTAGE:
					data.setAverageVoltage(Float.parseFloat(record.getValue()));
			}
		});
		return data;
	}

	private SMARecord processChildNode(Node node) {
		SMARecord record = new SMARecord();
		XMLUtil.iterableNodeList(node.getChildNodes()).forEach(childNode -> {
			switch (childNode.getNodeName()) {
				case KEY:
					String content = childNode.getTextContent();
					record.setDevice(content.substring(0, content.lastIndexOf(":")));
					record.setAttributeName(content.substring(content.lastIndexOf(":") + 1));
					break;
				case TIMESTAMP:
					record.setTimestamp(childNode.getTextContent());
					break;
				case MEAN:
					record.setValue(childNode.getTextContent());
					break;
			}
		});
		return record;
	}
}

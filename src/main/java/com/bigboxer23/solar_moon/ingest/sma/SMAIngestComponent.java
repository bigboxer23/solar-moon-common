package com.bigboxer23.solar_moon.ingest.sma;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import com.bigboxer23.solar_moon.util.XMLUtil;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import com.bigboxer23.utils.properties.PropertyUtils;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.ResponseException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.StringUtils;

/** */
@Slf4j
public class SMAIngestComponent implements ISMAIngestConstants {
	private final SimpleDateFormat smaFtpFolderDateFormatter = new SimpleDateFormat("yyyyMMdd");

	private static S3Client s3;

	private final String bucket = PropertyUtils.getProperty("ftp.s3.bucket");

	public S3Client getS3Client() {
		if (s3 == null) {
			s3 = S3Client.builder()
					.region(Region.of(PropertyUtils.getProperty("aws.region")))
					.build();
		}
		return s3;
	}

	public void ingestXMLFile(String xml, String customerId) throws XPathExpressionException {
		if (StringUtils.isEmpty(xml)) {
			log.warn("empty xml file, not doing anything");
			return;
		}
		if (StringUtils.isEmpty(customerId)) {
			log.error("no customer id, not doing anything.");
			return;
		}
		if (StringUtils.isEmpty(xml)) {
			log.error("no xml body, not doing anything.");
			return;
		}
		NodeList nodelist = (NodeList) XPathFactory.newInstance()
				.newXPath()
				.compile(CURRENT_PUBLIC)
				.evaluate(new InputSource(new StringReader(xml)), XPathConstants.NODESET);
		Map<String, SMADevice> devices = new HashMap<>();
		XMLUtil.iterableNodeList(nodelist).forEach(node -> {
			SMARecord record = processChildNode(node);
			devices.computeIfAbsent(record.getDevice(), k -> new SMADevice(customerId))
					.addRecord(record);
		});
		nodelist = (NodeList) XPathFactory.newInstance()
				.newXPath()
				.compile(MEAN_PUBLIC)
				.evaluate(new InputSource(new StringReader(xml)), XPathConstants.NODESET);
		XMLUtil.iterableNodeList(nodelist).forEach(node -> {
			SMARecord record = processChildNode(node);
			devices.computeIfAbsent(record.getDevice(), k -> new SMADevice(customerId))
					.addRecord(record);
		});
		maybeAssignSite(customerId, devices);
		addMissingDevices(customerId, devices);
		checkForNewDevicesAndAssignSite(devices);
		devices.forEach((key, smaDevice) -> {
			if (smaDevice.getDevice() == null) {
				TransactionUtil.addDeviceId(null, null);
				log.warn("No device for " + smaDevice.getDeviceName() + ", not doing anything.");
				return;
			}
			try {
				TransactionUtil.addDeviceId(
						smaDevice.getDevice().getId(), smaDevice.getDevice().getSiteId());
				DeviceData data = IComponentRegistry.generationComponent.handleDevice(
						smaDevice.getDevice(), translateToDeviceData(smaDevice));
				log.info("successfully uploaded data: " + data.getDate());
			} catch (ResponseException e) {
				log.error("ingestXMLFile", e);
			}
		});
	}

	private void maybeAssignSite(String customerId, Map<String, SMADevice> devices) {
		boolean shouldChangeSite = devices.values().stream()
				.allMatch(d -> DeviceComponent.NO_SITE.equals(d.getDevice().getSiteId()));
		if (shouldChangeSite) {
			log.info("all new items, assigning site");
			Double[] yield = {(double) -1};
			String[] siteNameHolder = new String[1];
			devices.values().forEach(sma -> {
				sma.getRecords().stream()
						.filter(r -> TOTAL_YIELD.equalsIgnoreCase(r.getAttributeName()))
						.findFirst()
						.ifPresent(r -> {
							try {
								double localYield = Double.parseDouble(r.getValue());
								if (localYield > yield[0]) {
									yield[0] = localYield;
									siteNameHolder[0] = r.getDevice();
								}
							} catch (NumberFormatException nfe) {
							}
						});
			});
			lookupSiteDeviceAndAssignSiteToDeviceList(customerId, siteNameHolder[0], devices.values());
		}
	}

	private void lookupSiteDeviceAndAssignSiteToDeviceList(
			String customerId, String siteDeviceName, Collection<SMADevice> devices) {
		log.warn("Attempting to update site for " + siteDeviceName);
		Optional<Device> siteOptional =
				IComponentRegistry.deviceComponent.findDeviceByDeviceName(customerId, siteDeviceName);
		if (siteOptional.isEmpty()) {
			log.warn("Cannot find device for " + siteDeviceName + ". Cannot automatically stamp site");
			return;
		}
		Device site = siteOptional.get();
		site.setVirtual(false);
		site.setIsSite("1");
		site.setSiteId(siteOptional.get().getId());
		site.setSite(siteOptional.get().getDisplayName());
		IComponentRegistry.deviceComponent.updateDevice(site).ifPresent(s -> devices.stream()
				.map(SMADevice::getDevice)
				.filter(Objects::nonNull)
				.filter(device -> !site.getId().equals(device.getId()))
				.filter(device -> DeviceComponent.NO_SITE.equalsIgnoreCase(device.getSiteId()))
				.forEach(device -> {
					TransactionUtil.addDeviceId(device.getId(), site.getId());
					log.warn("adjusting site for " + device.getDisplayName() + " " + site.getSite());
					device.setSite(site.getDisplayName());
					device.setSiteId(site.getId());
					IComponentRegistry.deviceComponent.updateDevice(device);
				}));
	}

	private void checkForNewDevicesAndAssignSite(Map<String, SMADevice> devices) {
		log.debug("checking for new devices");
		Device donor = devices.values().stream()
				.map(SMADevice::getDevice)
				.filter(Objects::nonNull)
				.filter(device -> !DeviceComponent.NO_SITE.equalsIgnoreCase(device.getSiteId()))
				.findFirst()
				.orElse(null);
		if (donor == null) {
			log.error("cannot determine site.");
			return;
		}
		devices.values().stream()
				.map(SMADevice::getDevice)
				.filter(Objects::nonNull)
				.filter(device -> device.getSite().equalsIgnoreCase(DeviceComponent.NO_SITE))
				.forEach(device -> {
					TransactionUtil.addDeviceId(device.getId(), donor.getSiteId());
					log.warn("found unassigned device within site, assigning to site " + donor.getSite());
					device.setSite(donor.getSite());
					device.setSiteId(donor.getSiteId());
					IComponentRegistry.deviceComponent.updateDevice(device);
				});
		TransactionUtil.addDeviceId(null, null);
	}

	/**
	 * Devices disappear at night sometimes, add them back in at 0 production
	 *
	 * @param customerId
	 * @param devices
	 */
	private void addMissingDevices(String customerId, Map<String, SMADevice> devices) {
		if (devices.isEmpty()) {
			return;
		}
		devices.keySet().stream().findFirst().ifPresent(key -> {
			SMADevice donor = devices.get(key);
			if (DeviceComponent.NO_SITE.equals(donor.getDevice().getSiteId())) {
				log.debug("cannot find ghost devices w/o site configuration");
				return;
			}
			boolean isDay = IComponentRegistry.locationComponent
					.isDay(
							new Date(),
							donor.getDevice().getLatitude(),
							donor.getDevice().getLongitude())
					.orElse(true);
			IComponentRegistry.deviceComponent
					.getDevicesBySiteId(customerId, donor.getDevice().getSiteId())
					.stream()
					.filter(d -> !d.isDisabled())
					.forEach(d -> {
						if (d.getDeviceName() != null && !devices.containsKey(d.getDeviceName())) {
							TransactionUtil.addDeviceId(d.getId(), d.getSiteId());
							if (isDay) {
								log.info("adding ghost device " + d.getDeviceName());
							} else {
								log.debug("adding ghost device " + d.getDeviceName());
							}
							SMADevice smaDevice = new SMADevice(customerId);
							SMARecord record = new SMARecord(donor.getRecords().getFirst());
							record.setDevice(d.getDeviceName());
							record.setValue("0");
							smaDevice.addRecord(record);
							devices.put(d.getDeviceName(), smaDevice);
						}
					});
		});
		TransactionUtil.addDeviceId(null, null);
	}

	private DeviceData translateToDeviceData(SMADevice smaDevice) {
		DeviceData data = DeviceData.createEmpty(
				smaDevice.getDevice().getSiteId(),
				smaDevice.getCustomerId(),
				smaDevice.getDevice().getId(),
				smaDevice.getTimestamp());
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
		if (data.getTotalEnergyConsumed() == 0) {
			data.setTotalEnergyConsumed(IComponentRegistry.OSComponent.getMaxTotalEnergyConsumed(
					smaDevice.getCustomerId(), smaDevice.getDevice().getId(), 6 * TimeConstants.HOUR));
		}
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

	/**
	 * set time zone in date formatter by looking up the site's location. If no site attached, fall
	 * back to the customer's default timezone
	 *
	 * @param device
	 * @return
	 */
	public static SimpleDateFormat getDateFormatter(Device device) {
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		TimeUtils.getTimeZone(device).ifPresent(tz -> sdf.setTimeZone(TimeZone.getTimeZone(tz)));
		return sdf;
	}

	public void handleAccessKeyChange(String oldAccessKey, String newAccessKey) {
		if (!StringUtils.isEmpty(oldAccessKey)) {
			// TODO: maybe remove this later? Might be useful to keep here for un-updated sma device
			// TODO: pushing
		}
		if (!StringUtils.isEmpty(newAccessKey)) {
			log.warn("Creating new folder for access key " + newAccessKey);
			PutObjectResponse response = getS3Client()
					.putObject(
							PutObjectRequest.builder()
									.bucket(bucket)
									.key(newAccessKey + "/")
									.build(),
							RequestBody.empty());
			log.info("Created folder: " + response.toString());
		} else {
			// Account deletion
			log.warn("Deleting folder for " + oldAccessKey);
			DeleteObjectResponse response = getS3Client()
					.deleteObject(DeleteObjectRequest.builder()
							.bucket(bucket)
							.key(oldAccessKey + "/")
							.build());
			log.info("Deleted folder: " + response.toString());
		}
	}

	public Optional<Date> getDateFromSMAS3Path(String path) {
		if (StringUtils.isEmpty(path)) {
			return Optional.empty();
		}
		int index = path.lastIndexOf("/");
		if (index > -1 && !path.endsWith("/")) {
			path = path.substring(0, index);
		}
		String[] paths = path.split("/");
		try {
			return Optional.of(smaFtpFolderDateFormatter.parse(paths[paths.length - 1]));
		} catch (ParseException e) {
			log.info("cannot parse: " + path, e);
		}
		return Optional.empty();
	}
}

package com.bigboxer23.solar_moon;

/** */
public interface TestConstants {

	double testLatitude = 44.986;
	double testLongitude = -93.37777;

	String deviceName = "testDevice";

	String CUSTOMER_ID = "0badd0c2-450b-4204-80d5-c7c77fc13500";

	String CUSTOMER_EMAIL = "noreply@solarmoonanalytics.com";
	String CUSTOMER_NAME = "mr fake customer";
	String CUSTOMER_ACCESS_KEY = "4ab84ed3-0ce1-4615-b919-c34c7b619702";

	String CUSTOMER_STRIPE_ID = "ST_id_1234";

	String SITE = "testSite";

	String date = "2020-08-21 17:30:00";

	String device1Name = "Test generator 1";

	String serialNumber = "xx1xx6xxxxxx";

	String LINKED_DEVICE_XML =
			"""
			<?xml version="1.0" encoding="UTF-8" ?>
			<DAS>
			<mode>LOGFILEUPLOAD</mode>
			<name>"""
					+ serialNumber
					+ """
			A1</name>
			<serial>"""
					+ serialNumber
					+ """
			</serial>
			<devices>
			<device>
			<name>"""
					+ device1Name
					+ """
			</name>
			<address>1</address>
			<type>xxxxxx</type>
			<class>4000</class>
			<numpoints>17</numpoints>
			<records>
			<record>
			<time zone="UTC">"""
					+ date
					+ """
			</time>
			<error text="Ok">0</error>
			<point number="0" name="DC Voltage" units="V" value="367.970" />
			<point number="1" name="Real AC Power" units="W" value="51113.281" />
			<point number="2" name="AC Grid Frequency" units="Hz" value="59.961" />
			<point number="3" name="AC Power Stage Current" units="A" value="154.741" />
			<point number="4" name="L1-to-L2 AC Voltage" units="V" value="500.978" />
			<point number="5" name="L2-to-L3 AC Voltage" units="V" value="502.736" />
			<point number="6" name="L1-to-L3 AC Voltage" units="V" value="502.150" />
			<point number="7" name="Phase Sequence" units="" value="1.000" />
			<point number="8" name="Ac Energy/MSW" units="KWh" value="227.000" />
			<point number="9" name="AC Energy/LSW" units="" value="5433.500" />
			<point number="10" name="On-grid Hours MSW" units="Hrs" value="0" />
			<point number="11" name="On-Grid Hours LSW" units="" value="47366.000" />
			<point number="12" name="Fan on-time Hours" units="Hrs" value="34822.000" />
			<point number="13" name="AC Contactors Cycles" units="" value="24311.000" />
			<point number="14" name="Slave ID" units="" value="1.000" />
			<point number="15" name="Critical Alarms" units="" value="0" />
			<point number="16" name="Informative Alarms" units="" value="0" />
			</record>
			</records>
			</device>
			</devices>
			</DAS>""";

	String device1Xml = "<DAS>\n"
			+ "<mode>LOGFILEUPLOAD</mode>"
			+ "<name>"
			+ serialNumber
			+ " A1</name>\n"
			+ "<serial>"
			+ serialNumber
			+ "</serial>\n"
			+ "<devices>\n"
			+ "<device>\n"
			+ "<name>"
			+ device1Name
			+ "</name>\n"
			+ "<address>12</address>\n"
			+ "<type>Veris Hxxxx-xxxx-x, Full-Data, Modbus, xxx Amp</type>\n"
			+ "<class>2</class>\n"
			+ "<status>Ok</status>\n"
			+ "<numpoints>26</numpoints>\n"
			+ "<records>\n"
			+ "<record>\n"
			+ "<time zone=\"UTC\">"
			+ date
			+ "</time>\n"
			+ "<age units=\"seconds\">1</age>\n"
			+ "<error text=\"Ok\">0</error>\n"
			+ "<point number=\"0\" name=\"Energy Consumption\" units=\"kWh\""
			+ " value=\"7675835.50\"/>\n"
			+ "<point number=\"1\" name=\"Real Power\" units=\"kW\" value=\"6.219\"/>\n"
			+ "<point number=\"2\" name=\"Reactive Power\" units=\"kVAR\""
			+ " value=\"8.781\"/>\n"
			+ "<point number=\"3\" name=\"Apparent Power\" units=\"kVA\""
			+ " value=\"10.750\"/>\n"
			+ "<point number=\"4\" name=\"Power Factor\" units=\"\" value=\"-100\"/>\n"
			+ "<point number=\"5\" name=\"Voltage, Line to Line\" units=\"Volts\""
			+ " value=\"486.2\"/>\n"
			+ "<point number=\"6\" name=\"Voltage, Line to Neutral\" units=\"Volts\""
			+ " value=\"281.7\"/>\n"
			+ "<point number=\"7\" name=\"Current\" units=\"Amps\" value=\"12.7\"/>\n"
			+ "<point number=\"8\" name=\"Real Power phase A\" units=\"kW\""
			+ " value=\"1.891\"/>\n"
			+ "<point number=\"9\" name=\"Real Power phase B\" units=\"kW\""
			+ " value=\"2.258\"/>\n"
			+ "<point number=\"10\" name=\"Real Power phase C\" units=\"kW\""
			+ " value=\"2.055\"/>\n"
			+ "<point number=\"11\" name=\"Power Factor phase A\" units=\"\""
			+ " value=\"0.5278\"/>\n"
			+ "<point number=\"12\" name=\"Power Factor phase B\" units=\"\""
			+ " value=\"0.6138\"/>\n"
			+ "<point number=\"13\" name=\"Power Factor phase C\" units=\"\""
			+ " value=\"0.5971\"/>\n"
			+ "<point number=\"14\" name=\"Voltage phase A-B\" units=\"Volts\""
			+ " value=\"486.3\"/>\n"
			+ "<point number=\"15\" name=\"Voltage phase B-C\" units=\"Volts\""
			+ " value=\"485.8\"/>\n"
			+ "<point number=\"16\" name=\"Voltage phase C-A\" units=\"Volts\""
			+ " value=\"486.3\"/>\n"
			+ "<point number=\"17\" name=\"Voltage phase A-N\" units=\"Volts\""
			+ " value=\"282.2\"/>\n"
			+ "<point number=\"18\" name=\"Voltage phase B-N\" units=\"Volts\""
			+ " value=\"281.5\"/>\n"
			+ "<point number=\"19\" name=\"Voltage phase C-N\" units=\"Volts\""
			+ " value=\"281.3\"/>\n"
			+ "<point number=\"20\" name=\"Current phase A\" units=\"Amps\""
			+ " value=\"12.6\"/>\n"
			+ "<point number=\"21\" name=\"Current phase B\" units=\"Amps\""
			+ " value=\"13.1\"/>\n"
			+ "<point number=\"22\" name=\"Current phase C\" units=\"Amps\""
			+ " value=\"12.3\"/>\n"
			+ "<point number=\"23\" name=\"Average Demand\" units=\"kW\""
			+ " value=\"6.563\"/>\n"
			+ "<point number=\"24\" name=\"Minimum Demand\" units=\"kW\""
			+ " value=\"5.844\"/>\n"
			+ "<point number=\"25\" name=\"Maximum Demand\" units=\"kW\""
			+ " value=\"7.938\"/>\n"
			+ "</record>\n"
			+ "</records>\n"
			+ "</device>\n"
			+ "</devices>\n"
			+ "</DAS>";

	String deviceError =
			"""
			<?xml version="1.0" encoding="UTF-8" ?>
			<DAS>
			<mode>LOGFILEUPLOAD</mode>
			<name>001EC6000D80 A1</name>
			<serial>001EC6000D80</serial>
			<devices>
			<device>
			<name>Generation Meter A1</name>
			<address>14</address>
			<type>Elkor Watts On</type>
			<class>50</class>
			<numpoints>59</numpoints>
			<records>
			<record>
			<time zone="UTC">2020-08-21 17:30:00</time>
			<error text="Device Failed to Respond (the modbus device may be off or disconnected)">139</error>
			<point number="0" name="Total Energy Consumption" units="kWh" value="NULL" />
			</record>
			</records>
			</device>
			</devices>
			</DAS>""";
	String device2Xml = "<DAS>\n"
			+ "<mode>LOGFILEUPLOAD</mode>\n"
			+ "<name>xxxxC600xxxx</name>\n"
			+ "<serial>xxxxC600xxxx</serial>\n"
			+ "<devices>\n"
			+ "<device>\n"
			+ "<name>"
			+ deviceName
			+ "</name>\n"
			+ "<address>100</address>\n"
			+ "<type>Power Measurement ION xxxx</type>\n"
			+ "<class>12</class>\n"
			+ "<status>Ok</status>\n"
			+ "<numpoints>64</numpoints>\n"
			+ "<records>\n"
			+ "<record>\n"
			+ "<time zone=\"UTC\">"
			+ date
			+ "</time>\n"
			+ "<age units=\"seconds\">503</age>\n"
			+ "<error text=\"Ok\">0</error>\n"
			+ "<point number=\"10\" name=\"Total Real Power\" units=\"kW\""
			+ " value=\"2.134055\"/>\n"
			+ "<point number=\"0\" name=\"Vln a\" units=\"Volts\" value=\"2476.4\"/>\n"
			+ "<point number=\"1\" name=\"Vln b\" units=\"Volts\" value=\"2466.5\"/>\n"
			+ "<point number=\"2\" name=\"Vln c\" units=\"Volts\" value=\"2475.8\"/>\n"
			+ "<point number=\"3\" name=\"Vln ave\" units=\"Volts\" value=\"2472.9\"/>\n"
			+ "<point number=\"4\" name=\"Vll ab\" units=\"Volts\" value=\"4285.5\"/>\n"
			+ "<point number=\"5\" name=\"Vll bc\" units=\"Volts\" value=\"4280.8\"/>\n"
			+ "<point number=\"6\" name=\"Vll ca\" units=\"Volts\" value=\"4283.1\"/>\n"
			+ "<point number=\"7\" name=\"Vll ave\" units=\"Volts\" value=\"4283.1\"/>\n"
			+ "<point number=\"8\" name=\"I a\" units=\"Amps\" value=\"57.2345\"/>\n"
			+ "<point number=\"9\" name=\"I b\" units=\"Amps\" value=\"57.0\"/>\n"
			+ "<point number=\"10\" name=\"I c\" units=\"Amps\" value=\"57.1\"/>\n"
			+ "<point number=\"11\" name=\"I ave\" units=\"Amps\" value=\"57.1\"/>\n"
			+ "<point number=\"12\" name=\"V unbal\" units=\"%\" value=\"0.3\"/>\n"
			+ "<point number=\"13\" name=\"I unbal\" units=\"%\" value=\"0.1\"/>\n"
			+ "<point number=\"14\" name=\"Freq\" units=\"Hz\" value=\"60.0\"/>\n"
			+ "<point number=\"15\" name=\"I4\" units=\"Amps\" value=\"0\"/>\n"
			+ "<point number=\"16\" name=\"kW a\" units=\"kW\" value=\"-0.1\"/>\n"
			+ "<point number=\"17\" name=\"kW b\" units=\"kW\" value=\"-0.1\"/>\n"
			+ "<point number=\"18\" name=\"kW c\" units=\"kW\" value=\"-0.1\"/>\n"
			+ "<point number=\"19\" name=\"kW tot\" units=\"kW\" value=\"-0.4\"/>\n"
			+ "<point number=\"20\" name=\"kVAR a\" units=\"kVAR\" value=\"0\"/>\n"
			+ "<point number=\"21\" name=\"kVAR b\" units=\"kVAR\" value=\"0\"/>\n"
			+ "<point number=\"22\" name=\"kVAR c\" units=\"kVAR\" value=\"0\"/>\n"
			+ "<point number=\"23\" name=\"kVAR tot\" units=\"kVAR\" value=\"0\"/>\n"
			+ "<point number=\"24\" name=\"kVA a\" units=\"kVA\" value=\"0.1\"/>\n"
			+ "<point number=\"25\" name=\"kVA b\" units=\"kVA\" value=\"0.1\"/>\n"
			+ "<point number=\"26\" name=\"kVA c\" units=\"kVA\" value=\"0.1\"/>\n"
			+ "<point number=\"27\" name=\"kVA tot\" units=\"kVA\" value=\"0.4\"/>\n"
			+ "<point number=\"28\" name=\"PF sign a\" units=\"\" value=\"99.9\"/>\n"
			+ "<point number=\"29\" name=\"PF sign b\" units=\"\" value=\"99.9\"/>\n"
			+ "<point number=\"30\" name=\"PF sign c\" units=\"\" value=\"99.9\"/>\n"
			+ "<point number=\"31\" name=\"PF sign tot\" units=\"\" value=\"89.123\"/>\n"
			+ "<point number=\"32\" name=\"Vll ave mx\" units=\"Volts\""
			+ " value=\"5401.1\"/>\n"
			+ "<point number=\"33\" name=\"I ave mx\" units=\"Amps\" value=\"106.9\"/>\n"
			+ "<point number=\"34\" name=\"kW tot mx\" units=\"kW\" value=\"0.7\"/>\n"
			+ "<point number=\"35\" name=\"kVAR tot mx\" units=\"kVAR\" value=\"0.7\"/>\n"
			+ "<point number=\"36\" name=\"kVA tot mx\" units=\"kVA\" value=\"0.9\"/>\n"
			+ "<point number=\"37\" name=\"Freq mx\" units=\"Hz\" value=\"63.7\"/>\n"
			+ "<point number=\"38\" name=\"Vll ave mn\" units=\"Volts\" value=\"0\"/>\n"
			+ "<point number=\"39\" name=\"I ave mn\" units=\"Amps\" value=\"0\"/>\n"
			+ "<point number=\"40\" name=\"Freq mn\" units=\"Hz\" value=\"53.9\"/>\n"
			+ "<point number=\"41\" name=\"kW sd del-rec\" units=\"kW\""
			+ " value=\"-271.8\"/>\n"
			+ "<point number=\"42\" name=\"kVA sd del+rec\" units=\"kVA\""
			+ " value=\"272.0\"/>\n"
			+ "<point number=\"43\" name=\"kVAR sd del-rec\" units=\"kVAR\""
			+ " value=\"10.7\"/>\n"
			+ "<point number=\"44\" name=\"kW sd mx d-r\" units=\"kW\" value=\"3.3\"/>\n"
			+ "<point number=\"45\" name=\"kVA sd mx d+r\" units=\"kVA\""
			+ " value=\"507.6\"/>\n"
			+ "<point number=\"46\" name=\"kVAR sd mx d-r\" units=\"kVAR\""
			+ " value=\"77.2\"/>\n"
			+ "<point number=\"47\" name=\"Phase Rev\" units=\"\" value=\"0\"/>\n"
			+ "<point number=\"48\" name=\"kWh del\" units=\"kWh\" value=\"100028\"/>\n"
			+ "<point number=\"49\" name=\"kWh rec\" units=\"kWh\" value=\"9603318\"/>\n"
			+ "<point number=\"50\" name=\"kWh del+rec\" units=\"kWh\""
			+ " value=\"9703343\"/>\n"
			+ "<point number=\"51\" name=\"kWh del-rec\" units=\"kWh\""
			+ " value=\"-9503290\"/>\n"
			+ "<point number=\"52\" name=\"kVARh del\" units=\"kVARh\" value=\"251883\"/>\n"
			+ "<point number=\"53\" name=\"kVARh rec\" units=\"kVARh\" value=\"81704\"/>\n"
			+ "<point number=\"54\" name=\"kVARh del+rec\" units=\"kVARh\""
			+ " value=\"333587\"/>\n"
			+ "<point number=\"55\" name=\"kVARh del-rec\" units=\"kVARh\""
			+ " value=\"170180\"/>\n"
			+ "<point number=\"56\" name=\"kVAh del+rec\" units=\"kVAh\""
			+ " value=\"9742942\"/>\n"
			+ "<point number=\"57\" name=\"V1 THD mx\" units=\"%\" value=\"245\"/>\n"
			+ "<point number=\"58\" name=\"V2 THD mx\" units=\"%\" value=\"235\"/>\n"
			+ "<point number=\"59\" name=\"V3 THD mx\" units=\"%\" value=\"242\"/>\n"
			+ "<point number=\"60\" name=\"I1 THD mx\" units=\"%\" value=\"4074\"/>\n"
			+ "<point number=\"61\" name=\"I2 THD mx\" units=\"%\" value=\"4076\"/>\n"
			+ "<point number=\"62\" name=\"I3 THD mx\" units=\"%\" value=\"4509\"/>\n"
			+ "<point number=\"63\" name=\"(unused)\" units=\"\" value=\"-10001\"/>\n"
			+ "</record>\n"
			+ "</records>\n"
			+ "</device>\n"
			+ "</devices>\n"
			+ "</DAS>";

	String nonUpdateStatus =
			"""
			<?xml version="1.0" encoding="UTF-8" ?>
			<DAS>
			<mode>STATUS</mode>
			<name>001EC60007B2</name>
			</DAS>""";

	String device2XmlNull = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
			+ "<DAS>\n"
			+ "<mode>LOGFILEUPLOAD</mode>\n"
			+ "<name>xxxxC600xxxx</name>\n"
			+ "<serial>xxxxC600xxxx</serial>\n"
			+ "<devices>\n"
			+ "<device>\n"
			+ "<name>"
			+ deviceName
			+ "</name>\n"
			+ "<address>100</address>\n"
			+ "<type>Power Measurement ION 8600</type>\n"
			+ "<class>12</class>\n"
			+ "<numpoints>64</numpoints>\n"
			+ "<records>\n"
			+ "<record>\n"
			+ "<time zone=\"UTC\">"
			+ date
			+ "</time>\n"
			+ "<error text=\"Start log\">160</error>\n"
			+ "<point number=\"0\" name=\"Vln a\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"1\" name=\"Vln b\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"2\" name=\"Vln c\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"3\" name=\"Vln ave\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"4\" name=\"Vll ab\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"5\" name=\"Vll bc\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"6\" name=\"Vll ca\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"7\" name=\"Vll ave\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"8\" name=\"I a\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"9\" name=\"I b\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"10\" name=\"I c\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"11\" name=\"I ave\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"12\" name=\"V unbal\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"13\" name=\"I unbal\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"14\" name=\"Freq\" units=\"Hz\" value=\"NULL\" />\n"
			+ "<point number=\"15\" name=\"I4\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"16\" name=\"kW a\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"17\" name=\"kW b\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"18\" name=\"kW c\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"19\" name=\"kW tot\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"20\" name=\"kVAR a\" units=\"kVAR\" value=\"NULL\" />\n"
			+ "<point number=\"21\" name=\"kVAR b\" units=\"kVAR\" value=\"NULL\" />\n"
			+ "<point number=\"22\" name=\"kVAR c\" units=\"kVAR\" value=\"NULL\" />\n"
			+ "<point number=\"23\" name=\"kVAR tot\" units=\"kVAR\" value=\"NULL\" />\n"
			+ "<point number=\"24\" name=\"kVA a\" units=\"kVA\" value=\"NULL\" />\n"
			+ "<point number=\"25\" name=\"kVA b\" units=\"kVA\" value=\"NULL\" />\n"
			+ "<point number=\"26\" name=\"kVA c\" units=\"kVA\" value=\"NULL\" />\n"
			+ "<point number=\"27\" name=\"kVA tot\" units=\"kVA\" value=\"NULL\" />\n"
			+ "<point number=\"28\" name=\"PF sign a\" units=\"\" value=\"NULL\" />\n"
			+ "<point number=\"29\" name=\"PF sign b\" units=\"\" value=\"NULL\" />\n"
			+ "<point number=\"30\" name=\"PF sign c\" units=\"\" value=\"NULL\" />\n"
			+ "<point number=\"31\" name=\"PF sign tot\" units=\"\" value=\"NULL\" />\n"
			+ "<point number=\"32\" name=\"Vll ave mx\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"33\" name=\"I ave mx\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"34\" name=\"kW tot mx\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"35\" name=\"kVAR tot mx\" units=\"kVAR\" value=\"NULL\" />\n"
			+ "<point number=\"36\" name=\"kVA tot mx\" units=\"kVA\" value=\"NULL\" />\n"
			+ "<point number=\"37\" name=\"Freq mx\" units=\"Hz\" value=\"NULL\" />\n"
			+ "<point number=\"38\" name=\"Vll ave mn\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"39\" name=\"I ave mn\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"40\" name=\"Freq mn\" units=\"Hz\" value=\"NULL\" />\n"
			+ "<point number=\"41\" name=\"kW sd del-rec\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"42\" name=\"kVA sd del+rec\" units=\"kVA\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"43\" name=\"kVAR sd del-rec\" units=\"kVAR\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"44\" name=\"kW sd mx d-r\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"45\" name=\"kVA sd mx d+r\" units=\"kVA\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"46\" name=\"kVAR sd mx d-r\" units=\"kVAR\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"47\" name=\"Phase Rev\" units=\"\" value=\"NULL\" />\n"
			+ "<point number=\"48\" name=\"kWh del\" units=\"kWh\" value=\"NULL\" />\n"
			+ "<point number=\"49\" name=\"kWh rec\" units=\"kWh\" value=\"NULL\" />\n"
			+ "<point number=\"50\" name=\"kWh del+rec\" units=\"kWh\" value=\"NULL\" />\n"
			+ "<point number=\"51\" name=\"kWh del-rec\" units=\"kWh\" value=\"NULL\" />\n"
			+ "<point number=\"52\" name=\"kVARh del\" units=\"kVARh\" value=\"NULL\" />\n"
			+ "<point number=\"53\" name=\"kVARh rec\" units=\"kVARh\" value=\"NULL\" />\n"
			+ "<point number=\"54\" name=\"kVARh del+rec\" units=\"kVARh\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"55\" name=\"kVARh del-rec\" units=\"kVARh\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"56\" name=\"kVAh del+rec\" units=\"kVAh\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"57\" name=\"V1 THD mx\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"58\" name=\"V2 THD mx\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"59\" name=\"V3 THD mx\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"60\" name=\"I1 THD mx\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"61\" name=\"I2 THD mx\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"62\" name=\"I3 THD mx\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"63\" name=\"(unused)\" units=\"\" value=\"NULL\" />\n"
			+ "</record>\n"
			+ "<record>\n"
			+ "<time zone=\"UTC\">2023-08-23 18:22:30</time>\n"
			+ "<error text=\"No route to host\">113</error>\n"
			+ "<point number=\"0\" name=\"Vln a\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"1\" name=\"Vln b\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"2\" name=\"Vln c\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"3\" name=\"Vln ave\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"4\" name=\"Vll ab\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"5\" name=\"Vll bc\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"6\" name=\"Vll ca\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"7\" name=\"Vll ave\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"8\" name=\"I a\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"9\" name=\"I b\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"10\" name=\"I c\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"11\" name=\"I ave\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"12\" name=\"V unbal\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"13\" name=\"I unbal\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"14\" name=\"Freq\" units=\"Hz\" value=\"NULL\" />\n"
			+ "<point number=\"15\" name=\"I4\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"16\" name=\"kW a\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"17\" name=\"kW b\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"18\" name=\"kW c\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"19\" name=\"kW tot\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"20\" name=\"kVAR a\" units=\"kVAR\" value=\"NULL\" />\n"
			+ "<point number=\"21\" name=\"kVAR b\" units=\"kVAR\" value=\"NULL\" />\n"
			+ "<point number=\"22\" name=\"kVAR c\" units=\"kVAR\" value=\"NULL\" />\n"
			+ "<point number=\"23\" name=\"kVAR tot\" units=\"kVAR\" value=\"NULL\" />\n"
			+ "<point number=\"24\" name=\"kVA a\" units=\"kVA\" value=\"NULL\" />\n"
			+ "<point number=\"25\" name=\"kVA b\" units=\"kVA\" value=\"NULL\" />\n"
			+ "<point number=\"26\" name=\"kVA c\" units=\"kVA\" value=\"NULL\" />\n"
			+ "<point number=\"27\" name=\"kVA tot\" units=\"kVA\" value=\"NULL\" />\n"
			+ "<point number=\"28\" name=\"PF sign a\" units=\"\" value=\"NULL\" />\n"
			+ "<point number=\"29\" name=\"PF sign b\" units=\"\" value=\"NULL\" />\n"
			+ "<point number=\"30\" name=\"PF sign c\" units=\"\" value=\"NULL\" />\n"
			+ "<point number=\"31\" name=\"PF sign tot\" units=\"\" value=\"NULL\" />\n"
			+ "<point number=\"32\" name=\"Vll ave mx\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"33\" name=\"I ave mx\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"34\" name=\"kW tot mx\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"35\" name=\"kVAR tot mx\" units=\"kVAR\" value=\"NULL\" />\n"
			+ "<point number=\"36\" name=\"kVA tot mx\" units=\"kVA\" value=\"NULL\" />\n"
			+ "<point number=\"37\" name=\"Freq mx\" units=\"Hz\" value=\"NULL\" />\n"
			+ "<point number=\"38\" name=\"Vll ave mn\" units=\"Volts\" value=\"NULL\" />\n"
			+ "<point number=\"39\" name=\"I ave mn\" units=\"Amps\" value=\"NULL\" />\n"
			+ "<point number=\"40\" name=\"Freq mn\" units=\"Hz\" value=\"NULL\" />\n"
			+ "<point number=\"41\" name=\"kW sd del-rec\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"42\" name=\"kVA sd del+rec\" units=\"kVA\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"43\" name=\"kVAR sd del-rec\" units=\"kVAR\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"44\" name=\"kW sd mx d-r\" units=\"kW\" value=\"NULL\" />\n"
			+ "<point number=\"45\" name=\"kVA sd mx d+r\" units=\"kVA\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"46\" name=\"kVAR sd mx d-r\" units=\"kVAR\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"47\" name=\"Phase Rev\" units=\"\" value=\"NULL\" />\n"
			+ "<point number=\"48\" name=\"kWh del\" units=\"kWh\" value=\"NULL\" />\n"
			+ "<point number=\"49\" name=\"kWh rec\" units=\"kWh\" value=\"NULL\" />\n"
			+ "<point number=\"50\" name=\"kWh del+rec\" units=\"kWh\" value=\"NULL\" />\n"
			+ "<point number=\"51\" name=\"kWh del-rec\" units=\"kWh\" value=\"NULL\" />\n"
			+ "<point number=\"52\" name=\"kVARh del\" units=\"kVARh\" value=\"NULL\" />\n"
			+ "<point number=\"53\" name=\"kVARh rec\" units=\"kVARh\" value=\"NULL\" />\n"
			+ "<point number=\"54\" name=\"kVARh del+rec\" units=\"kVARh\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"55\" name=\"kVARh del-rec\" units=\"kVARh\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"56\" name=\"kVAh del+rec\" units=\"kVAh\" value=\"NULL\""
			+ " />\n"
			+ "<point number=\"57\" name=\"V1 THD mx\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"58\" name=\"V2 THD mx\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"59\" name=\"V3 THD mx\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"60\" name=\"I1 THD mx\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"61\" name=\"I2 THD mx\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"62\" name=\"I3 THD mx\" units=\"%\" value=\"NULL\" />\n"
			+ "<point number=\"63\" name=\"(unused)\" units=\"\" value=\"NULL\" />\n"
			+ "</record>\n"
			+ "</records>\n"
			+ "</device>\n"
			+ "</devices>\n"
			+ "</DAS>";

	String device2XmlNoDate = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
			+ "<DAS>\n"
			+ "<mode>LOGFILEUPLOAD</mode>\n"
			+ "<name>xxxxC600xxxx</name>\n"
			+ "<serial>xxxxC600xxxx</serial>\n"
			+ "<devices>\n"
			+ "<device>\n"
			+ "<name>"
			+ deviceName
			+ "</name>\n"
			+ "<address>100</address>\n"
			+ "<type>Power Measurement ION 8600</type>\n"
			+ "<class>12</class>\n"
			+ "<numpoints>64</numpoints>\n"
			+ "<records>\n"
			+ "<record>\n"
			+ "<error text=\"Start log\">160</error>\n"
			+ "</record>\n"
			+ "</records>\n"
			+ "</device>\n"
			+ "</devices>\n"
			+ "</DAS>";

	String device2XmlBadDate = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
			+ "<DAS>\n"
			+ "<mode>LOGFILEUPLOAD</mode>\n"
			+ "<name>xxxxC600xxxx</name>\n"
			+ "<serial>xxxxC600xxxx</serial>\n"
			+ "<devices>\n"
			+ "<device>\n"
			+ "<name>"
			+ deviceName
			+ "</name>\n"
			+ "<address>100</address>\n"
			+ "<type>Power Measurement ION 8600</type>\n"
			+ "<class>12</class>\n"
			+ "<numpoints>64</numpoints>\n"
			+ "<records>\n"
			+ "<record>\n"
			+ "<time zone=\"UTC\">NULL</time>\n"
			+ "<error text=\"Start log\">160</error>\n"
			+ "</record>\n"
			+ "</records>\n"
			+ "</device>\n"
			+ "</devices>\n"
			+ "</DAS>";

	String device2XmlNoTZ = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
			+ "<DAS>\n"
			+ "<mode>LOGFILEUPLOAD</mode>\n"
			+ "<name>xxxxC600xxxx</name>\n"
			+ "<serial>xxxxC600xxxx</serial>\n"
			+ "<devices>\n"
			+ "<device>\n"
			+ "<name>"
			+ deviceName
			+ "</name>\n"
			+ "<address>100</address>\n"
			+ "<type>Power Measurement ION 8600</type>\n"
			+ "<class>12</class>\n"
			+ "<numpoints>64</numpoints>\n"
			+ "<records>\n"
			+ "<record>\n"
			+ "<time>2023-08-23 18:22:25</time>\n"
			+ "<error text=\"Start log\">160</error>\n"
			+ "</record>\n"
			+ "</records>\n"
			+ "</device>\n"
			+ "</devices>\n"
			+ "</DAS>";
}

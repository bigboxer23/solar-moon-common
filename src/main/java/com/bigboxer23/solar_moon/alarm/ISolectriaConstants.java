package com.bigboxer23.solar_moon.alarm;

import java.util.HashMap;
import java.util.Map;

/** */
public interface ISolectriaConstants {
	// https://solectria.com/site/assets/files/2340/docr-070468-b_solectria_renewables_modbus_level_7.pdf
	String FAN_OVER_40K_HOURS = "2048.000"; // TODO: remove

	int DC_voltage_Low = 1;
	int DC_Voltage_High = 1 << 1;
	int AC_Voltage_Critical_Low = 1 << 2;
	int AC_Voltage_Low = 1 << 3;
	int AC_Voltage_High = 1 << 4;
	int AC_Voltage_Critical_High = 1 << 5;
	int Low_DC_Power_Condition = 1 << 6;
	int Operation_Outside_MPPT = 1 << 7;
	int Power_Conversion_Current_Limit = 1 << 8;
	int Waiting_for_Restart = 1 << 9;
	int Waiting_for_More_DC_Power = 1 << 10;
	int Fan_Life_Reached = 1 << 11;
	int AC_Frequency_Low = 1 << 12;
	int AC_Frequency_High = 1 << 13;
	int Waiting_for_Grid = 1 << 14;
	int UL_Islanding_Fault = 1 << 15;

	Map<Integer, String> INFORMATIVE_ERROR_CODES = new HashMap<Integer, String>() {
		{
			put(DC_voltage_Low, "DC voltage Low");
			put(DC_Voltage_High, "DC Voltage High");
			put(AC_Voltage_Critical_Low, "AC Voltage Critical Low");
			put(AC_Voltage_Low, "AC Voltage Low");
			put(AC_Voltage_High, "AC Voltage High");
			put(AC_Voltage_Critical_High, "AC Voltage Critical High");
			put(Low_DC_Power_Condition, "Low DC Power Condition");
			put(Operation_Outside_MPPT, "Operation Outside MPPT");
			put(Power_Conversion_Current_Limit, "Power Conversion Current Limit");
			put(Waiting_for_Restart, "Waiting For Restart");
			put(Waiting_for_More_DC_Power, "Waiting For More DC Power");
			put(Fan_Life_Reached, "Fan Life Reached");
			put(AC_Frequency_Low, "AC Frequency Low");
			put(AC_Frequency_High, "AC Frequency High");
			put(Waiting_for_Grid, "Waiting For Grid");
			put(UL_Islanding_Fault, "UL Islanding Fault");
		}
	};

	int Temperature_Sensor_Failure = 1;
	int AC_Contactor_Opened = 1 << 1;
	int Power_Stage_Over_Temperature = 1 << 2;
	int Power_Stage_Desaturation = 1 << 3;
	int Contactor_Failure = 1 << 4;
	int AC_Current_Sensor_Circuit_Failure = 1 << 5;
	int MOV_Failure = 1 << 6;
	int Ground_Fault_Failure = 1 << 7;
	int VAC_Sense_Circuit_Failure = 1 << 8;
	int Open_Phase_Failure = 1 << 9;
	int MAG_Failure = 1 << 10;

	Map<Integer, String> CRITICAL_ERROR_CODES = new HashMap<Integer, String>() {
		{
			put(Temperature_Sensor_Failure, "Temperature Sensor Failure");
			put(AC_Contactor_Opened, "AC Contactor Opened");
			put(Power_Stage_Over_Temperature, "Power Stage Over Temperature");
			put(Power_Stage_Desaturation, "Power Stage Desaturation");
			put(Contactor_Failure, "Contactor Failure ");
			put(AC_Current_Sensor_Circuit_Failure, "AC Current Sensor Circuit Failure");
			put(MOV_Failure, "MOV Failure ");
			put(Ground_Fault_Failure, "Ground Fault Failure");
			put(VAC_Sense_Circuit_Failure, "VAC Sense Circuit Failure ");
			put(Open_Phase_Failure, "Open Phase Failure");
			put(MAG_Failure, "MAG Failure");
		}
	};
}

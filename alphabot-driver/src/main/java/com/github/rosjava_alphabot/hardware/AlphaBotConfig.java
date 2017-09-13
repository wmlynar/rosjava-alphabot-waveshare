package com.github.rosjava_alphabot.hardware;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.GpioUtil;

public class AlphaBotConfig {
	// PIN MAPPING
	public final static Pin leftMotorForward =			RaspiPin.GPIO_26;
	public final static Pin leftMotorBackward =			RaspiPin.GPIO_23;
	public final static Pin leftMotorPWM =				RaspiPin.GPIO_22;
	
	public final static Pin rightMotorForward =			RaspiPin.GPIO_29;
	public final static Pin rightMotorBackward =		RaspiPin.GPIO_28;
	public final static Pin rightMotorPWM =				RaspiPin.GPIO_25;
	
	public final static Pin leftEncoder =				RaspiPin.GPIO_11;
	public final static Pin rightEncoder =				RaspiPin.GPIO_10;
	
	public final static Pin trackerChipSelect = 		RaspiPin.GPIO_21;
	public final static Pin trackerClock = 				RaspiPin.GPIO_06;
	public final static Pin trackerAddress = 			RaspiPin.GPIO_05;
	public final static Pin trackerDataOut = 			RaspiPin.GPIO_04;
	
	// CHASSIS PARAMETERS
	public final static double ticksPerMeter =			190.48 / 2;
	public final static double baseWidthInMeters =		0.132;
	
	// SENSOR PARAMETERS
	public final static int trackerNumberOfSensors =	5;
	public final static int trackerColorSensingThreshold =	500;
	
	// DEFINITIONS
	public enum Side {LEFT, RIGHT}
	
	public static void enableNonPrivilegedAccess() {
		try {
			GpioUtil.enableNonPrivilegedAccess();} catch (RuntimeException re) {
				System.out.println("Unable to run with user privileges");
			}
	}
}

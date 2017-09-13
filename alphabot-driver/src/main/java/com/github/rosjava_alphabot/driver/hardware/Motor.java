package com.github.rosjava_alphabot.driver.hardware;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
//import com.pi4j.io.gpio.GpioPinPwm;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;

public class Motor {
	private GpioController gpio;
	
	private GpioPinDigitalOutput forward;
	private GpioPinDigitalOutput backward;
	private GpioPinPwmOutput pwm;
	
	private final int MAX_PWM = 100;
	private final int MIN_PWM = -100;
	
	public Motor(Pin Forward, Pin Backward, Pin PWM){
		AlphaBotConfig.enableNonPrivilegedAccess();
		this.gpio = GpioFactory.getInstance();
		
		this.forward = gpio.provisionDigitalOutputPin(Forward);
		this.backward = gpio.provisionDigitalOutputPin(Backward);
		this.pwm = gpio.provisionSoftPwmOutputPin(PWM);
	}
	
	public Motor(AlphaBotConfig.Side side) {
		AlphaBotConfig.enableNonPrivilegedAccess();
		this.gpio = GpioFactory.getInstance();
		
		Pin Forward, Backward, PWM;
		
		if(side == AlphaBotConfig.Side.LEFT) {
			Forward = AlphaBotConfig.leftMotorForward;
			Backward = AlphaBotConfig.leftMotorBackward;
			PWM = AlphaBotConfig.leftMotorPWM;
		}
		else {
			Forward = AlphaBotConfig.rightMotorForward;
			Backward = AlphaBotConfig.rightMotorBackward;
			PWM = AlphaBotConfig.rightMotorPWM;
		}
		this.forward = gpio.provisionDigitalOutputPin(Forward);
		this.backward = gpio.provisionDigitalOutputPin(Backward);
		this.pwm = gpio.provisionSoftPwmOutputPin(PWM);
	}
	/**
	 * PWM is trimmed to fit in range [-100, 100]
	 */
	public void setPWM(int PWM){
		PWM = trimPWM(PWM);
		
		if(PWM >= 0) {
			forward.setState(PinState.HIGH);
			backward.setState(PinState.LOW);
			pwm.setPwm(PWM);
		}
		else {
			forward.setState(PinState.LOW);
			backward.setState(PinState.HIGH);
			pwm.setPwm(-PWM);
		}
	}
	public int getPWM() {
		return this.pwm.getPwm();
	}
	
	public void stop() {
		forward.setState(PinState.LOW);
		backward.setState(PinState.LOW);
		pwm.setPwm(0);
	}
	
	private int trimPWM(int input) {
		if(input > MAX_PWM) return MAX_PWM;
		if(input < MIN_PWM) return MIN_PWM;
		return input;
		
	}
}

package com.github.rosjava_alphabot.driver.hardware;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;

public class EncoderCounter {

	private Object monitor = new Object();
	private volatile int count = 0;
	private volatile int direction = 1;
	private GpioController gpio;
	private GpioPinDigitalInput encoderPin;

	public EncoderCounter(AlphaBotConfig.Side side) {
		AlphaBotConfig.enableNonPrivilegedAccess();
		this.gpio = GpioFactory.getInstance();
		if (side == AlphaBotConfig.Side.LEFT) {
			this.encoderPin = gpio.provisionDigitalInputPin(AlphaBotConfig.leftEncoder);
		} else {
			this.encoderPin = gpio.provisionDigitalInputPin(AlphaBotConfig.rightEncoder);
		}
		
		direction = 1;
	}

	public int getTicks() {
		synchronized (monitor) {
			return count;
		}
	}

	public void setForward(boolean forward) {
		synchronized (monitor) {
			if (forward) {
				direction = 1;
			} else {
				direction = -1;
			}
		}
	}

	public void startThread() {
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				boolean currentEncoderState = encoderPin.isHigh();
				while (true) {
					boolean state = encoderPin.isHigh();
					if (state != currentEncoderState) {
						currentEncoderState = state;
						if (currentEncoderState) {
							increaseCounter();
						}
					}

				}
			}
		});
		t.start();
	}

	private void increaseCounter() {
		synchronized (monitor) {
			count += direction;
		}
	}
}

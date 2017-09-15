package com.github.rosjava_alphabot.driver.hardware;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

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

	public void mountEvent() {
		this.encoderPin.setShutdownOptions(true);
		this.encoderPin.addListener(new GpioPinListenerDigital() {
            //@Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // place callback here
            	if(encoderPin.isHigh()) {
            		increaseCounter();
            	}
            }
        });
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

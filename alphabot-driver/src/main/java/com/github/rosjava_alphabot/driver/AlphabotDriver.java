package com.github.rosjava_alphabot.driver;

import com.github.rosjava_alphabot.driver.dto.DistDto;
import com.github.rosjava_alphabot.driver.dto.TwistDto;
import com.github.rosjava_alphabot.encoder.AlphaBotConfig;

public class AlphabotDriver {
	
	double TICKS_PER_METER = 190.48 / 2;
	double BASE_WIDTH = 0.17;
	
	EncoderCounter counterLeft = new EncoderCounter(AlphaBotConfig.Side.LEFT);
	EncoderCounter counterRight = new EncoderCounter(AlphaBotConfig.Side.RIGHT);
	Motor motorLeft = new Motor(AlphaBotConfig.Side.LEFT);
	Motor motorRight = new Motor(AlphaBotConfig.Side.RIGHT);
	
	public AlphabotDriver() {
		
	}
	
	public void startThreads() {
		System.out.println("driver");
		counterLeft.startThread();
		counterRight.startThread();
	}
	
	public DistDto getDistances() {
		
		int left = counterLeft.getTicks();
		int right = counterRight.getTicks();
		
		DistDto dist = new DistDto();
		dist.left = left / TICKS_PER_METER;
		dist.right = right / TICKS_PER_METER;
		
		return dist;
	}

	public void processTwistMessage(TwistDto twist) {
		double velocityLeft = twist.linear - twist.angular * BASE_WIDTH * 0.5;
		double velocityRight = twist.linear + twist.angular * BASE_WIDTH * 0.5;
		
		motorLeft.setPWM((int) (velocityLeft * 50));
		motorRight.setPWM((int) (velocityRight * 50));
	}

}

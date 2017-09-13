package com.github.rosjava_alphabot.driver;

import com.github.rosjava_alphabot.driver.dto.DistDto;
import com.github.rosjava_alphabot.driver.dto.TwistDto;
import com.github.rosjava_alphabot.driver.utils.Differentiator;
import com.github.rosjava_alphabot.driver.utils.MiniPID;
import com.github.rosjava_alphabot.encoder.AlphaBotConfig;

public class AlphabotDriver {
	
	private double TICKS_PER_METER = 190.48 / 2;
	private double BASE_WIDTH = 0.17;
	
	private EncoderCounter counterLeft = new EncoderCounter(AlphaBotConfig.Side.LEFT);
	private EncoderCounter counterRight = new EncoderCounter(AlphaBotConfig.Side.RIGHT);
	private Motor motorLeft = new Motor(AlphaBotConfig.Side.LEFT);
	private Motor motorRight = new Motor(AlphaBotConfig.Side.RIGHT);
	private Differentiator leftDistDifferentiator = new Differentiator(1e-6);
	private Differentiator rightDistDifferentiator = new Differentiator(1e-6);
	private MiniPID leftPid = new MiniPID(1, 0, 0, 50);
	private MiniPID rightPid = new MiniPID(1, 0, 0, 50);
	private volatile double setpointVelocityLeft;
	private volatile double setpointVelocityRight;
	
	private Object monitor = new Object();
	
	public AlphabotDriver() {
		// PWM output needed to move the robot because of static friction
		leftPid.setMaxIOutput(30);
		rightPid.setMaxIOutput(30);
		
		// output cannot be higher than 100, or the software PWM will go crazy
		leftPid.setOutputLimits(100);
		rightPid.setOutputLimits(100);
	}
	
	public void setPidParameters(double p, double i, double d, double f) {
		leftPid.setPID(p, i, d, f);
		rightPid.setPID(p, i, d, f);
	}
	
	public void startThreads() {
		counterLeft.startThread();
		counterRight.startThread();
		
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					controlLoop();
				}
			}
		});
		t.start();
	}
	
	public DistDto getDistances() {
		
		DistDto dist = new DistDto();
		
		dist.left = counterLeft.getTicks() / TICKS_PER_METER;;
		dist.right = counterRight.getTicks() / TICKS_PER_METER;;
		
		return dist;
	}

	public void processTwistMessage(TwistDto twist) {
		synchronized (monitor) {
			setpointVelocityLeft = twist.linear - twist.angular * BASE_WIDTH * 0.5;
			setpointVelocityRight = twist.linear + twist.angular * BASE_WIDTH * 0.5;
		}
	}


	private void controlLoop() {

		// get setpoints
		double setpointVelocityLeft;
		double setpointVelocityRight;
		
		synchronized (monitor) {
			setpointVelocityLeft = this.setpointVelocityLeft;
			setpointVelocityRight = this.setpointVelocityRight;
		}

		// get time
		double time = System.currentTimeMillis() / 1000.;
		
		// compute velocities
		double currentLeftDistance = counterLeft.getTicks() / TICKS_PER_METER;;
		double currentRightDistance = counterRight.getTicks() / TICKS_PER_METER;;
		
		double currentLeftVelocity = leftDistDifferentiator.differentiate(time, currentLeftDistance);
		double currentRightVelocity = rightDistDifferentiator.differentiate(time, currentRightDistance);
		
		if(!leftDistDifferentiator.isOk() || !rightDistDifferentiator.isOk()) {
			return;
		}
		
		// get PID output
		int pwmLeft = (int) leftPid.getOutput(currentLeftVelocity, setpointVelocityLeft);
		int pwmRight = (int) rightPid.getOutput(currentRightVelocity, setpointVelocityRight);

		// set output
		motorLeft.setPWM(pwmLeft);
		motorRight.setPWM(pwmRight);

		// set direction of rotation for tick counting
		counterLeft.setForward(pwmLeft>=0);
		counterRight.setForward(pwmRight>=0);
		
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
		}
	}
}

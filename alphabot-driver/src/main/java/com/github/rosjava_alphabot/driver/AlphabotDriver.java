package com.github.rosjava_alphabot.driver;

import com.github.rosjava_alphabot.driver.dto.DistancesDto;
import com.github.rosjava_alphabot.driver.dto.VelocitiesDto;
import com.github.rosjava_alphabot.driver.hardware.AlphaBotConfig;
import com.github.rosjava_alphabot.driver.hardware.EncoderCounter;
import com.github.rosjava_alphabot.driver.hardware.Motor;
import com.github.rosjava_alphabot.driver.utils.Differentiator;
import com.github.rosjava_alphabot.driver.utils.MiniPID;

public class AlphabotDriver {
	
	public static int PERIOD_MS = 200;
	
	private EncoderCounter counterLeft = new EncoderCounter(AlphaBotConfig.Side.LEFT);
	private EncoderCounter counterRight = new EncoderCounter(AlphaBotConfig.Side.RIGHT);
	private Motor motorLeft = new Motor(AlphaBotConfig.Side.LEFT);
	private Motor motorRight = new Motor(AlphaBotConfig.Side.RIGHT);
	private Differentiator distDifferentiatorLeft = new Differentiator(1e-6);
	private Differentiator distDifferentiatorRight = new Differentiator(1e-6);
	private MiniPID leftPid = new MiniPID(50, 50*PERIOD_MS/1000., 0, 100);
	private MiniPID rightPid = new MiniPID(50, 50*PERIOD_MS/1000., 0, 115);
	private volatile double setpointVelocityLeft;
	private volatile double setpointVelocityRight;
	
	private Object monitor = new Object();
	private double currentVelocityLeft;
	private double currentVelocityRight;
	private double measurementTime;
	
	public AlphabotDriver() {
		// PWM output needed to move the robot because of static friction
		leftPid.setMaxIOutput(50);
		rightPid.setMaxIOutput(50);
		
		// output cannot be higher than 100, or the software PWM will go crazy
		leftPid.setOutputLimits(100);
		rightPid.setOutputLimits(100);
	}

	
	public void startThreads() {
		//counterLeft.startThread();
		counterLeft.mountEvent();
		//counterRight.startThread();
		counterRight.mountEvent();
		
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
	
	public void setPidParameters(double p, double i, double d, double f) {
		leftPid.setPID(p, i*PERIOD_MS/1000., d, f);
		rightPid.setPID(p, i*PERIOD_MS/1000., d, f);
	}
	
	public void setVelocities(VelocitiesDto velocities) {
		synchronized (monitor) {
			setpointVelocityLeft = velocities.left;
			setpointVelocityRight = velocities.right;
		}
	}

	public VelocitiesDto getVelocities() {
		
		VelocitiesDto dist = new VelocitiesDto();
		
		synchronized (monitor) {
			dist.time = measurementTime;
			dist.left = currentVelocityLeft;
			dist.right = currentVelocityRight;
		}
		
		return dist;
	}

	public DistancesDto getDistances() {
		
		DistancesDto dist = new DistancesDto();
		
		dist.left = counterLeft.getTicks() / AlphaBotConfig.ticksPerMeter;;
		dist.right = counterRight.getTicks() / AlphaBotConfig.ticksPerMeter;

		return dist;
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
		double currentDistanceLeft = counterLeft.getTicks() / AlphaBotConfig.ticksPerMeter;
		double currentDistanceRight = counterRight.getTicks() / AlphaBotConfig.ticksPerMeter;
		
		double currentVelocityLeft = distDifferentiatorLeft.differentiate(time, currentDistanceLeft);
		double currentVelocityRight = distDifferentiatorRight.differentiate(time, currentDistanceRight);

		synchronized (monitor) {
			this.currentVelocityLeft = currentVelocityLeft;
			this.currentVelocityRight = currentVelocityRight;
			this.measurementTime = time;
		}
		
		// get PID output
		int pwmLeft = (int) leftPid.getOutput(currentVelocityLeft, setpointVelocityLeft);
		int pwmRight = (int) rightPid.getOutput(currentVelocityRight, setpointVelocityRight);
		
		// set output
		motorLeft.setPWM(pwmLeft);
		motorRight.setPWM(pwmRight);

		// set direction of rotation for tick counting
		counterLeft.setForward(pwmLeft>=0);
		counterRight.setForward(pwmRight>=0);
		
		// sleep
		try {
			Thread.sleep(PERIOD_MS);
		} catch (InterruptedException e) {
		}
	}
}

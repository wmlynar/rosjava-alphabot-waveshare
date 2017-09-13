package com.github.rosjava_alphabot.node;

import org.ros.concurrent.CancellableLoop;
import org.ros.concurrent.Rate;
import org.ros.concurrent.WallTimeRate;
import org.ros.message.MessageListener;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import com.github.rosjava_alphabot.driver.AlphabotDriver;
import com.github.rosjava_alphabot.driver.dto.DistancesDto;
import com.github.rosjava_alphabot.driver.dto.VelocitiesDto;

import geometry_msgs.Twist;
import geometry_msgs.Vector3Stamped;

public class AlphabotNode extends AbstractNodeMain {

	private AlphabotDriver driver = new AlphabotDriver();
	
	private Publisher<Vector3Stamped> distPublisher = null;
	private static int QUEUE_SIZE = 10;

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("alphabot");
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		
		driver.startThreads();
		
		// publish distance traveled
		distPublisher = connectedNode.newPublisher("dist", Vector3Stamped._TYPE);
		connectedNode.executeCancellableLoop(new CancellableLoop() {

			@Override
			protected void loop() throws InterruptedException {
				
				Time time = connectedNode.getCurrentTime();

				DistancesDto dist = driver.getDistances();

				Vector3Stamped distVector = distPublisher.newMessage();
				distVector.getHeader().setStamp(time);
				distVector.getVector().setX(dist.left);
				distVector.getVector().setY(dist.right);
				distPublisher.publish(distVector);

				Thread.sleep(100);
			}

		});

		// process twist messages
		Subscriber<Twist> twistSubscriber = connectedNode.newSubscriber("cmd_vel", Twist._TYPE);
		twistSubscriber.addMessageListener(new MessageListener<Twist>() {
			
			@Override
			public void onNewMessage(Twist m) {
				VelocitiesDto twist = new VelocitiesDto();
				
				double linear = m.getLinear().getX();
				double angular = m.getAngular().getZ();
				
				twist.velocityLeft = linear - angular * AlphabotDriver.BASE_WIDTH * 0.5;
				twist.velocityRight = linear + angular * AlphabotDriver.BASE_WIDTH * 0.5;
				
				driver.processTwistMessage(twist);
			}
		}, QUEUE_SIZE);
	}
	
}

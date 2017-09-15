package com.github.rosjava_alphabot.node;

import java.util.List;

import org.ros.concurrent.CancellableLoop;
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
import com.github.rosjava_alphabot.driver.hardware.AlphaBotConfig;

import geometry_msgs.TransformStamped;
import geometry_msgs.Twist;
import geometry_msgs.Vector3;
import geometry_msgs.Vector3Stamped;

public class AlphabotNode extends AbstractNodeMain {

	private AlphabotDriver driver = new AlphabotDriver();

	private double lastAbsolutePositionX;
	private double lastAbsolutePositionY;
	private double lastAbsolutePositionTh;
	private DistancesDto prevDist;

	private static int QUEUE_SIZE = 10;

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("alphabot");
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {

		driver.startThreads();

		// publish velocities
		Publisher<Vector3Stamped> velocityPublisher = connectedNode.newPublisher("vel", Vector3Stamped._TYPE);
		// publish distance traveled
		Publisher<Vector3Stamped> distPublisher = connectedNode.newPublisher("dist", Vector3Stamped._TYPE);
		// publish odometry
		final Publisher<nav_msgs.Odometry> odometryPublisher = connectedNode.newPublisher("odom",
				nav_msgs.Odometry._TYPE);
		// transform
		final Publisher<tf2_msgs.TFMessage> transformPublisher = connectedNode.newPublisher("tf",
				tf2_msgs.TFMessage._TYPE);

		connectedNode.executeCancellableLoop(new CancellableLoop() {

			@Override
			protected void loop() throws InterruptedException {

				Time time = connectedNode.getCurrentTime();

				DistancesDto dist = driver.getDistances();
				VelocitiesDto velocities = driver.getVelocities();

				if (prevDist == null) {
					prevDist = dist;
					return;
				}

				// publish wheel distance travelled
				Vector3Stamped distVector = distPublisher.newMessage();
				distVector.getHeader().setStamp(time);
				distVector.getVector().setX(dist.left);
				distVector.getVector().setY(dist.right);
				distPublisher.publish(distVector);

				// publish wheel velocities
				Vector3Stamped velocityVector = velocityPublisher.newMessage();
				velocityVector.getHeader().setStamp(Time.fromNano((long) (1000000000L * velocities.time)));
				velocityVector.getVector().setX(velocities.left);
				velocityVector.getVector().setY(velocities.right);
				velocityPublisher.publish(velocityVector);

				// compute odometry

				double linearVelocity = (velocities.left + velocities.right) / 2;
				double angularVelocity = (velocities.right - velocities.left)
						/ (AlphaBotConfig.baseWidthInMeters * 0.5);

				double dLeft = dist.left - prevDist.left;
				double dRight = dist.right - prevDist.right;

				double translation = (dLeft + dRight) / 2;
				double rotation = (dRight - dLeft) / (AlphaBotConfig.baseWidthInMeters * 0.5);

				double relativeDistanceX = Math.cos(rotation) * translation;
				double relativeDistanceY = -Math.sin(rotation) * translation;

				lastAbsolutePositionX += Math.cos(lastAbsolutePositionTh) * relativeDistanceX
						- Math.sin(lastAbsolutePositionTh) * relativeDistanceY;
				lastAbsolutePositionY += Math.sin(lastAbsolutePositionTh) * relativeDistanceX
						+ Math.cos(lastAbsolutePositionTh) * relativeDistanceY;
				lastAbsolutePositionTh += rotation;

				geometry_msgs.Point position = connectedNode.getTopicMessageFactory()
						.newFromType(geometry_msgs.Point._TYPE);
				position.setX(lastAbsolutePositionX);
				position.setY(lastAbsolutePositionY);

				geometry_msgs.Quaternion orientation = connectedNode.getTopicMessageFactory()
						.newFromType(geometry_msgs.Quaternion._TYPE);
				orientation.setZ(Math.sin(lastAbsolutePositionTh / 2));
				orientation.setW(Math.cos(lastAbsolutePositionTh / 2));

				// publish transform

				geometry_msgs.TransformStamped newTransform = connectedNode.getTopicMessageFactory()
						.newFromType(geometry_msgs.TransformStamped._TYPE);

				newTransform.getHeader().setStamp(time);
				newTransform.getHeader().setFrameId("odom");

				newTransform.setChildFrameId("base_link");

				newTransform.getTransform().getTranslation().setX(lastAbsolutePositionX);
				newTransform.getTransform().getTranslation().setY(lastAbsolutePositionY);
				newTransform.getTransform().setRotation(orientation);

				tf2_msgs.TFMessage transformMessage = transformPublisher.newMessage();
				List<TransformStamped> transformList = transformMessage.getTransforms();
				transformList.add(newTransform);
				transformMessage.setTransforms(transformList);

				transformPublisher.publish(transformMessage);

				// publish odometry

				nav_msgs.Odometry newOdometry = odometryPublisher.newMessage();

				newOdometry.getHeader().setStamp(time);
				newOdometry.getHeader().setFrameId("odom");

				newOdometry.setChildFrameId("base_link");

				newOdometry.getPose().getPose().setPosition(position);
				newOdometry.getPose().getPose().setOrientation(orientation);

				newOdometry.getTwist().getTwist().getLinear().setX(linearVelocity);
				newOdometry.getTwist().getTwist().getAngular().setZ(angularVelocity);

				odometryPublisher.publish(newOdometry);

				prevDist = dist;

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

				twist.left = linear - angular * AlphaBotConfig.baseWidthInMeters * 0.5;
				twist.right = linear + angular * AlphaBotConfig.baseWidthInMeters * 0.5;

				driver.setVelocities(twist);
			}
		}, QUEUE_SIZE);

		// process parameters messages
		Subscriber<Vector3> parameterSubscriber = connectedNode.newSubscriber("parameters", Vector3._TYPE);
		parameterSubscriber.addMessageListener(new MessageListener<Vector3>() {

			@Override
			public void onNewMessage(Vector3 m) {
				double p = m.getX();
				double i = m.getY();
				double f = m.getZ();

				driver.setPidParameters(p, i, 0, f);
			}
		}, QUEUE_SIZE);
	}

}

package com.github.rosjava_alphabot.node;

import org.ros.concurrent.CancellableLoop;
import org.ros.concurrent.Rate;
import org.ros.concurrent.WallTimeRate;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import geometry_msgs.Vector3Stamped;

public class AlphabotNode extends AbstractNodeMain {

	private AlphabotDriver driver = new AlphabotDriver();
	private Publisher<Vector3Stamped> distPublisher = null;

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("alphabot");
	}
	
	@Override
	public void onStart(final ConnectedNode connectedNode) {

		Rate distRate = new WallTimeRate(10);
		distPublisher = connectedNode.newPublisher("dist", Vector3Stamped._TYPE);
		connectedNode.executeCancellableLoop(new CancellableLoop() {

			@Override
			protected void loop() throws InterruptedException {
				Time time = connectedNode.getCurrentTime();
				
				Dist dist = driver.getDistances();
				
				Vector3Stamped distVector = distPublisher.newMessage();
				distVector.getHeader().setStamp(time);
				distVector.getVector().setX(dist.left);
				distVector.getVector().setY(dist.right);
				
				distRate.wait();
			}
			
		});
		
	}
	

}

package com.github.rosjava_alphabot.node;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import geometry_msgs.Vector3Stamped;

public class AlphabotNode extends AbstractNodeMain {

	private Publisher<Vector3Stamped> publisher = null;

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("alphabot");
	}
	
	@Override
	public void onStart(final ConnectedNode connectedNode) {
		AlphabotDriver driver = new AlphabotDriver();

		publisher = connectedNode.newPublisher("dist", Vector3Stamped._TYPE);
		connectedNode.executeCancellableLoop(new CancellableLoop() {

			@Override
			protected void loop() throws InterruptedException {
			}
			
		});
		
	}
	

}

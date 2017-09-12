package com.github.rosjava_alphabot.node;

import org.ros.internal.loader.CommandLineLoader;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class Main {
	private static String[] EMPTY = { "" };

	private static AlphabotNode alphabotNode = new AlphabotNode();

	public static void main(String[] args) {
		NodeConfiguration nodeConfiguration = null;
		if (args.length == 0) {
			args = EMPTY;
		}
		CommandLineLoader loader = new CommandLineLoader(Lists.newArrayList(args));
		nodeConfiguration = loader.build();

		NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
		nodeMainExecutor.execute(alphabotNode, nodeConfiguration);
	}

}

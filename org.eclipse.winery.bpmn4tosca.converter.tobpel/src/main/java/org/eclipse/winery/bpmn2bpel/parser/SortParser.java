/*******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/
package org.eclipse.winery.bpmn2bpel.parser;

import org.eclipse.winery.bpmn2bpel.model.ManagementFlow;
import org.eclipse.winery.bpmn2bpel.model.Node;
import org.eclipse.winery.bpmn2bpel.model.OrGatewayMerge;
import org.eclipse.winery.bpmn2bpel.model.OrGatewaySplit;

import java.util.*;

/**
 * This class will do two things.
 * <p>
 * First, it will finds all of the following nodes for each exclusive gateway, and construct a
 * branch for each following nodes.
 * <p>
 * second, it will convert the nodes to a management flow.
 */
public class SortParser {
	private Map<String, Node> nodeMap;

	private Map<String, Set<String>> nodeWithTargetsMap;

	public SortParser(Map<String, Node> nodeMap, Map<String, Set<String>> nodeWithTargetsMap) {
		super();

		this.nodeMap = nodeMap;
		this.nodeWithTargetsMap = nodeWithTargetsMap;
	}

	public SortParser() {
		super();
	}

	public ManagementFlow buildManagementFlow() {
		List<Node> nodeList = buildNodeList(getStartEvent().getId());
		ManagementFlow managementFlow = convert2ManagementFlow(nodeList);

		return managementFlow;
	}

	private Node getStartEvent() {
		for (Node node : nodeMap.values()) {
			if (JsonKeys.NODE_TYPE_START_EVENT.equals(node.getType())) {
				return node;
			}
		}

		return null;
	}

	private ManagementFlow convert2ManagementFlow(List<Node> nodeList) {
		ManagementFlow managementFlow = new ManagementFlow();

		for (Node node : nodeList) {
			managementFlow.addVertex(node);
		}

		Node src = null;
		Node target = null;
		for (int i = 0; i < nodeList.size() - 1; i++) {
			src = nodeList.get(i);
			target = nodeList.get(i + 1);
			managementFlow.addEdge(src, target);
		}

		return managementFlow;
	}

	private List<Node> buildNodeList(String currentId) {
		List<Node> nodeList = new ArrayList<Node>();
		boolean hasNext = true;
		do {
			Node node = nodeMap.get(currentId);

			Set<String> followSet = nodeWithTargetsMap.get(currentId);

			if (node instanceof OrGatewaySplit) {
				String endGatewayId = addBranches4Gateway((OrGatewaySplit) node);
				nodeList.add(node);

				// loop for the next node
				Set<String> endFollowSet = nodeWithTargetsMap.get(endGatewayId);
				currentId = endFollowSet.iterator().next();
			} else if (node instanceof OrGatewayMerge) {
				hasNext = false;
			} else { // task node
				nodeList.add(node);

				// the next node
				if (followSet == null || followSet.size() == 0) {
					// final node
					hasNext = false;
				} else {
					currentId = followSet.iterator().next();
				}
			}
		} while (hasNext);

		return nodeList;
	}

	private String addBranches4Gateway(OrGatewaySplit gateway) {
		String endGatewayId = null;

		Iterator<String> iterator = nodeWithTargetsMap.get(gateway.getId()).iterator();

		while (iterator.hasNext()) {
			String currentId = iterator.next();

			List<Node> subList = buildNodeList(currentId);
			gateway.getBranch(currentId).ifPresent((value) -> {
				value.setNodeList(subList);
			});

			if (null == endGatewayId) {
				endGatewayId = getEndDecisionNodeId(subList);
			}
		}

		return endGatewayId;
	}

	private String getEndDecisionNodeId(List<Node> subList) {

		Node lastTask = subList.get(subList.size() - 1);

		Set<String> followSet = nodeWithTargetsMap.get(lastTask.getId());
		return followSet.iterator().next();
	}
}

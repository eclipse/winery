/**
/*******************************************************************************
 * Copyright (c) 2015-2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Sebastian Wagner - initial API and implementation
 *     Armin Hüneburg - rename interface to fit modeler
 *******************************************************************************/
package org.eclipse.winery.bpmn2bpel.parser;

public interface JsonKeys {


	/*
	 * Field names of BPMN4Tosca Model
	 */

	public static final String NAME = "name";

	public static final String ID = "id";

	public static final String TYPE = "type";

	public static final String INPUT = "input";

	public static final String OUTPUT = "output";

	public static final String VALUE = "value";

	public static final String NODE_TEMPLATE = "node_template";

	public static final String NODE_OPERATION = "node_operation";

	public static final String NODE_INTERFACE_NAME = "interface";

	public static final String CONNECTIONS = "connections";


	/*
	 * Event, Management-Task Types
	 *
	 */
	public static final String TASK_TYPE_MGMT_TASK = "ToscaNodeManagementTask";

	public static final String TASK_TYPE_START_EVENT = "StartEvent";

	public static final String TASK_TYPE_END_EVENT = "EndEvent";


	/*
	 * Parameter Types
	 */
	public static final String PARAM_TYPE_VALUE_STRING = "string";

	public static final String PARAM_TYPE_VALUE_TOPOLOGY = "topology";

	public static final String PARAM_TYPE_VALUE_PLAN = "plan";

	public static final String PARAM_TYPE_VALUE_CONCAT = "concat";

	public static final String PARAM_TYPE_VALUE_IA = "implementation_artifact";

	public static final String PARAM_TYPE_VALUE_DA = "deployment_artifact";

}

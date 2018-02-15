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
package org.eclipse.winery.repository.export;

import org.apache.commons.io.output.NullOutputStream;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

@Ignore("not finished - CSARExporterTest is currently the way to go")
public class ToscaExportUtilTest {

    @Test
    public void exportTOSCA() throws Exception {
        ToscaExportUtil exporter = new ToscaExportUtil();
        // we include everything related
        Map<String, Object> conf = new HashMap<>();
        NullOutputStream out = new NullOutputStream();
        // exporter.exportTOSCA(TestToscaExporter.serviceTemplateId, out, conf);
    }
}

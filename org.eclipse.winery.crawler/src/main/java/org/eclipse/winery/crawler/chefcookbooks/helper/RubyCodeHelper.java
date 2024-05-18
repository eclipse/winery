/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.crawler.chefcookbooks.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;

/**
 * This class provides methods to prepare Ruby code for parsing. Provides methods to remove unnecessary newlines and
 * code.
 */
public class RubyCodeHelper {

    // Line endings where newline is allowed, but newline should removed to make the chefdsl grammar work properly.
    public static final String[] SET_VALUES = new String[] {"+", "=", "*", "-", ",", "{", "[", "("};

    // Line endings where a newline ist removed.
    public static final HashSet<String> LINE_ENDINGS = new HashSet<>(Arrays.asList(SET_VALUES));

    /**
     * Removes comments from Ruby Code and removes unnecessary newlines.
     *
     * @param buf Buffered reader buffering the code to prepare.
     * @return Returns prepared Ruby Code as String
     */
    public static String removeUnnecessaryCode(BufferedReader buf) throws IOException {
        String line = buf.readLine();
        String previousLine = null;
        String nextLine;
        StringBuilder sb = new StringBuilder();
        int BUFFER_SIZE = 1000;

        while (line != null) {
            if (!line.startsWith("#") && !line.trim().startsWith("Chef::")) {
                buf.mark(BUFFER_SIZE);
                nextLine = buf.readLine();
                removeUnnecessaryNewlines(previousLine, line, nextLine, sb);
                buf.reset();
            }

            previousLine = line;
            line = buf.readLine();
        }
        return sb.toString();
    }

    /**
     * Method removes unnecessary newlines from Ruby Code. Some newlines need to be removed for the Chef DSL Antlr 4
     * grammar, because they make the grammar more complex and error prone. Newlines are removed when previous newline
     * is a newline too. Newlines are removed when previous or next line start with a whitespace.
     *
     * @param previousLine Previous line from code.
     * @param line         Actual processed line from code.
     * @param nextLine     Next line of code.
     * @param sb           String Builder for building the file without unnecessary newlines.
     */
    private static void removeUnnecessaryNewlines(String previousLine, String line, String nextLine, StringBuilder sb) {
        boolean lineIsNewLine = line.isEmpty() || line.trim().length() == 0;
        boolean notFirstOrLastLine = previousLine != null && nextLine != null;
        boolean previousLineIsNewLine;

        if (notFirstOrLastLine) {
            previousLineIsNewLine = previousLine.trim().length() == 0;
        } else {
            previousLineIsNewLine = false;
        }

        if ((previousLineIsNewLine && lineIsNewLine) || (notFirstOrLastLine && lineIsNewLine && (previousLine.startsWith(" ") || nextLine.startsWith(" ")))) {

        } else if (lineIsNewLine) {
            sb.append(line).append("\n");
        } else if (LINE_ENDINGS.contains(line.trim().substring(line.trim().length() - 1))) {
            sb.append(line).append(" ");
        } else {
            sb.append(line).append("\n");
        }
    }

    /**
     * Method prepares ruby code for chefdsl grammar. This method is necessary to remove some unnecessary code, which
     * would make the grammar more complex. The original file is not changed.
     *
     * @param rubyFilePath The path to ruby file to prepare.
     * @return Returns a temporary file where prepared Ruby code is stored
     */
    public static File prepareCodeFromFile(String rubyFilePath) throws IOException {
        File temp = Files.createTempFile("temprubyfile", ".rb").toFile();
        temp.deleteOnExit();
        try (InputStream is = new FileInputStream(rubyFilePath)) {
            try (BufferedReader buf = new BufferedReader(new InputStreamReader(is))) {

                String fileAsString = removeUnnecessaryCode(buf);

                buf.close();
                is.close();

                try (PrintWriter out = new PrintWriter(temp)) {
                    out.println(fileAsString);
                }
            }
        }
        return temp;
    }
}

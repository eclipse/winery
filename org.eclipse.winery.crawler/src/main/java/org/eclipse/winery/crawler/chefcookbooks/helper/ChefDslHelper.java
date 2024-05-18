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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.winery.crawler.chefcookbooks.chefcookbook.ChefCookbookConfiguration;
import org.eclipse.winery.crawler.chefcookbooks.chefcookbook.CookbookParseResult;
import org.eclipse.winery.crawler.chefcookbooks.chefdslparser.ChefDSLLexer;
import org.eclipse.winery.crawler.chefcookbooks.chefdslparser.ChefDSLParser;
import org.eclipse.winery.crawler.chefcookbooks.chefdslparser.CodeInStringVisitor;
import org.eclipse.winery.crawler.chefcookbooks.chefdslparser.PrimaryBaseVisitor;
import org.eclipse.winery.crawler.chefcookbooks.constants.ChefDslConstants;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.antlr.v4.runtime.CharStreams.fromFileName;

public class ChefDslHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrimaryBaseVisitor.class.getName());

    /**
     * Check if String contains an Chef attribute.
     *
     * @param rubyString The String to check.
     * @return Returns true if String contains an attribute.
     */
    public static boolean hasChefAttributeInString(String rubyString) {
        boolean hasAttribute = false;
        if (rubyString.contains("#{")) {
            hasAttribute = true;
        }
        return hasAttribute;
    }

    /**
     * This method resolves Ruby code in a String.
     *
     * @param rubyString Ruby String with implemeted ruby code.
     */
    public static String resolveRubyStringWithAttributes(ChefCookbookConfiguration cookbookConfiguration, String rubyString) {
        int attributeBegin;
        int attributeEnd;
        boolean containsAttribute;
        String processString = rubyString;
        String attributeName;
        List attributeValueList;
        String attributeValue;
        StringBuilder result = new StringBuilder();

        containsAttribute = processString.contains("#{");
        while (containsAttribute) {
            attributeBegin = processString.indexOf("#{");
            attributeEnd = attributeBegin;

            result.append(processString.substring(0, attributeBegin));
            processString = processString.substring(attributeBegin);

            attributeEnd = processString.indexOf("}");
            attributeName = processString.substring(2, attributeEnd);

            if (attributeName.startsWith("node")) {
                attributeName = attributeName.substring(4);
            }

            attributeValueList = cookbookConfiguration.getAttribute(attributeName);

            if (attributeValueList == null) {
                attributeValue = attributeName;
            } else if (attributeValueList.size() == 1) {
                attributeValue = attributeValueList.get(0).toString();
            } else {
                LOGGER.info("Should not happen. Attribute in String is an array.");
                attributeValue = "";
            }
            result.append(attributeValue);
            processString = processString.substring(attributeEnd + 1);

            containsAttribute = processString.contains("#{");
            if (!containsAttribute) {
                result.append(processString);
            }
        }

        return result.toString();
    }

    /**
     * This method resolves the argument of an recipe call with ChefDsl "include_recipe 'recipe' " command. If recipe
     * call contains "::" it is splitted in two parts. The first part is name of the cookbook where the recipe is. The
     * second part is name of the called recipe.
     *
     * @param recipeCall The argument in the "include_recipe" command.
     * @return Returns a String array where the first part is the name of the cookbook and the second part is the name
     * of the recipe with *.rb ending.
     */
    public static String[] resolveRecipeCall(String recipeCall) {
        String[] parts = new String[2];

        if (recipeCall.contains("::")) {
            parts = recipeCall.split("(::)");
            parts[1] = parts[1] + ".rb";
        } else if (!recipeCall.isEmpty()) {
            parts[0] = recipeCall;
            parts[1] = ChefDslConstants.DEFAULT_RUBYFILE;
        }

        return parts;
    }

    /**
     * This method resolves Ruby code in a String.
     *
     * @param parseResult Parse Result with only one cookbook configuration.
     * @param rubyString  Ruby String with implemeted ruby code.
     * @return Returns the resolved string.
     */
    public static String resolveRubyStringWithCode(CookbookParseResult parseResult, String rubyString) {
        int codeBegin;
        int codeEnd;

        /**
         * Boolean to check if string contains ruby code.
         */
        boolean containsRubyCode;
        String processString = rubyString;
        String rubyCode;
        String resolvedRubyCode;

        /**
         * String builder to build the resolved string.
         */
        StringBuilder resultStringBuilder = new StringBuilder();

        containsRubyCode = processString.contains("#{");
        while (containsRubyCode) {
            codeBegin = processString.indexOf("#{");
            codeEnd = codeBegin;

            resultStringBuilder.append(processString.substring(0, codeBegin));
            processString = processString.substring(codeBegin);

            codeEnd = processString.indexOf("}");
            rubyCode = processString.substring(2, codeEnd);

            ChefDSLParser chefDSLParser = null;
            try {
                File file = Files.createTempFile("tempcodeinstringfile", ".tmp").toFile();
                BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                bw.write(rubyCode + "\n");
                bw.close();
                CharStream input = fromFileName(file.getAbsolutePath().replace("\\", "/"));
                ChefDSLLexer chefDSLLexer = new ChefDSLLexer(input);
                CommonTokenStream commonTokenStream = new CommonTokenStream(chefDSLLexer);
                chefDSLParser = new ChefDSLParser(commonTokenStream);
                file.delete();
                CodeInStringVisitor cookbookVisitor = new CodeInStringVisitor(parseResult);
                resolvedRubyCode = cookbookVisitor.visit(chefDSLParser.program());
            } catch (IOException e) {
                e.printStackTrace();
                resolvedRubyCode = "";
            }

            resultStringBuilder.append(resolvedRubyCode);
            processString = processString.substring(codeEnd + 1);

            containsRubyCode = processString.contains("#{");
            if (!containsRubyCode) {
                resultStringBuilder.append(processString);
            }
        }

        return resultStringBuilder.toString();
    }
}

<!---~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  ~ Copyright (c) 2020 Contributors to the Eclipse Foundation
  ~
  ~ See the NOTICE file(s) distributed with this work for additional
  ~ information regarding copyright ownership.
  ~
  ~ This program and the accompanying materials are made available under the
  ~ terms of the Eclipse Public License 2.0 which is available at
  ~ http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
  ~ which is available at https://www.apache.org/licenses/LICENSE-2.0.
  ~
  ~ SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->


# Eclipse Configuration HowTo

We have issues with removing trailing spaces at Eclipse.
Eclipse adds spaces between methods.
Since we cannot fix this issue, we either ask you to use IntelliJ or to review each commit not to include unnecessary spaces.

This howto is based on [Eclipse IDE for Java EE Developers].
First of all, generate a war to have all dependencies fetched by maven.

## Recommended plugins

* [AnyEdit](http://andrei.gmxhome.de/anyedit/) for ensuring that tabs are always used
    * Configure: Window -> Preferences -> General / Editors / AnyEdit Tools -> "Auto - Convert EXCLUSION file list" -> "Add filter" -> "*.java", "Convert...": 4 spaces for a tab

## Optional plugins

* [Eclipse Code Recommenders](http://www.eclipse.org/recommenders/)
* [VJET JavaScript IDE](http://www.eclipse.org/proposals/webtools.vjet/)

## Make Winery projects known to Eclipse

1. Import all projects
    * Use "Existing Maven Projects". `mvn eclips:m2eclipse` currently does not enable "maven" in eclipse.
2. At `org.eclipse.winery.repository` and ` org.eclipse.winery.topologymodeler`:
    * Right click -> Properties -> JavaScript -> Include Path -> Source -> Expand folder -> Select "Excluded" -> "Edit..."
    * Exclusion Patterns: Add multiple -> Select "3rd party" -> "OK"
    * Exclusion Patterns: Add multiple -> Select "components" -> "OK"
    * "Finish" -> "OK"

### Setup Tomcat

1. Open servers window: Window -> Show View -> Other -> Server -> Servers
2. New server wizard... -> Apache -> Tomcat v7.0 Server -> Next -> Winery -> Add -> Finish
3. Rename the Server to "Apache Tomcat v7.0"

Now you can see the Tomcat v7.0 Server at localhost [Stopped, Republish] in your server window.
Select it and click on the green play button in the window.

Now, Winery can be viewed at http://localhost:8080/winery-ui/

## Setup Code Style

Java -> Code Style -> Clean Up: cleanup.xml
Formatter -> formatter_settings.xml
Code Templates: codetemplates.xml

Java -> Editor -> Templates: java_editor_templates.xml

Save Actions: follow eclipse_save_actions*.png:

![](figures/eclipse_save_actions.png)
![](figures/eclipse_save_actions_1_code_organizing.png)
![](figures/eclipse_save_actions_2_code_style.png)
![](figures/eclipse_save_actions_3_member_accesses.png)
![](figures/eclipse_save_actions_4_missing_code.png)
![](figures/eclipse_save_actions_5_unnecessary_code.png)

## Further hints

The repository location can be changed:
Copy `winery.properties` to `path-to-workspace\.metadata\.plugins\org.eclipse.wst.server.core\tmp0\wtpwebapps\winery`.

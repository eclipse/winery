/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.model.adaptation.instance;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.version.VersionUtils;
import org.eclipse.winery.common.version.WineryVersion;
import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.constants.OpenToscaBaseTypes;
import org.eclipse.winery.model.tosca.constants.ToscaBaseTypes;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class InstanceModelUtils {

    public static final String vmUser = "VMUserName";
    public static final String vmPrivateKey = "VMPrivateKey";
    public static final String vmIP = "VMIP";
    public static final String vmSshPort = "VMSSHPort";

    public static final String dockerContainerIDProp = "ContainerID";
    public static final String dockerEngineURLProp = "DockerEngineURL";

    public static final String getInput = "get_input";

    public static final String stateProp = "State";
    public static final String stateRunning = "Running";

    private static final Logger logger = LoggerFactory.getLogger(InstanceModelUtils.class);

    @NotNull
    public static Set<String> getRequiredInputs(TTopologyTemplate template, ArrayList<String> nodeIdsToBeReplaced, Map<QName, TNodeType> nodeTypes) {
        Optional<TNodeTemplate> dockerContainer = getDockerContainer(template, nodeIdsToBeReplaced, nodeTypes);
        if (dockerContainer.isPresent()) {
            return InstanceModelUtils.getRequiredDockerTTYInputs(template, nodeIdsToBeReplaced);
        } else {
            return InstanceModelUtils.getRequiredSSHInputs(template, nodeIdsToBeReplaced);
        }
    }

    public static Set<String> getRequiredSSHInputs(TTopologyTemplate topology, List<String> nodeIdsToBeRefined) {
        return getMissingInputs(
            topology,
            nodeIdsToBeRefined,
            vmUser, vmPrivateKey, vmIP, vmSshPort
        );
    }

    public static Set<String> getMissingInputs(TTopologyTemplate topology, List<String> nodeIdsToBeRefined, String... props) {
        Set<String> inputs = new HashSet<>();
        Map<String, String> requiredInputs = getRequiredInputs(topology, nodeIdsToBeRefined, props);

        for (String prop : props) {
            String input = requiredInputs.get(prop);
            if (input == null || input.isBlank() || input.toLowerCase().startsWith(getInput)) {
                inputs.add(prop);
            }
        }

        return inputs;
    }

    public static Set<String> getRequiredDockerTTYInputs(TTopologyTemplate topology, List<String> nodeIdsToBeRefined) {
        return getMissingInputs(
            topology,
            nodeIdsToBeRefined,
            dockerContainerIDProp, dockerEngineURLProp
        );
    }

    public static Map<String, String> getDockerTTYInputs(TTopologyTemplate topology, List<String> nodeIdsToBeRefined) {
        return getRequiredInputs(
            topology,
            nodeIdsToBeRefined,
            dockerContainerIDProp, dockerEngineURLProp
        );
    }

    public static Map<String, String> getSSHCredentials(TTopologyTemplate topology, List<String> nodeIdsToBeRefined) {
        return getRequiredInputs(
            topology,
            nodeIdsToBeRefined,
            vmUser, vmPrivateKey, vmIP, vmSshPort
        );
    }

    private static Map<String, String> getRequiredInputs(TTopologyTemplate topology, List<String> nodeIdsToBeRefined, String... props) {
        Map<String, String> properties = new HashMap<>();

        topology.getNodeTemplates().stream()
            .filter(node -> nodeIdsToBeRefined.contains(node.getId()))
            .forEach(node -> {
                List<TNodeTemplate> hostedOnSuccessors = ModelUtilities.getHostedOnSuccessors(topology, node);
                hostedOnSuccessors.add(node);

                for (TNodeTemplate host : hostedOnSuccessors) {
                    if (host.getProperties() != null && host.getProperties() instanceof TEntityTemplate.WineryKVProperties) {
                        Map<String, String> kvProperties = ((TEntityTemplate.WineryKVProperties) host.getProperties())
                            .getKVProperties();
                        kvProperties.entrySet().stream()
                            .filter(entry -> Arrays.stream(props).anyMatch(property -> property.equalsIgnoreCase(entry.getKey())))
                            .forEach(entry -> properties.put(entry.getKey(), entry.getValue()));
                    }
                }
            });

        return properties;
    }

    public static Session createJschSession(TTopologyTemplate topology, List<String> nodeIdsToBeRefined) {
        Map<String, String> sshCredentials = getSSHCredentials(topology, nodeIdsToBeRefined);

        File key = null;
        try {
            JSch jsch = new JSch();
            key = Files.createTempFile(FileUtils.getTempDirectory().toPath(), "key", "tmp").toFile();
            FileUtils.write(key, sshCredentials.get(vmPrivateKey), "UTF-8");
            logger.info("tmp key file created: {}", key.exists());

            jsch.addIdentity(key.getAbsolutePath());
            Session session = sshCredentials.containsKey(vmSshPort)
                ? jsch.getSession(sshCredentials.get(vmUser), sshCredentials.get(vmIP), Integer.parseInt(sshCredentials.get(vmSshPort)))
                : jsch.getSession(sshCredentials.get(vmUser), sshCredentials.get(vmIP));
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            return session;
        } catch (JSchException | IOException e) {
            logger.error("Failed to connect to {} using user {}.", sshCredentials.get(vmIP), sshCredentials.get(vmUser), e);
            throw new RuntimeException(e);
        } finally {
            if (key != null) {
                try {
                    FileUtils.forceDelete(key);
                } catch (IOException e) {
                    logger.warn("Could not delete file...");
                }
                logger.info("tmp key file deleted: {}", !key.exists());
            }
        }
    }

    public static void setUserInputs(Map<String, String> userInputs, TTopologyTemplate topology, List<String> nodeIdsToBeRefined) {
        nodeIdsToBeRefined.forEach(nodeId -> {
            TNodeTemplate node = topology.getNodeTemplate(nodeId);
            List<TNodeTemplate> nodes = ModelUtilities.getHostedOnSuccessors(topology, node);
            nodes.add(node);

            nodes.forEach(nodeTemplate -> {
                if (nodeTemplate.getProperties() != null && nodeTemplate.getProperties() instanceof TEntityTemplate.WineryKVProperties) {
                    Map<String, String> kvProperties = ((TEntityTemplate.WineryKVProperties) nodeTemplate.getProperties()).getKVProperties();
                    Optional.ofNullable(userInputs).ifPresent(inputs -> inputs.forEach((key, value) -> {
                        if (kvProperties.containsKey(key)) {
                            kvProperties.put(key, value);
                        }
                    }));
                }
            });
        });
    }

    static String executeCommand(Session session, String command) {
        ChannelExec channelExec = null;
        try {
            logger.info("Executing script: \"{}\"", command);
            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(command);

            StringBuilder outputBuffer = new StringBuilder();
            StringBuilder errorBuffer = new StringBuilder();
            InputStream in = channelExec.getInputStream();
            InputStream err = channelExec.getExtInputStream();

            channelExec.connect();
            byte[] tmp = new byte[1024];
            int timer = 0;
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    outputBuffer.append(new String(tmp, 0, i));
                }
                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) break;
                    errorBuffer.append(new String(tmp, 0, i));
                }
                if (channelExec.isClosed() && in.available() == 0 && err.available() == 0) {
                    break;
                }
                if (timer++ % 5 == 0) {
                    logger.info("Still executing...");
                }
                //noinspection BusyWait
                Thread.sleep(1000);
            }

            if (!errorBuffer.toString().isEmpty()) {
                logger.error(errorBuffer.toString());
            }
            if (!outputBuffer.toString().isEmpty()) {
                logger.info(outputBuffer.toString());
            }

            logger.info("\"{}\" exited with code '{}'", command, channelExec.getExitStatus());

            return outputBuffer.toString().trim();
        } catch (JSchException | InterruptedException | IOException e) {
            throw new RuntimeException("Failed to execute command \"" + command + "\"");
        } finally {
            if (channelExec != null) {
                channelExec.disconnect();
            }
        }
    }

    public static List<String> executeCommands(TTopologyTemplate topology, List<String> nodeIdsToBeRefined,
                                               Map<QName, ? extends TEntityType> types, String... commands) {
        List<String> output = new ArrayList<>();

        // determine whether the host is a Docker Container or a VM
        Optional<TNodeTemplate> dockerContainer = getDockerContainer(topology, nodeIdsToBeRefined, types);

        if (dockerContainer.isPresent()) {
            Map<String, String> dockerTTYInputs = getDockerTTYInputs(topology, nodeIdsToBeRefined);

            try (DockerClient dockerClient = getDockerClient(dockerTTYInputs.get(dockerEngineURLProp))) {
                for (String command : commands) {
                    output.add(
                        executeDockerCommand(dockerClient, dockerTTYInputs.get(dockerContainerIDProp), command)
                    );
                }
            } catch (InterruptedException | IOException e) {
                logger.error("Execution was interrupted!", e);
            }
        } else {
            Session jschSession = createJschSession(topology, nodeIdsToBeRefined);
            for (String command : commands) {
                output.add(executeCommand(jschSession, "sudo " + command));
            }
            jschSession.disconnect();
        }

        return output;
    }

    public static String executeDockerCommand(DockerClient dockerClient, String containerId, String command) throws InterruptedException {
        logger.info("Executing command on container: {}", command);

        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
            .withTty(true)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .withCmd("/bin/sh", "-c", command)
            .exec();

        try (AttachContainerCallback callback = dockerClient.execStartCmd(execCreateCmdResponse.getId())
            .withTty(true)
            .withDetach(false)
            .exec(new AttachContainerCallback(containerId))) {
            callback.awaitCompletion();
            return callback.builder.toString();
        } catch (IOException e) {
            logger.error("Error while executing...", e);
        }

        return "";
    }

    public static String getDockerLogs(TTopologyTemplate topology, List<String> nodeIdsToBeRefined) {
        Map<String, String> dockerTTYInputs = getDockerTTYInputs(topology, nodeIdsToBeRefined);
        return getDockerLogs(dockerTTYInputs.get(dockerEngineURLProp), dockerTTYInputs.get(dockerContainerIDProp));
    }

    public static String getDockerLogs(String dockerEngineURL, String containerId) {
        DockerClient dockerClient = getDockerClient(dockerEngineURL);
        StringBuilder logs = new StringBuilder();

        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId);
        logContainerCmd.withStdOut(true).withStdErr(true);
        ;

        try {
            logContainerCmd.exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame item) {
                    logs.append(item.toString());
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            logger.error("Interrupted Exception!", e);
        } finally {
            try {
                dockerClient.close();
            } catch (IOException e) {
                logger.error("Error closing Docker connection", e);
            }
        }

        return logs.toString()
            .replaceFirst("STDOUT: ", "")
            .replaceAll("STDOUT: ", "\n");
    }

    private static DockerClient getDockerClient(String dockerEngine) {
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(dockerEngine)
            .withDockerTlsVerify(false);
        DefaultDockerClientConfig config = configBuilder.build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            // wait for infinity
            .responseTimeout(Duration.ZERO)
            .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    public static Optional<TNodeTemplate> getDockerContainer(TTopologyTemplate topology, List<String> nodeIdsToBeRefined, Map<QName, ? extends TEntityType> types) {
        return topology.getNodeTemplates().stream()
            .filter(node -> nodeIdsToBeRefined.contains(node.getId()))
            .flatMap(node -> {
                List<TNodeTemplate> hostedOnSuccessors = ModelUtilities.getHostedOnSuccessors(topology, node);
                hostedOnSuccessors.add(node);

                return hostedOnSuccessors.stream()
                    .filter(host -> ModelUtilities.isOfType(
                        OpenToscaBaseTypes.dockerContainerNodeType,
                        host.getType(),
                        types)
                    );
            })
            .distinct()
            .findFirst();
    }

    public static TNodeTemplate getOrAddNodeTemplateMatchingTypeAndHost(TTopologyTemplate topology, String hostId, QName requiredType,
                                                                        Map<QName, ? extends TEntityType> nodeTypes) {
        TNodeTemplate containerNode = topology.getNodeTemplate(hostId);
        ArrayList<TNodeTemplate> hostedOnPredecessors = ModelUtilities.getHostedOnPredecessors(topology, containerNode);
        Optional<TNodeTemplate> springWebApp = hostedOnPredecessors.stream()
            .filter(node -> ModelUtilities.isOfType(requiredType, node.getType(), nodeTypes))
            .findFirst();

        IRepository repo = RepositoryFactory.getRepository();

        TNodeType nodeType = repo.getElement(new NodeTypeId(requiredType));
        TNodeTemplate nodeTempalte = ModelUtilities.instantiateNodeTemplate(nodeType);
        setStateRunning(nodeTempalte);

        if (springWebApp.isPresent()) {
            nodeTempalte = springWebApp.get();
        } else {
            addNodeAsHostedOnSuccessor(topology, containerNode, nodeTempalte);
        }
        return nodeTempalte;
    }

    public static void addNodeAsHostedOnSuccessor(TTopologyTemplate topology, TNodeTemplate host, TNodeTemplate node) {
        TRelationshipTemplate relationshipTemplate = ModelUtilities.instantiateRelationshipTemplate(
                RepositoryFactory.getRepository().getElement(
                        new RelationshipTypeId(ToscaBaseTypes.hostedOnRelationshipType)
                ),
                node,
                host
        );
        node.setX(host.getX());
        node.setY(String.valueOf(Integer.parseInt(host.getY()) - 160));
        topology.addNodeTemplate(node);
        topology.addRelationshipTemplate(relationshipTemplate);
    }

    /**
     * Do fancy detection of a NodeType matching the given namespace and id while being as close as possible to the
     * given version. Additionally, it tries to identify the latest Version of the found version.
     *
     * @param namespace The namespace to search in
     * @param id        The ID of the required NodeType
     * @param version   The version to search for
     * @param nodeTypes The list of all NodeTypes in the current repository
     * @return The closest Node Type found
     */
    public static QName getClosestVersionMatchOfVersion(String namespace, String id, String version, Map<QName, TNodeType> nodeTypes) {
        String namespaceWithCurlies = namespace;
        if (!namespace.contains("{")) {
            namespaceWithCurlies = "{" + namespaceWithCurlies;
        }
        if (!namespace.contains("}")) {
            namespaceWithCurlies += "}";
        }

        // Backup type that may not exist...
        QName closestMatchToGivenVersion = QName.valueOf(namespaceWithCurlies + id + "_" + version + "-w1");
        List<WineryVersion> allVersionsOfThisType = nodeTypes.keySet().stream()
            .filter(type -> VersionUtils.getNameWithoutVersion(type.getLocalPart())
                .equals(VersionUtils.getNameWithoutVersion(id)))
            .map(type -> VersionUtils.getVersion(type.getLocalPart()))
            .toList();

        for (int[] index = {version.length() - 1}; index[0] > 0; index[0]--) {
            if (version.charAt(index[0]) == '.') {
                List<WineryVersion> list = allVersionsOfThisType.stream()
                    .filter(type -> type.getComponentVersion().equals(version.substring(0, index[0])))
                    .sorted(WineryVersion::compareTo)
                    .toList();
                if (list.size() > 0) {
                    // since we search for the most concrete version first, we now return
                    return QName.valueOf(namespaceWithCurlies + id + "_" + list.get(list.size() - 1).toString());
                }
            }
        }

        return closestMatchToGivenVersion;
    }

    public static void setStateRunning(TNodeTemplate nodeTemplate) {
        TEntityTemplate.Properties dockerEngineProps = nodeTemplate.getProperties();
        if (dockerEngineProps == null) {
            dockerEngineProps = new TEntityTemplate.WineryKVProperties();
            nodeTemplate.setProperties(dockerEngineProps);
        }
        if (dockerEngineProps instanceof TEntityTemplate.WineryKVProperties props) {
            props.getKVProperties().put(stateProp, stateRunning);
        }
    }

    public static class AttachContainerCallback extends ResultCallback.Adapter<Frame> {

        StringBuilder builder = new StringBuilder();
        private final String containerId;

        public AttachContainerCallback(String containerId) {
            this.containerId = containerId;
        }

        @Override
        public void onNext(Frame frame) {
            String result = new String(frame.getPayload());

            // There are linebreaks, we just print everything. Otherwise, works might get ripped apart.
            if (logger.isDebugEnabled()) {
                System.out.print(result);
            }
            builder.append(result);

            super.onNext(frame);
        }

        @Override
        public void onComplete() {
            super.onComplete();
            logger.info("Connection to container \"{}\" is completed!", this.containerId);
        }

        @Override
        public void onError(Throwable throwable) {
            super.onError(throwable);
            logger.error("An error appeared...", throwable);
        }
    }
}

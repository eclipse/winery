/*******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.repository.targetallocation.criteria.minexternalconnections;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.slf4j.LoggerFactory;

/**
 * For the distribution of target labels of a topology adapted implementation of
 * the randomized Karger Minimum Cut algorithm.
 * Uses {@link ConnectsToGraph} to clone topologies with less overhead.
 * To compute the minimum cut with high probability, the algorithm is executed n^2 * log(n) times.
 */
public class KargerMinCutVariation {

    private static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(KargerMinCutVariation.class);

    private ConnectsToGraph original;
    private ConnectsToGraph current;

    private int repetitions;
    private Random random = new SecureRandom();

    public KargerMinCutVariation(ConnectsToGraph graph) {
        original = graph;
        int n = graph.getNodes().size();
        // -> high probability of finding all min cuts
        repetitions = (int) (Math.pow(n, 2) * Math.log(n));
    }

    public Set<ConnectsToGraph> computeTargetLabelPartitions() {
        Set<ConnectsToGraph> best = new HashSet<>();
        int bestMinCut = Integer.MAX_VALUE;
        int currentMinCut;

        for (int i = 0; i < repetitions; i++) {
            current = new ConnectsToGraph(original);
            minCut();
            currentMinCut = current.getEdges().size();

            if (currentMinCut < bestMinCut) {
                bestMinCut = currentMinCut;
                best.clear();
                best.add(current);
            } else if (currentMinCut == bestMinCut) {
                best.add(current);
            }
        }
        LOGGER.debug("Minimum cut of " + bestMinCut +
            " found for " + best.size() + " possibilities in " +
            repetitions + " repetitions");
        return best;
    }

    private void minCut() {
        while (!current.getEdgesForContraction().isEmpty()) {
            ConnectsToGraph.Edge edge = getRandomEdge();
            contract(edge);
        }
    }

    private ConnectsToGraph.Edge getRandomEdge() {
        int index = random.nextInt(current.getEdgesForContraction().size());
        return current.getEdgesForContraction().get(index);
    }

    private void contract(ConnectsToGraph.Edge edgeToContract) {
        ConnectsToGraph.Node source = edgeToContract.getSource();
        ConnectsToGraph.Node target = edgeToContract.getTarget();

        if (source.getTargetLabel().equals("")) {
            merge(target, source);
        } else {
            merge(source, target);
        }
    }

    private void merge(ConnectsToGraph.Node nodeToMergeInto, ConnectsToGraph.Node nodeToMerge) {
        reassignEdges(nodeToMergeInto, nodeToMerge);
        nodeToMergeInto.addNodeTemplateIds(nodeToMerge.getNodeTemplateIds());
        current.getNodes().remove(nodeToMerge);
    }

    private void reassignEdges(ConnectsToGraph.Node nodeToMergeInto, ConnectsToGraph.Node nodeToMerge) {
        Iterator<ConnectsToGraph.Edge> iterator = current.getEdges().iterator();
        ConnectsToGraph.Edge next;

        current.getEdgesForContraction().clear();
        while (iterator.hasNext()) {
            next = iterator.next();
            // delete edges between merged nodes
            if ((next.getSource().equals(nodeToMergeInto) && next.getTarget().equals(nodeToMerge)) ||
                (next.getSource().equals(nodeToMerge) && next.getTarget().equals(nodeToMergeInto))) {
                iterator.remove();
                continue;
                // reassign other edges
            } else if (next.getTarget().equals(nodeToMerge)) {
                next.setTarget(nodeToMergeInto);
            } else if (next.getSource().equals(nodeToMerge)) {
                next.setSource(nodeToMergeInto);
            }

            if (next.isContractible()) {
                current.getEdgesForContraction().add(next);
            }
        }
    }
}

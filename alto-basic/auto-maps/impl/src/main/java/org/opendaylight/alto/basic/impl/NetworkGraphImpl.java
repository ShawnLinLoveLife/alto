/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.alto.basic.impl;

import com.google.common.base.Preconditions;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkGraphImpl {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkGraphImpl.class);

    Graph<NodeId, Link> networkGraph = null;
    Set<String> linkAdded = new HashSet<>();

    DijkstraShortestPath<NodeId, Link> shortestPath = null;

    /**
     * Adds links to existing graph or creates new directed graph with given
     * links if graph was not initialized.
     *
     * @param links
     *            The links to add.
     */
    public synchronized void addLinks(List<Link> links) {
        if (links == null || links.isEmpty()) {
            LOG.info("In addLinks: No link added as links is null or empty.");
            return;
        }

        if (networkGraph == null) {
            networkGraph = new SparseMultigraph<>();
        }

        for (Link link : links) {
            if (linkAlreadyAdded(link)) {
                continue;
            }
            NodeId sourceNodeId = link.getSource().getSourceNode();
            NodeId destinationNodeId = link.getDestination().getDestNode();
            networkGraph.addVertex(sourceNodeId);
            networkGraph.addVertex(destinationNodeId);
            networkGraph.addEdge(link, sourceNodeId, destinationNodeId, EdgeType.UNDIRECTED);
        }

        if(shortestPath == null) { shortestPath = new
                DijkstraShortestPath<>(networkGraph); } else { shortestPath.reset();
        }

    }

    private boolean linkAlreadyAdded(Link link) {
        String linkAddedKey = null;
        if (link.getDestination().getDestTp().hashCode() > link.getSource().getSourceTp().hashCode()) {
            linkAddedKey = link.getSource().getSourceTp().getValue() + link.getDestination().getDestTp().getValue();
        } else {
            linkAddedKey = link.getDestination().getDestTp().getValue() + link.getSource().getSourceTp().getValue();
        }
        if (linkAdded.contains(linkAddedKey)) {
            return true;
        } else {
            linkAdded.add(linkAddedKey);
            return false;
        }
    }

    /**
     * Removes links from existing graph.
     *
     * @param links
     *            The links to remove.
     */
    public synchronized void removeLinks(List<Link> links) {
        Preconditions.checkNotNull(networkGraph, "Graph is not initialized, add links first.");

        if (links == null || links.isEmpty()) {
            LOG.info("In removeLinks: No link removed as links is null or empty.");
            return;
        }

        for (Link link : links) {
            networkGraph.removeEdge(link);
        }

         if(shortestPath == null) {
            shortestPath = new DijkstraShortestPath<>(networkGraph); } else { shortestPath.reset();
         }


    }

    /**
     * returns a path between 2 nodes. Uses Dijkstra's algorithm to return
     * shortest path.
     *
     * @param sourceNodeId
     * @param destinationNodeId
     * @return
     */


     public synchronized List<Link> getPath(NodeId sourceNodeId, NodeId destinationNodeId) {
         Preconditions.checkNotNull(shortestPath, "Graph is not initialized, add links first.");
         if(sourceNodeId == null || destinationNodeId == null) {
             LOG.info("In getPath: returning null, as sourceNodeId or destinationNodeId is null.");
             return null;
         }
         return shortestPath.getPath(sourceNodeId, destinationNodeId);
     }


    /**
     * Clears the prebuilt graph, in case same service instance is required to
     * process a new graph.
     */
    public synchronized void clear() {
        networkGraph = null;
        linkAdded.clear();
        shortestPath = null;
    }

    /**
     * Get all the links in the network.
     *
     * @return The links in the network.
     */
    public List<Link> getAllLinks() {
        List<Link> allLinks = new ArrayList<>();
        if (networkGraph != null) {
            allLinks.addAll(networkGraph.getEdges());
        }
        return allLinks;
    }
}

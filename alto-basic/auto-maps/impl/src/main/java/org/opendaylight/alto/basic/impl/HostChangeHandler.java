/**
 * Copyright (c) 2014 André Martins, Colin Dixon, Evan Zeller and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.alto.basic.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rev150105.config.network.map.records.ConfigNetworkMapRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rev150105.config.network.map.records.config.network.map.record.network.map.type.AnchorBased;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class HostChangeHandler implements DataChangeListener {

    private static final int CPUS = Runtime.getRuntime().availableProcessors();
    private AltoAutoMapsProvider provider;
    private AltoAutoMapsReader reader;

    /**
     * As defined on
     * controller/opendaylight/md-sal/topology-manager/src/main/java/org/opendaylight/md/controller/topology/manager/FlowCapableTopologyProvider.java
     */
    private static final String TOPOLOGY_NAME = "flow:1";

    private static final Logger LOG = LoggerFactory.getLogger(HostChangeHandler.class);

    private final DataBroker dataService;
    private final String topologyId = "flow:1";


    private ScheduledExecutorService exec = Executors.newScheduledThreadPool(CPUS);

    private ListenerRegistration<DataChangeListener> addrsNodeListerRegistration;
    private ListenerRegistration<DataChangeListener> hostNodeListerRegistration;

    /**
     * It creates hosts using reference to MD-SAl / toplogy module. For every hostPurgeIntervalInput time interval
     * it requests to purge hosts that are not seen for hostPurgeAgeInput time interval.
     *
     * @param dataService A reference to the MD-SAL
     */
    public HostChangeHandler(final DataBroker dataService, AltoAutoMapsProvider altoAutoMapsProvider, AltoAutoMapsReader reader) {
        Preconditions.checkNotNull(dataService, "dataBrokerService should not be null.");
        this.dataService = dataService;
        this.provider = altoAutoMapsProvider;
        this.reader = reader;
    }

    public void init() {
        InstanceIdentifier<Addresses> addrCapableNodeConnectors = //
                InstanceIdentifier.builder(Nodes.class) //
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class) //
                        .child(NodeConnector.class) //
                        .augmentation(AddressCapableNodeConnector.class)//
                        .child(Addresses.class).build();
        this.addrsNodeListerRegistration = dataService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, addrCapableNodeConnectors, this, DataChangeScope.SUBTREE);

        InstanceIdentifier<HostNode> hostNodes = InstanceIdentifier.builder(NetworkTopology.class)//
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))//
                .child(Node.class)
                .augmentation(HostNode.class).build();
        this.hostNodeListerRegistration = dataService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, hostNodes, this, DataChangeScope.SUBTREE);

        InstanceIdentifier<Link> lIID = InstanceIdentifier.builder(NetworkTopology.class)//
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))//
                .child(Link.class).build();

        this.addrsNodeListerRegistration = dataService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, lIID, this, DataChangeScope.BASE);
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        exec.submit(new Runnable() {
            @Override
            public void run() {
                if (change == null) {
                    LOG.info("In onDataChanged: No processing done as change even is null.");
                    return;
                }
                Map<InstanceIdentifier<?>, DataObject> updatedData = change.getUpdatedData();
                Map<InstanceIdentifier<?>, DataObject> createdData = change.getCreatedData();
                Map<InstanceIdentifier<?>, DataObject> originalData = change.getOriginalData();
                Set<InstanceIdentifier<?>> deletedData = change.getRemovedPaths();

                for (InstanceIdentifier<?> iid : deletedData) {
                    if (iid.getTargetType().equals(Node.class)) {
                        Node node = ((Node) originalData.get(iid));
                        InstanceIdentifier<Node> iiN = (InstanceIdentifier<Node>) iid;
                        HostNode hostNode = node.getAugmentation(HostNode.class);
                        if (hostNode != null) {
                            //synchronized (hosts) {
                                try {
                                    //hosts.removeLocally(iiN);
                                    LOG.info("a host disappear~~~");
                                    refreshExistedNetworkmap();
                                } catch (ClassCastException ex) {
                                    LOG.debug("Exception occurred while remove host locally", ex);
                                }
                            //}
                        }
                    } else if (iid.getTargetType().equals(Link.class)) {
                        // TODO performance improvement here
                        InstanceIdentifier<Link> iiL = (InstanceIdentifier<Link>) iid;
                        //synchronized (links) {
                            try {
                                //links.removeLocally(iiL);
                                LOG.info("a link disappear~~~~");
                                refreshExistedNetworkmap();
                            } catch (ClassCastException ex) {
                                LOG.debug("Exception occurred while remove link locally", ex);
                            }
                        //}
                        //linkRemoved((InstanceIdentifier<Link>) iid, (Link) originalData.get(iid));
                    }
                }

                for (Map.Entry<InstanceIdentifier<?>, DataObject> entrySet : updatedData.entrySet()) {
                    InstanceIdentifier<?> iiD = entrySet.getKey();
                    final DataObject dataObject = entrySet.getValue();
                    if (dataObject instanceof Addresses) {
                        //packetReceived((Addresses) dataObject, iiD);
                    } else if (dataObject instanceof Node) {
                        //synchronized (hosts) {
                            LOG.info("A Address update");
                            refreshExistedNetworkmap();
                        //hosts.putLocally((InstanceIdentifier<Node>) iiD, Host.createHost((Node) dataObject));
                        //}
                    } else if (dataObject instanceof Link) {
                        //synchronized (links) {
                            LOG.info("A link update");
                            refreshExistedNetworkmap();
                        //links.putLocally((InstanceIdentifier<Link>) iiD, (Link) dataObject);
                        //}
                    }
                }

                for (Map.Entry<InstanceIdentifier<?>, DataObject> entrySet : createdData.entrySet()) {
                    InstanceIdentifier<?> iiD = entrySet.getKey();
                    final DataObject dataObject = entrySet.getValue();
                    if (dataObject instanceof Addresses) {
                        //packetReceived((Addresses) dataObject, iiD);
                    } else if (dataObject instanceof Node) {
                       // synchronized (hosts) {
                            //hosts.putLocally((InstanceIdentifier<Node>) iiD, Host.createHost((Node) dataObject));
                            LOG.info("An Address created");
                            refreshExistedNetworkmap();
                        //}
                    } else if (dataObject instanceof Link) {
                        //synchronized (links) {
                            //links.putLocally((InstanceIdentifier<Link>) iiD, (Link) dataObject);
                            LOG.info("An link created");
                            refreshExistedNetworkmap();
                        //}
                    }
                }
            }
        });
    }

    private void refreshExistedNetworkmap() {
        LOG.info("need update existed networkmap!!!");
        List<ConfigNetworkMapRecord> configNetworkMapRecordList = this.reader.readResourceNetworkMapListFromConfigRecord();
        if (configNetworkMapRecordList != null) {
            for (ConfigNetworkMapRecord record: configNetworkMapRecordList) {
                LOG.info("Network Map " + record.getResourceId().getValue() + "need to be updated");
                this.provider.generateNetworkMap(record.getResourceId().getValue(), ((AnchorBased)record.getNetworkMapType()).getAnchors(), true);
            }
        }
    }

    public void close() {
        this.addrsNodeListerRegistration.close();
        this.hostNodeListerRegistration.close();
        this.exec.shutdownNow();
    }
}

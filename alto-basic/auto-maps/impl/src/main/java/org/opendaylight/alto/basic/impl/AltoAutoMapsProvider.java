/*
 * Copyright © 2017 SNLab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.alto.basic.impl;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.alto.types.rev150921.PidName;
import org.opendaylight.yang.gen.v1.urn.alto.types.rev150921.ResourceId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AltoAutoMapsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AltoAutoMapsProvider.class);

    private final DataBroker dataBroker;
    private final String DEFAULT_TOPOLOGY_ID = "flow:1";
    private static final long DEFAULT_GRAPH_REFRESH_DELAY = 1000;
    private static final String DEFAULT_HOST_LABEL = "host:";
    private TopologyLinkDataChangeHandler topologyLinkDataChangeHandler;
    private Registration listenerRegistration = null, topoNodeListnerReg = null;
    public NetworkGraphImpl ngi;
    public AltoAutoMapsReader reader;
    public AltoAutoMapsWriter writer;
    private HostChangeHandler hostChangeHandler;

    private static final String DEFAULT_CONTEXT_ID = "00000000-0000-0000-0000-000000000000";
    public AltoAutoMapsProvider(final DataBroker dataBroker, final NotificationService ns) {
        this.dataBroker = dataBroker;
        //ns.registerNotificationListener(this);
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        this.reader = new AltoAutoMapsReader(this.dataBroker);
        this.writer = new AltoAutoMapsWriter(this.dataBroker);
        ngi = new NetworkGraphImpl();
        this.topologyLinkDataChangeHandler = new TopologyLinkDataChangeHandler(dataBroker, ngi, this.reader, this.writer, this);
        this.topologyLinkDataChangeHandler.setTopologyId(DEFAULT_TOPOLOGY_ID);
        this.topologyLinkDataChangeHandler.setGraphRefreshDelay(DEFAULT_GRAPH_REFRESH_DELAY);
        this.listenerRegistration = topologyLinkDataChangeHandler.registerAsDataChangeListener();
        this.hostChangeHandler = new HostChangeHandler(dataBroker, this, this.reader);
        this.hostChangeHandler.init();
        //generateDefaultPID();
        LOG.info("AltoAutoMapsProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() throws Exception {
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
        LOG.info("AltoAutoMapsProvider Closed");
    }

    /**
     * Generate a network map based on the given anchors.
     *
     * @param anchors The list of every PID's center.
     */
    public void generateNetworkMap(String rid, List<PidName> anchors, boolean isUpdate) {
        //TOOD: Check the resource named rid existed or not
        Map<PidName, List<String>> networkmap = new HashMap<>();
        for (int i = 0; i < anchors.size(); ++i) {
            networkmap.put(anchors.get(i), new LinkedList<>());
        }
        Topology topology = getTopology();
        for (Node node : topology.getNode()) {
            //System.out.println("NodeId: " + node.getNodeId().getValue());
            //System.out.println("NodeKey: " + node.getKey().getNodeId().getValue());
            if (node.getNodeId().getValue().contains(DEFAULT_HOST_LABEL)) {
                System.out.println("Find a host: " + node.getKey().getNodeId().getValue());
                HostNode hostNode = node.getAugmentation(HostNode.class);
                List<Addresses> addressesList = hostNode.getAddresses();
                int min = Integer.MAX_VALUE;
                for (Addresses address : addressesList) {
                    int noPathCount = 0;
                    //System.out.println(address.getIp().getIpv4Address().getValue());
                    //Get nodeId from tpId
                    String nodeId = hostNode.getAttachmentPoints().get(0).getTpId().getValue();
                    nodeId = nodeId.substring(0, nodeId.lastIndexOf(":"));
                    String ipAddress = address.getIp().getIpv4Address().getValue() + "/32";
                    System.out.println(address.getIp().getIpv4Address().getValue() + "===" + nodeId);
                    min = Integer.MAX_VALUE;
                    //String belong_to_anchor = anchors.get(0).getValue();
                    PidName belong_to_anchor = anchors.get(0);
                    for (PidName anchor : anchors) {
                        List<Link> path;
                        int hopcount = Integer.MAX_VALUE;
                        if (anchor.getValue().equals(nodeId)) {
                            min = 0;
                            belong_to_anchor = anchor;
                            continue;
                        }
                        try {
                            path = ngi.getPath(new NodeId(anchor.getValue()), new NodeId(nodeId));
                        } catch (IllegalArgumentException exception) {
                            continue;
                        }
                        if (path != null) {
                            hopcount = path.size();
                            LOG.info(anchor.getValue() + "-->" + nodeId + ":" + hopcount);
                            if (hopcount == 0) {
                                ++noPathCount;
                            }
                        } else {
                            ++noPathCount;
                        }

                        if (min > hopcount && hopcount!=0) {
                            min = hopcount;
                            belong_to_anchor = anchor;
                        }
                    }
                    LOG.info("The null path number for " + nodeId + " is " + String.valueOf(noPathCount));
                    if (noPathCount < anchors.size() && min!=Integer.MAX_VALUE) {
                        LOG.info(nodeId+":"+String.valueOf(noPathCount));
                        networkmap.get(belong_to_anchor).add(ipAddress);
                    }
                }
            }
        }

        //this is for test
        for (PidName anchor : networkmap.keySet()) {
            System.out.print("PID=" + anchor.getValue());
            for (String address : networkmap.get(anchor)) {
                System.out.print(", " + address);
            }
            System.out.println();
        }
        //
        // writeNetworkMap(rid, networkmap);
        this.writer.writeNetworkMap(new ResourceId(rid), networkmap, DEFAULT_CONTEXT_ID, isUpdate);
        this.writer.writeNetworkMapRecord(new ResourceId(rid), anchors);
    }

/*    private void writeNetworkMap(String rid, Map<String, List<String>> map) {
        List<org.opendaylight.yang.gen.v1.urn.alto
                .manual.maps.networkmap.rev151021.network.map.Map> networkMap = new LinkedList<>();
        org.opendaylight.yang.gen.v1.urn.alto
                .manual.maps.networkmap.rev151021.network.map.MapBuilder builder =
                new org.opendaylight.yang.gen.v1.urn.alto
                        .manual.maps.networkmap.rev151021.network.map.MapBuilder();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            List<EndpointAddressGroup> emptyEndpointAddressGroup = new LinkedList<>();
            emptyEndpointAddressGroup.add(new EndpointAddressGroupBuilder()
                    .setAddressType(new EndpointAddressType(EndpointAddressType.Enumeration.Ipv4))
                    .setEndpointPrefix(toIpPrefix(entry.getValue()))
                    .build());
            builder.setPid(new PidName(entry.getKey()));
            builder.setEndpointAddressGroup(emptyEndpointAddressGroup);
            networkMap.add(builder.build());
        }
        final WriteTransaction wx = dataBroker.newWriteOnlyTransaction();
        LOG.info("Begin to write network map into the DataStore");
        ManualMapsUtils.createResourceNetworkMap(rid, networkMap, wx);
        wx.submit();
        LOG.info("Write network map end");
    }*/

/*    private void writeNetworkMapRecord(ResourceId rid, List<PidName> anchors) {
        InstanceIdentifier<ConfigNetworkMapRecord> configNetworkMapRecordIID = InstanceIdentifier.builder(ConfigNetworkMapRecord.class, new ConfigNetworkMapRecordKey(rid)).build();
        ConfigNetworkMapRecordBuilder cnmrBuilder = new ConfigNetworkMapRecordBuilder();
        cnmrBuilder.setKey(new ConfigNetworkMapRecordKey(rid));
        cnmrBuilder.setResourceId(rid);
        AnchorBasedBuilder abBuilder = new AnchorBasedBuilder();
        abBuilder.setAnchors(anchors);
        cnmrBuilder.setNetworkMapType(abBuilder.build());
        final WriteTransaction wx = dataBroker.newWriteOnlyTransaction();
        wx.merge(LogicalDatastoreType.CONFIGURATION, configNetworkMapRecordIID, cnmrBuilder.build());
        wx.submit();
    }

    private void writeNetworkMap(ResourceId rid, Map<PidName, List<String>> map) {
        LOG.info("I will write something into manual map");
        InstanceIdentifier<ConfigContext> configContextIID = InstanceIdentifier.builder(ConfigContext.class, new ConfigContextKey(new Uuid(DEFAULT_CONTEXT_ID))).build();
        ConfigContextBuilder configContextBuilder = new ConfigContextBuilder();
        configContextBuilder.setContextId(new Uuid(DEFAULT_CONTEXT_ID));
        configContextBuilder.setKey(new ConfigContextKey(new Uuid(DEFAULT_CONTEXT_ID)));

        //Construct networkmap
        List<ResourceNetworkMap> networkMapList = new LinkedList<ResourceNetworkMap>();

        ResourceNetworkMapBuilder resourceNetworkmapBuilder = new ResourceNetworkMapBuilder();
        resourceNetworkmapBuilder.setResourceId(rid);
        resourceNetworkmapBuilder.setKey(new ResourceNetworkMapKey(rid));
        List<org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.network.map.Map> mapList =
                new LinkedList<org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.network.map.Map>();
        //Construct a map
        MapBuilder mapBuilder = new MapBuilder();
        for (Map.Entry<PidName, List<String>> item: map.entrySet()) {
            mapBuilder.setPid(item.getKey());
            mapBuilder.setKey(new MapKey(item.getKey()));
            //Construct endpoint address group
            List<EndpointAddressGroup> endpointAddressGroupList = new LinkedList<>();
            EndpointAddressGroupBuilder endpointAddressgroupBuilder = new EndpointAddressGroupBuilder();
            EndpointAddressType ipv4Type = new EndpointAddressType(EndpointAddressType.Enumeration.Ipv4);
            endpointAddressgroupBuilder.setAddressType(ipv4Type);
            endpointAddressgroupBuilder.setKey(new EndpointAddressGroupKey(ipv4Type));
            List<IpPrefix> ipPrefixList = new LinkedList<>();
            for (String iitem: item.getValue()) {
                ipPrefixList.add(new IpPrefix(new Ipv4Prefix(iitem)));
            }
            endpointAddressgroupBuilder.setEndpointPrefix(ipPrefixList);
            endpointAddressGroupList.add(endpointAddressgroupBuilder.build());
            mapList.add(mapBuilder.setEndpointAddressGroup(endpointAddressGroupList).build());
        }
        //Add the default PID
        mapList.add(defaultPID);
        resourceNetworkmapBuilder.setMap(mapList);
        //TODO genrate random tag
        resourceNetworkmapBuilder.setTag(new Tag(DigestUtils.md5Hex(rid.getValue()+mapList.toString())));
        networkMapList.add(resourceNetworkmapBuilder.build());
        configContextBuilder.setResourceNetworkMap(networkMapList);
        configContextBuilder.setResourceNetworkMap(networkMapList);
        LOG.info(configContextBuilder.build().toString());
        final WriteTransaction wx = dataBroker.newWriteOnlyTransaction();
        wx.merge(LogicalDatastoreType.CONFIGURATION, configContextIID, configContextBuilder.build());
        wx.submit();
    }*/

    private List<IpPrefix> toIpPrefix(List<String> stringList) {
        List<IpPrefix> result = new LinkedList<IpPrefix>();
        for (String element : stringList) {
            result.add(new IpPrefix(new Ipv4Prefix(element)));
        }
        return result;
    }

    private Topology getTopology() {
        try {
            ReadOnlyTransaction readTx = this.dataBroker.newReadOnlyTransaction();

            InstanceIdentifier<Topology> topologyInstanceIdentifier = InstanceIdentifier
                    .builder(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId("flow:1")))
                    .build();

            Optional<Topology> dataFuture = readTx.read(LogicalDatastoreType.OPERATIONAL,
                    topologyInstanceIdentifier).get();

            return dataFuture.get();
        } catch (Exception e) {
            LOG.info("Exception occurs when get topology: " + e.getMessage());
        }
        return null;
    }

/*    private static org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.network.map.Map defaultPID = null;
    private static final String DEFAULT_PID = "DEFAULT";
    private static final String DEFAULT_IPv4 = "0.0.0.0/0";
    private static final String DEFAULT_IPv6 = "::/0";
    private void generateDefaultPID() {
        if (defaultPID!= null) {
            return;
        } else {
*//*            List<org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.network.map.Map> mapList =
                    new LinkedList<org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.network.map.Map>();*//*
            MapBuilder mapBuilder = new MapBuilder();
            mapBuilder.setPid(new PidName(DEFAULT_PID));
            List<EndpointAddressGroup> endpointAddressGroupList = new LinkedList<>();
            EndpointAddressGroupBuilder endpointAddressgroupBuilder = new EndpointAddressGroupBuilder();
            EndpointAddressType ipv4Type = new EndpointAddressType(EndpointAddressType.Enumeration.Ipv4);
            endpointAddressgroupBuilder.setAddressType(ipv4Type);
            endpointAddressgroupBuilder.setKey(new EndpointAddressGroupKey(ipv4Type));
            List<IpPrefix> ipPrefixList = new LinkedList<>();
            ipPrefixList.add(new IpPrefix(new Ipv4Prefix(DEFAULT_IPv4)));
            endpointAddressgroupBuilder.setEndpointPrefix(ipPrefixList);
            endpointAddressGroupList.add(endpointAddressgroupBuilder.build());

            EndpointAddressType ipv6Type = new EndpointAddressType(EndpointAddressType.Enumeration.Ipv6);
            endpointAddressgroupBuilder.setAddressType(ipv6Type);
            endpointAddressgroupBuilder.setKey(new EndpointAddressGroupKey(ipv6Type));
            ipPrefixList = new LinkedList<>();
            ipPrefixList.add(new IpPrefix(new Ipv6Prefix(DEFAULT_IPv6)));
            endpointAddressgroupBuilder.setEndpointPrefix(ipPrefixList);
            endpointAddressGroupList.add(endpointAddressgroupBuilder.build());
            //mapList.add(mapBuilder.setEndpointAddressGroup(endpointAddressGroupList).build());
            mapBuilder.setEndpointAddressGroup(endpointAddressGroupList);
            defaultPID = mapBuilder.build();
        }
    }*/
}
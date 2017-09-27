/*
 * Copyright © 2017 SNLab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.alto.basic.impl;

import org.apache.commons.codec.digest.DigestUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.EndpointAddressType;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.endpoint.address.group.EndpointAddressGroup;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.endpoint.address.group.EndpointAddressGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.endpoint.address.group.EndpointAddressGroupKey;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.network.map.MapBuilder;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.network.map.MapKey;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.rev151021.ConfigContext;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.rev151021.ConfigContextBuilder;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.rev151021.ConfigContextKey;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.rev151021.config.context.ResourceNetworkMap;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.rev151021.config.context.ResourceNetworkMapBuilder;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.rev151021.config.context.ResourceNetworkMapKey;
import org.opendaylight.yang.gen.v1.urn.alto.types.rev150921.PidName;
import org.opendaylight.yang.gen.v1.urn.alto.types.rev150921.ResourceId;
import org.opendaylight.yang.gen.v1.urn.alto.types.rev150921.Tag;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rev150105.ConfigNetworkMapRecords;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rev150105.config.network.map.records.ConfigNetworkMapRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rev150105.config.network.map.records.ConfigNetworkMapRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rev150105.config.network.map.records.ConfigNetworkMapRecordKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rev150105.config.network.map.records.config.network.map.record.network.map.type.AnchorBasedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by shawn on 14/04/2017.
 */
public class AltoAutoMapsWriter {

    private DataBroker dataBroker;
    private static final Logger LOG = LoggerFactory.getLogger(AltoAutoMapsWriter.class);
    private static org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.network.map.Map defaultPID = null;
    private static final String DEFAULT_PID = "DEFAULT";
    private static final String DEFAULT_IPv4 = "0.0.0.0/0";
    private static final String DEFAULT_IPv6 = "::/0";

    public AltoAutoMapsWriter(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        generateDefaultPID();
    }

    public void writeNetworkMapRecord(ResourceId rid, List<PidName> anchors) {
        InstanceIdentifier<ConfigNetworkMapRecord> configNetworkMapRecordIID = InstanceIdentifier
                .create(ConfigNetworkMapRecords.class)
                .child(ConfigNetworkMapRecord.class, new ConfigNetworkMapRecordKey(rid));
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

    public void writeNetworkMap(ResourceId rid, Map<PidName, List<String>> map, String s_ContextId, boolean update) {
        LOG.info("I will write something into manual map");
        InstanceIdentifier<ConfigContext> configContextIID = InstanceIdentifier.builder(ConfigContext.class, new ConfigContextKey(new Uuid(s_ContextId))).build();
        ConfigContextBuilder configContextBuilder = new ConfigContextBuilder();
        configContextBuilder.setContextId(new Uuid(s_ContextId));
        configContextBuilder.setKey(new ConfigContextKey(new Uuid(s_ContextId)));

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
        if (update) {
            wx.delete(LogicalDatastoreType.CONFIGURATION, configContextIID.child(ResourceNetworkMap.class, new ResourceNetworkMapKey(rid)));
        }
        wx.merge(LogicalDatastoreType.CONFIGURATION, configContextIID, configContextBuilder.build());
        wx.submit();
    }

    private void generateDefaultPID() {
        if (defaultPID!= null) {
            return;
        } else {
/*            List<org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.network.map.Map> mapList =
                    new LinkedList<org.opendaylight.yang.gen.v1.urn.alto.manual.maps.networkmap.rev151021.network.map.Map>();*/
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
    }
}

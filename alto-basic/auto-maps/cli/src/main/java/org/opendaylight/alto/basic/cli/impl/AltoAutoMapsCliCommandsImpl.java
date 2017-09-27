/*
 * Copyright © 2017 SNLab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.alto.basic.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.urn.alto.types.rev150921.PidName;
import org.opendaylight.yang.gen.v1.urn.alto.types.rev150921.ResourceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rpc.rev150105.AltoAutoMapsRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rpc.rev150105.ConfigNetworkMapInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rpc.rev150105.config.network.map.input.network.map.type.AnchorBasedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.alto.basic.cli.api.AltoAutoMapsCliCommands;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AltoAutoMapsCliCommandsImpl implements AltoAutoMapsCliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(AltoAutoMapsCliCommandsImpl.class);
    private final DataBroker dataBroker;
    private final AltoAutoMapsRpcService aamrs;
    private final String ANCHOR_BASED_NETWORKMAP="-a";

    public AltoAutoMapsCliCommandsImpl(final DataBroker db, final AltoAutoMapsRpcService aamrs) {
        this.dataBroker = db;
        this.aamrs = aamrs;
        LOG.info("AltoAutoMapsCliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }

    @Override
    public Object generateNetworkMap(String resourceId, String type, Map<Object, List<String>> options) {
        System.out.println("generateNetworkmap " + options.get(ANCHOR_BASED_NETWORKMAP).get(0));
        ResourceId rid = new ResourceId(resourceId);
        List<PidName> pidNameList = new LinkedList<>();
        for (String item: options.get(ANCHOR_BASED_NETWORKMAP)) {
            pidNameList.add(new PidName(item));
        }
        AnchorBasedBuilder abb = new AnchorBasedBuilder();
        abb.setAnchors(pidNameList);


        ConfigNetworkMapInputBuilder cnmib = new ConfigNetworkMapInputBuilder()
                .setNetworkMapType(abb.build())
                .setResourceId(rid);

        Object result = null;
        try {
            result = aamrs.configNetworkMap(cnmib.build()).get().getResult().getErrorCode();
        } catch (Exception e) {
            LOG.error("Config network map error.");
            e.printStackTrace();
        }
        return result;
    }
}
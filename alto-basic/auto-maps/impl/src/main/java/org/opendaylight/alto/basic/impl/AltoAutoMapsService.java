/*
 * Copyright © 2017 SNLab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.alto.basic.impl;

import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.rev151021.config.context.ResourceNetworkMap;
import org.opendaylight.yang.gen.v1.urn.alto.types.rev150921.PidName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rpc.rev150105.AltoAutoMapsRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rpc.rev150105.ConfigNetworkMapInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rpc.rev150105.ConfigNetworkMapOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rpc.rev150105.ConfigNetworkMapOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rpc.rev150105.config.network.map.input.network.map.type.AnchorBased;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by shawn on 08/03/2017.
 */
public class AltoAutoMapsService implements AltoAutoMapsRpcService{

    private static final Logger LOG = LoggerFactory.getLogger(AltoAutoMapsService.class);

    private final AltoAutoMapsProvider aamp;

    private static final String DEFAULT_CONTEXT_ID = "00000000-0000-0000-0000-000000000000";

    public AltoAutoMapsService(final AltoAutoMapsProvider aamProvider) { aamp = aamProvider; }

    @Override
    public Future<RpcResult<ConfigNetworkMapOutput>> configNetworkMap(ConfigNetworkMapInput input) {
        LOG.info("The AltoAutoMapsService RPC has been called");
        System.out.printf("AltoAutoMapsService got a RPC call, add a network map named %s", input.getResourceId());
        System.out.println(((AnchorBased)input.getNetworkMapType()).toString());
        List<PidName> anchors = ((AnchorBased) (input.getNetworkMapType())).getAnchors();

        System.out.println("The number of anchors: " + anchors.size());
        List <Link> path = aamp.ngi.getPath(new NodeId(anchors.get(0).getValue()), new NodeId(anchors.get(1).getValue()));
        System.out.printf("The hop count from anchor1 to anchor2 is %d", path.size());

        List<ResourceNetworkMap> resourceNetworkMapsList = this.aamp.reader.readResourceNetworkMapList(DEFAULT_CONTEXT_ID);
        String errorCode = "Resource ID already existed. Please use another unique resource ID.";

        if (resourceNetworkMapsList == null || !this.aamp.reader.resourceIdIsExist(input.getResourceId(), resourceNetworkMapsList)) {
            aamp.generateNetworkMap(input.getResourceId().getValue(), anchors, false);
            errorCode = "The network map has been generated and stored successfully.";
        }

        ConfigNetworkMapOutputBuilder cnmob = new ConfigNetworkMapOutputBuilder();
        cnmob.setErrorCode(errorCode);
        final SettableFuture<RpcResult<ConfigNetworkMapOutput>> futureResult = SettableFuture.create();
        futureResult.set(RpcResultBuilder.success(cnmob.build()).build());
        return futureResult;
    }
}

/*
 * Copyright © 2017 SNLab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.alto.basic.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.rev151021.ConfigContext;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.rev151021.ConfigContextKey;
import org.opendaylight.yang.gen.v1.urn.alto.manual.maps.rev151021.config.context.ResourceNetworkMap;
import org.opendaylight.yang.gen.v1.urn.alto.types.rev150921.ResourceId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rev150105.ConfigNetworkMapRecords;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.alto.auto.maps.rev150105.config.network.map.records.ConfigNetworkMapRecord;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by shawn on 14/04/2017.
 */
public class AltoAutoMapsReader {
    private DataBroker dataBroker;
    private static final String DEFAULT_CONTEXT_ID = "00000000-0000-0000-0000-000000000000";
    private static final Logger LOG = LoggerFactory.getLogger(AltoAutoMapsReader.class);

    public AltoAutoMapsReader(DataBroker db) {
        this.dataBroker = db;
        //rx = dataBroker.newReadOnlyTransaction();
    }

    public List<ConfigNetworkMapRecord> readResourceNetworkMapListFromConfigRecord() {
        InstanceIdentifier<ConfigNetworkMapRecords> configNetworkMapRecordIID = InstanceIdentifier.create(ConfigNetworkMapRecords.class);
        final ReadTransaction rx = this.dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<ConfigNetworkMapRecords>, ReadFailedException> future = rx.read(LogicalDatastoreType.CONFIGURATION, configNetworkMapRecordIID);
        Optional<ConfigNetworkMapRecords> configContextOptional = Optional.absent();
        try {
            configContextOptional = future.checkedGet();
        } catch (ReadFailedException e) {
            LOG.error("Reading ConfigContext failed:", e);
        }
        ConfigNetworkMapRecords configContext = configContextOptional.get();
        LOG.info("Read ResourceNetworkmap ConfigContext is: " + configContext.toString());
        return configContext.getConfigNetworkMapRecord();
    }

    public List<ResourceNetworkMap> readResourceNetworkMapList(String s_ConfigContext) {
        InstanceIdentifier<ConfigContext> configContextIID = InstanceIdentifier.builder(ConfigContext.class, new ConfigContextKey(new Uuid(s_ConfigContext))).build();
        final ReadTransaction rx = this.dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<ConfigContext>, ReadFailedException> future = rx.read(LogicalDatastoreType.CONFIGURATION, configContextIID);
        Optional<ConfigContext> configContextOptional = Optional.absent();
        try {
            configContextOptional = future.checkedGet();
        } catch (ReadFailedException e) {
            LOG.error("Reading ConfigContext failed:", e);
        }
        ConfigContext configContext = configContextOptional.get();
        LOG.info("Read ResourceNetworkmap ConfigContext is: " + configContext.toString());
        return configContext.getResourceNetworkMap();
    }

    public boolean resourceIdIsExist(ResourceId resourceId, List<ResourceNetworkMap> mapList) {
        for (ResourceNetworkMap item: mapList) {
            LOG.info("restored Resource Id=" + item.getResourceId().toString());
            LOG.info("new Resource Id=" + resourceId.toString());
            if (item.getResourceId().equals(resourceId)) {
                return true;
            }
        }
        return false;
    }
}

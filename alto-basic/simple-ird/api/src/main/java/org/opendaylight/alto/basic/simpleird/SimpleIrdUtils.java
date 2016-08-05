/*
 * Copyright © 2015 Yale University and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.alto.basic.simpleird;

import java.util.concurrent.ExecutionException;

import com.google.common.base.Optional;

import org.opendaylight.alto.core.resourcepool.ResourcepoolUtils;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.basic.simple.ird.rev151021.IrdInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.basic.simple.ird.rev151021.IrdInstanceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.basic.simple.ird.rev151021.IrdInstanceConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.basic.simple.ird.rev151021.IrdInstanceConfigurationKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.basic.simple.ird.rev151021.ird.entry.configuration.data.location.RelativePathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.basic.simple.ird.rev151021.ird.instance.IrdEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.basic.simple.ird.rev151021.ird.instance.IrdEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.basic.simple.ird.rev151021.ird.instance.configuration.IrdConfigurationEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.basic.simple.ird.rev151021.ird.instance.configuration.IrdConfigurationEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.basic.simple.ird.rev151021.ird.instance.configuration.IrdConfigurationEntryKey;

import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.core.types.rev150921.ResourceId;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SimpleIrdUtils {

    public static final String DEFAULT_IRD_RESOURCE = "default";

    public static InstanceIdentifier<IrdInstance> getInstanceIID(String rid) {
        return getInstanceIID(new ResourceId(rid));
    }

    public static InstanceIdentifier<IrdInstance> getInstanceIID(ResourceId rid) {
        IrdInstanceKey key = new IrdInstanceKey(rid);
        return InstanceIdentifier.builder(IrdInstance.class, key).build();
    }

    public static InstanceIdentifier<IrdInstance> getInstanceListIID() {
        return InstanceIdentifier.builder(IrdInstance.class).build();
    }

    public static InstanceIdentifier<IrdInstanceConfiguration> getInstanceConfigurationIID(ResourceId rid) {
        IrdInstanceConfigurationKey key = new IrdInstanceConfigurationKey(rid);
        return InstanceIdentifier.builder(IrdInstanceConfiguration.class, key).build();
    }

    public static InstanceIdentifier<IrdInstanceConfiguration> getInstanceConfigurationListIID() {
        return InstanceIdentifier.builder(IrdInstanceConfiguration.class).build();
    }

    public static InstanceIdentifier<IrdEntry> getEntryIID(ResourceId instance, ResourceId resource) {
        IrdEntryKey key = new IrdEntryKey(resource);

        return getInstanceIID(instance).child(IrdEntry.class, key);
    }

    public static InstanceIdentifier<IrdEntry> getEntryListIID(ResourceId instance) {
        return getInstanceIID(instance).child(IrdEntry.class);
    }

    public static InstanceIdentifier<IrdConfigurationEntry> getConfigEntryIID(ResourceId instance,
                                                                                ResourceId resource) {
        IrdConfigurationEntryKey key = new IrdConfigurationEntryKey(resource);

        return getInstanceConfigurationIID(instance).child(IrdConfigurationEntry.class, key);
    }

    public static InstanceIdentifier<IrdConfigurationEntry> getConfigEntryListIID(ResourceId instance) {
        return getInstanceConfigurationIID(instance).child(IrdConfigurationEntry.class);
    }

    public static IrdInstance readInstance(ResourceId instanceId, ReadTransaction rx)
                throws InterruptedException, ExecutionException {
        InstanceIdentifier<IrdInstance> iid = getInstanceIID(instanceId);

        Optional<IrdInstance> instance = rx.read(LogicalDatastoreType.OPERATIONAL, iid).get();
        if (instance.isPresent()) {
            return instance.get();
        }
        return null;
    }

    public static void createConfigEntry(String path, ResourceId rid, Uuid cid, final WriteTransaction wx) {
        createConfigEntry(path, new ResourceId(DEFAULT_IRD_RESOURCE), rid, cid, wx);
    }

    public static void createConfigEntry(String path, ResourceId irdid, ResourceId rid, Uuid cid,
                                         final WriteTransaction wx) {
        IrdConfigurationEntryBuilder builder = new IrdConfigurationEntryBuilder();
        builder.setEntryId(rid);
        builder.setInstance(ResourcepoolUtils.getResourceIID(cid, rid));
        builder.setLocation(new RelativePathBuilder()
                .setPath(new Uri(path))
                .build());

        InstanceIdentifier<IrdConfigurationEntry> iceIID = getConfigEntryIID(irdid, rid);

        wx.put(LogicalDatastoreType.CONFIGURATION, iceIID, builder.build());
    }

    public static void deleteConfigEntry(ResourceId rid, final WriteTransaction wx) {
        deleteConfigEntry(new ResourceId(DEFAULT_IRD_RESOURCE), rid, wx);
    }

    public static void deleteConfigEntry(ResourceId irdid, ResourceId rid, final WriteTransaction wx) {
        InstanceIdentifier<IrdConfigurationEntry> iid = getConfigEntryIID(irdid, rid);

        wx.delete(LogicalDatastoreType.CONFIGURATION, iid);
    }
}

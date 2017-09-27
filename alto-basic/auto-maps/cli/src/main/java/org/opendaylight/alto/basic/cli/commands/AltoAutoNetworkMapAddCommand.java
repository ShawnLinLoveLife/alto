/*
 * Copyright © 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.alto.basic.cli.commands;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.alto.basic.cli.api.AltoAutoMapsCliCommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by shawn on 06/03/2017.
 */

@Command(name = "add",
        scope = "networkmap",
        description = "Add an ALTO network map to the controller."
                + "\n Examples: --resource-id [default-anchor-based-networkmap] --type [anchor-based] --anchors openflow:1,openflow:2")

public class AltoAutoNetworkMapAddCommand extends OsgiCommandSupport {

    protected final AltoAutoMapsCliCommands service;
    private static final String DEFAULT_RESOURCE_ID = "default-anchor-based-networkmap";
    private static final String DEFAULT_TYPE = "anchor-based";
    private static final String DEFAULT_TYPE_ABBR = "-a";
    private static final String DEFAULT_ANCHOR = "openflow:1";

    public AltoAutoNetworkMapAddCommand(final AltoAutoMapsCliCommands service) {
        this.service = service;
    }

    @Option(name = "-r",
            aliases = { "--resource-id" },
            description = "The resource id of the network map you will create, and this field is REQUIRED.",
            required = true,
            multiValued = false)
    String resourceId = DEFAULT_RESOURCE_ID;

    @Option(name = "-t",
            aliases = { "--type" },
            description = "The type of the network map you will generate, for example anchor-based type network map will generate a network map based on the anchors you give.",
            required = false,
            multiValued = false)
    String type = DEFAULT_TYPE;

    @Option(name = "-a",
            aliases = { "--anchors" },
            description = "Anchors to be used in the anchor based network map. Currently we only support OpenFlow switches, so each anchor ID MUST look like openflow:{number}.",
            required = false,
            multiValued = true)
    List<String> anchors = new ArrayList<>(Arrays.asList(DEFAULT_ANCHOR));

    @Override
    protected Object doExecute() throws Exception {
        System.out.println(resourceId);
        System.out.println(type);
        System.out.println(anchors.get(0));
        if (anchors.get(0).contains(",")) {
            anchors = Arrays.asList(anchors.get(0).split(","));
        }
        HashMap<Object, List<String>> m_HashMap = new HashMap<>();
        String result = "Fail to generate the network map.";
        if (type.equals(DEFAULT_TYPE)) {
            m_HashMap.put(DEFAULT_TYPE_ABBR, anchors);
            result = (String) service.generateNetworkMap(resourceId, type, m_HashMap);
        }
        return result;
    }
}

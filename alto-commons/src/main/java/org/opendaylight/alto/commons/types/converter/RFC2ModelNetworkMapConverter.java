package org.opendaylight.alto.commons.types.converter;

import org.opendaylight.alto.commons.helper.Converter;
import org.opendaylight.alto.commons.types.rfc7285.RFC7285NetworkMap;
import org.opendaylight.alto.commons.types.model150404.ModelNetworkMap;

public class RFC2ModelNetworkMapConverter
        extends Converter<RFC7285NetworkMap, ModelNetworkMap> {

    protected RFC2ModelNetworkMapDataConverter conv = new RFC2ModelNetworkMapDataConverter();

    public RFC2ModelNetworkMapConverter() {
    }

    public RFC2ModelNetworkMapConverter(RFC7285NetworkMap _in) {
        super(_in);
    }

    @Override
    public Object _convert() {
        ModelNetworkMap out = new ModelNetworkMap();
        out.setJSONResourceId(in().meta.vtag.rid);
        out.setJSONTag(in().meta.vtag.tag);
        out.setJSONMap(conv.reset(in().map).out());

        return out;
    }
}

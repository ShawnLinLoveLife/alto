package org.opendaylight.alto.commons.types.model150404;

import java.util.LinkedList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.rev150404.cost.map.Meta;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.service.types.rev150404.cost.map.meta.CostType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.alto.service.types.rev150404.dependent.vtags.DependentVtags;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ModelCostMapMeta implements Meta  {

  @JsonProperty("alto-service:dependent-vtags")
  public List<ModelDependentVtag> dependentVtags = new LinkedList<ModelDependentVtag>();

  @JsonProperty("alto-service:cost-type")
  public ModelCostType costType = new ModelCostType();

  @JsonIgnore
  @Override
  public Class<? extends DataContainer> getImplementedInterface() {
    return Meta.class;
  }

  @JsonIgnore
  @Override
  public List<DependentVtags> getDependentVtags() {
    return new LinkedList<DependentVtags>(dependentVtags);
  }

  @JsonIgnore
  @Override
  public CostType getCostType() {
    return costType;
  }

  @JsonIgnore
  @Override
  public <E extends Augmentation<Meta>> E getAugmentation(Class<E> arg0) {
    return null;
  }

}
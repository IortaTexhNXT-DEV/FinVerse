package com.iortatechnxt.brokerverse.migration.signoff.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import com.iortatechnxt.brokerverse.migration.mapping.domain.LayoutColumn;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMapService;
import com.iortatechnxt.brokerverse.migration.mapping.service.LayoutService;
import com.iortatechnxt.brokerverse.migration.object.domain.MigDataObject;
import com.iortatechnxt.brokerverse.migration.object.service.ObjectRegisterService;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/** The condition of the mapping sign-off (gate G2; DATA_MIGRATION_DESIGN section 13). */
@Component
class MappingGate {

  private final ObjectRegisterService register;
  private final LayoutService layouts;
  private final CodeMapService maps;

  MappingGate(ObjectRegisterService register, LayoutService layouts, CodeMapService maps) {
    this.register = register;
    this.layouts = layouts;
    this.maps = maps;
  }

  /** The layouts of the object are frozen and every code map its columns use is approved. */
  void requireReady(String objectCode) {
    MigDataObject object = register.get(objectCode);
    List<Layout> inForce = layouts.inForce(objectCode);
    if (inForce.isEmpty() && !object.getSourceSystems().isBlank()) {
      throw new BusinessRuleException(
          "MIG_LAYOUT_NOT_FROZEN",
          "Freeze the layouts of object " + objectCode + " before signing the mapping");
    }
    Set<String> missing = new TreeSet<>();
    for (Layout l : inForce) {
      layouts.columns(l.getId()).stream()
          .map(LayoutColumn::getMapSet)
          .filter(set -> set != null && !set.isBlank() && maps.approved(set).isEmpty())
          .forEach(missing::add);
    }
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "MIG_MAP_NOT_APPROVED",
          "These code maps have no approved version: " + String.join(", ", missing));
    }
  }
}

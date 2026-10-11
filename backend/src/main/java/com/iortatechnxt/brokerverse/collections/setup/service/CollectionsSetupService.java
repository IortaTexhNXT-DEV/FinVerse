package com.iortatechnxt.brokerverse.collections.setup.service;

import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.collections.common.domain.AgingBrackets;
import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange.Target;
import com.iortatechnxt.brokerverse.collections.common.domain.LovAttribute;
import com.iortatechnxt.brokerverse.collections.common.service.ChangeRecorder;
import com.iortatechnxt.brokerverse.collections.common.service.ClxSettings;
import com.iortatechnxt.brokerverse.collections.common.service.LovAttributes;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.system.domain.ParameterValueType;
import com.iortatechnxt.brokerverse.system.domain.SystemParameter;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Collections Setup (BRCLXN.005-007, 016/017, 037, 011; COLLECTIONS_DESIGN 11) under {@code
 * CLX_SETUP}: the Collections business parameters (threshold, aging basis and brackets, export cap,
 * edit-lock time and those of the other Collections waves), the attributes of the Collections LOV
 * values and the Unit Head of each sales unit. Every change is logged field by field (BRCLXN.043)
 * and audited by the owning service.
 */
@Service
@Transactional
public class CollectionsSetupService {

  /** Category of the Collections parameters. */
  public static final String CATEGORY = "COLLECTIONS";

  private static final String PARAMETER = "SystemParameter";
  private static final String SALES_UNIT = "SalesUnit";

  private final SystemParameterService parameters;
  private final LovAttributes attributes;
  private final SalesOrganisationService sales;
  private final ChangeRecorder changes;

  /**
   * Creates the service.
   *
   * @param parameters business parameters
   * @param attributes LOV attributes
   * @param sales sales organisation
   * @param changes change recorder
   */
  public CollectionsSetupService(
      SystemParameterService parameters,
      LovAttributes attributes,
      SalesOrganisationService sales,
      ChangeRecorder changes) {
    this.parameters = parameters;
    this.attributes = attributes;
    this.sales = sales;
    this.changes = changes;
  }

  /**
   * The Collections parameters.
   *
   * @return parameters by key
   */
  @Transactional(readOnly = true)
  public List<SystemParameter> parameters() {
    return parameters.list().stream().filter(p -> CATEGORY.equals(p.getCategory())).toList();
  }

  /**
   * Changes a Collections parameter; the aging brackets must parse.
   *
   * @param companyId company of the change log
   * @param key parameter key (category COLLECTIONS)
   * @param value new value
   * @return the parameter
   */
  public SystemParameter updateParameter(Long companyId, String key, String value) {
    SystemParameter current = parameters.get(key);
    if (!CATEGORY.equals(current.getCategory())) {
      throw new BusinessRuleException(
          "CLX_PARAMETER_NOT_COLLECTIONS", key + " is not a Collections parameter");
    }
    String clean = value == null ? "" : value.strip();
    if (ClxSettings.AGING_BRACKETS.equals(key)) {
      AgingBrackets.parse(ParameterValueType.items(clean));
    }
    if (ClxSettings.AGING_BASIS.equals(key) && !List.of("BOOKING", "INCEPTION").contains(clean)) {
      throw new BusinessRuleException("CLX_AGING_BASIS", "The aging basis is BOOKING or INCEPTION");
    }
    String before = current.getValue();
    SystemParameter updated = parameters.update(key, clean);
    changes.record(
        new Target(companyId, PARAMETER, key, null), "value", before, updated.getValue(), null);
    return updated;
  }

  /**
   * The attributes of the PR and unapplied-payment disposition values.
   *
   * @return attribute rows
   */
  @Transactional(readOnly = true)
  public List<LovAttribute> lovAttributes() {
    return Stream.concat(
            attributes.ofType(LovAttributes.PR_DISPOSITION).stream(),
            attributes.ofType(LovAttributes.UPP_DISPOSITION).stream())
        .toList();
  }

  /**
   * Sets or removes an attribute of a Collections LOV value (BRCLXN.016/017, 037).
   *
   * @param companyId company of the change log
   * @param typeCode LOV type
   * @param code value
   * @param attribute attribute
   * @param value value, blank to remove
   * @return the attribute, empty when removed
   */
  public Optional<LovAttribute> setLovAttribute(
      Long companyId, String typeCode, String code, String attribute, String value) {
    return attributes.set(companyId, typeCode, code, attribute, value);
  }

  /**
   * The sales units with their Unit Head (BRCLXN.011, CQ05).
   *
   * @param companyId company
   * @return units
   */
  @Transactional(readOnly = true)
  public List<SalesUnit> units(Long companyId) {
    return sales.units(companyId);
  }

  /**
   * Sets or clears the Unit Head of a sales unit (BRCLXN.011/012); the worklist takes it at the
   * next refresh.
   *
   * @param companyId company
   * @param unitCode sales unit
   * @param username head, blank to clear
   * @return the unit
   */
  public SalesUnit assignUnitHead(Long companyId, String unitCode, String username) {
    String before =
        sales.units(companyId).stream()
            .filter(u -> u.getCode().equals(unitCode))
            .findFirst()
            .map(SalesUnit::getHeadUsername)
            .orElse(null);
    SalesUnit unit = sales.assignHead(companyId, unitCode, username);
    changes.record(
        new Target(companyId, SALES_UNIT, unitCode, null),
        "headUsername",
        before,
        unit.getHeadUsername(),
        null);
    return unit;
  }
}

package com.iortatechnxt.brokerverse.collections.common.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.OpsAction;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.TaggingOwner;
import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange.Target;
import com.iortatechnxt.brokerverse.collections.common.domain.LovAttribute;
import com.iortatechnxt.brokerverse.collections.common.domain.LovAttributeRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The attributes of the Collections LOV values ({@code clx_lov_attribute}, V1000): read for the
 * rules of a PR collector disposition (BRCLXN.016) or an unapplied-payment disposition (BRCLXN.037,
 * 047/048), and maintained on Collections Setup under {@code CLX_SETUP} with validation of each
 * attribute's values.
 */
@Service
@Transactional
public class LovAttributes {

  /** LOV type of the PR collector dispositions. */
  public static final String PR_DISPOSITION = "CLX_PR_DISPOSITION";

  /** LOV type of the unapplied-payment dispositions. */
  public static final String UPP_DISPOSITION = "CLX_UPP_DISPOSITION";

  static final String CATEGORY = "category";
  static final String TAGGING_OWNER = "tagging_owner";
  static final String OPS_ACTION = "ops_action";
  static final String ALLOWED_ROLES = "allowed_roles";
  static final String REQUIRES_INVOICE = "requires_invoice";
  static final String CASHIERING_ACTION = "cashiering_action";

  private static final Pattern ROLE_LIST = Pattern.compile("^[A-Z0-9_]{1,40}(,[A-Z0-9_]{1,40})*$");
  private static final Set<String> CASHIERING_ACTIONS =
      Set.of("APPLY_TO_INVOICE", "REFUND", "RECLASS", "TRANSFER", "NONE");
  private static final String ENTITY = "CollectionsLovAttribute";

  private final LovAttributeRepository attributes;
  private final LovService lovs;
  private final ChangeRecorder changes;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param attributes attribute rows
   * @param lovs lists of values
   * @param changes change recorder
   * @param audit audit trail
   */
  public LovAttributes(
      LovAttributeRepository attributes,
      LovService lovs,
      ChangeRecorder changes,
      AuditTrailService audit) {
    this.attributes = attributes;
    this.lovs = lovs;
    this.changes = changes;
    this.audit = audit;
  }

  /**
   * The attributes of one value.
   *
   * @param typeCode LOV type
   * @param code value
   * @return attribute values by name
   */
  @Transactional(readOnly = true)
  public Map<String, String> of(String typeCode, String code) {
    return attributes.findByTypeCodeAndCode(typeCode, code).stream()
        .collect(
            Collectors.toMap(
                LovAttribute::getAttribute,
                LovAttribute::getValue,
                (a, b) -> b,
                LinkedHashMap::new));
  }

  /**
   * The attributes of every value of a type.
   *
   * @param typeCode LOV type
   * @return attribute rows by code and attribute
   */
  @Transactional(readOnly = true)
  public List<LovAttribute> ofType(String typeCode) {
    return attributes.findByTypeCodeOrderByCodeAscAttributeAsc(typeCode);
  }

  /**
   * The rule of a PR collector disposition (BRCLXN.016): category, owner, Operations action and the
   * roles allowed to set it.
   *
   * @param code disposition code
   * @return rule (NONE action and no restriction when the attributes are missing)
   */
  @Transactional(readOnly = true)
  public DispositionRule prRule(String code) {
    Map<String, String> a = of(PR_DISPOSITION, code);
    String owner = a.get(TAGGING_OWNER);
    String roles = a.get(ALLOWED_ROLES);
    return new DispositionRule(
        code,
        a.get(CATEGORY),
        owner == null ? null : TaggingOwner.valueOf(owner),
        OpsAction.valueOf(a.getOrDefault(OPS_ACTION, OpsAction.NONE.name())),
        roles == null || roles.isBlank()
            ? Set.of()
            : Arrays.stream(roles.split(",")).map(String::strip).collect(Collectors.toSet()));
  }

  /**
   * Sets, changes or removes an attribute (Collections Setup, BRCLXN.017/037).
   *
   * @param companyId company of the change log
   * @param typeCode CLX_PR_DISPOSITION or CLX_UPP_DISPOSITION
   * @param code value code (must exist in the LOV)
   * @param attribute attribute name
   * @param value new value; blank removes the attribute
   * @return the attribute, empty when removed
   */
  public Optional<LovAttribute> set(
      Long companyId, String typeCode, String code, String attribute, String value) {
    String clean = value == null || value.isBlank() ? null : value.strip();
    validate(typeCode, code, attribute, clean);
    Optional<LovAttribute> existing =
        attributes.findByTypeCodeAndCodeAndAttribute(typeCode, code, attribute);
    String before = existing.map(LovAttribute::getValue).orElse(null);
    Optional<LovAttribute> result;
    if (clean == null) {
      existing.ifPresent(attributes::delete);
      result = Optional.empty();
    } else if (existing.isPresent()) {
      existing.get().change(clean);
      result = existing;
    } else {
      result = Optional.of(attributes.save(new LovAttribute(typeCode, code, attribute, clean)));
    }
    String key = typeCode + ":" + code;
    changes.record(new Target(companyId, ENTITY, key, null), attribute, before, clean, null);
    audit.record(ENTITY, key, AuditAction.UPDATE, attribute + " = " + clean);
    return result;
  }

  private void validate(String typeCode, String code, String attribute, String value) {
    if (lovs.values(typeCode).stream().noneMatch(v -> v.getCode().equals(code))) {
      throw new BusinessRuleException(
          "CLX_LOV_VALUE_UNKNOWN", code + " is not a value of " + typeCode);
    }
    boolean pr = PR_DISPOSITION.equals(typeCode);
    boolean upp = UPP_DISPOSITION.equals(typeCode);
    boolean known =
        pr && Set.of(CATEGORY, TAGGING_OWNER, OPS_ACTION, ALLOWED_ROLES).contains(attribute)
            || upp && Set.of(REQUIRES_INVOICE, CASHIERING_ACTION).contains(attribute);
    if (!known) {
      throw new BusinessRuleException(
          "CLX_LOV_ATTRIBUTE_UNKNOWN", attribute + " is not an attribute of " + typeCode);
    }
    if (value != null && !validValue(attribute, value)) {
      throw new BusinessRuleException(
          "CLX_LOV_ATTRIBUTE_VALUE", "'" + value + "' is not a valid " + attribute);
    }
  }

  private static boolean validValue(String attribute, String value) {
    return switch (attribute) {
      case CATEGORY -> Set.of("A", "B", "C").contains(value);
      case TAGGING_OWNER -> isEnum(TaggingOwner.class, value);
      case OPS_ACTION -> isEnum(OpsAction.class, value);
      case ALLOWED_ROLES -> ROLE_LIST.matcher(value).matches();
      case REQUIRES_INVOICE -> "true".equals(value) || "false".equals(value);
      default -> CASHIERING_ACTIONS.contains(value);
    };
  }

  private static <E extends Enum<E>> boolean isEnum(Class<E> type, String value) {
    return Arrays.stream(type.getEnumConstants()).anyMatch(e -> e.name().equals(value));
  }

  /**
   * The rule of a PR collector disposition.
   *
   * @param code disposition code
   * @param category tagging category A / B / C, null when not set (CQ08)
   * @param taggingOwner owner after the disposition, null when not set
   * @param opsAction Operations hand-off
   * @param allowedRoles roles allowed to set it; empty = every user with CLX_WORK
   */
  public record DispositionRule(
      String code,
      String category,
      TaggingOwner taggingOwner,
      OpsAction opsAction,
      Set<String> allowedRoles) {

    /** Defensive copy. */
    public DispositionRule {
      allowedRoles = Set.copyOf(allowedRoles);
    }
  }
}

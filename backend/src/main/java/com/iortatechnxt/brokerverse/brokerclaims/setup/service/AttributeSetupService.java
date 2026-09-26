package com.iortatechnxt.brokerverse.brokerclaims.setup.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimLovAttribute;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimLovAttributeRepository;
import com.iortatechnxt.brokerverse.brokerclaims.setup.domain.AttributeChange;
import com.iortatechnxt.brokerverse.brokerclaims.setup.domain.AttributeChangeRepository;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Status and settlement type attributes on Claims Setup (BRCLM.010/014, FR-CM-040/043): the Unit
 * Head proposes the attributes of a value (validated here with the FRS messages), another BCL_SETUP
 * user authorizes them and they apply to {@code bcl_lov_attribute} without a new build. The values
 * themselves are maintained through the list-of-values API under the owner permission BCL_SETUP.
 */
@Service
@Transactional
public class AttributeSetupService {

  private static final String ENTITY = "BrokerClaimAttribute";

  private final ClaimLovAttributeRepository attributes;
  private final AttributeChangeRepository changes;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param attributes current attributes
   * @param changes proposed attributes
   * @param lovs lists of values
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public AttributeSetupService(
      ClaimLovAttributeRepository attributes,
      AttributeChangeRepository changes,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.attributes = attributes;
    this.changes = changes;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Every value of a list (statuses or settlement types) with its current and pending attributes.
   *
   * @param typeCode {@code BCL_CLAIM_STATUS} or {@code BCL_SETTLEMENT_TYPE}
   * @return values in list order
   */
  @Transactional(readOnly = true)
  public List<ValueAttributes> list(String typeCode) {
    requireType(typeCode);
    Map<String, Map<String, String>> current =
        attributes.findByTypeCodeOrderByCodeAscAttributeAsc(typeCode).stream()
            .collect(
                Collectors.groupingBy(
                    ClaimLovAttribute::getCode,
                    Collectors.toMap(
                        ClaimLovAttribute::getAttribute, ClaimLovAttribute::getValue)));
    Map<String, List<AttributeChange>> pending =
        changes.findByTypeCodeAndRecordStatus(typeCode, RecordStatus.PENDING_AUTHORIZATION).stream()
            .collect(Collectors.groupingBy(AttributeChange::getCode));
    return lovs.values(typeCode).stream()
        .map(v -> view(v, current.getOrDefault(v.getCode(), Map.of()), pending.get(v.getCode())))
        .toList();
  }

  /**
   * Proposes the attributes of a status (FR-CM-040).
   *
   * @param code status
   * @param proposal phase, waiting party, follow-up days and awaiting remittance flag
   * @return the value with its pending attributes
   */
  public ValueAttributes proposeStatus(String code, StatusAttributes proposal) {
    Map<String, String> values = AttributeRules.status(proposal);
    return propose(ClaimCodes.LOV_STATUS, code, values);
  }

  /**
   * Proposes the attributes of a settlement type (FR-CM-043).
   *
   * @param code settlement type
   * @param proposal outcome, closure and amount flags
   * @return the value with its pending attributes
   */
  public ValueAttributes proposeSettlement(String code, SettlementAttributes proposal) {
    Map<String, String> values = AttributeRules.settlement(proposal);
    return propose(ClaimCodes.LOV_SETTLEMENT_TYPE, code, values);
  }

  /**
   * Authorizes the pending attributes of a value (another user than the maker) and applies them.
   *
   * @param typeCode list
   * @param code value
   * @return the value with its new attributes
   */
  public ValueAttributes authorize(String typeCode, String code) {
    List<AttributeChange> pending = pending(typeCode, code);
    String checker = currentUser.username();
    for (AttributeChange change : pending) {
      change.authorize(checker, clock.instant());
      apply(change);
    }
    audit.record(ENTITY, typeCode + ":" + code, AuditAction.AUTHORIZE, describe(pending));
    return single(typeCode, code);
  }

  /**
   * Withdraws or rejects the pending attributes of a value.
   *
   * @param typeCode list
   * @param code value
   * @return the value with its current attributes
   */
  public ValueAttributes reject(String typeCode, String code) {
    List<AttributeChange> pending = pending(typeCode, code);
    pending.forEach(AttributeChange::deactivate);
    audit.record(ENTITY, typeCode + ":" + code, AuditAction.REJECT, describe(pending));
    return single(typeCode, code);
  }

  private ValueAttributes propose(String typeCode, String code, Map<String, String> values) {
    LovValue value = requireValue(typeCode, code);
    Map<String, String> current = currentOf(typeCode, code);
    changes
        .findByTypeCodeAndCodeAndRecordStatus(typeCode, code, RecordStatus.PENDING_AUTHORIZATION)
        .forEach(AttributeChange::deactivate);
    List<AttributeChange> proposed =
        values.entrySet().stream()
            .filter(e -> !Objects.equals(current.get(e.getKey()), e.getValue()))
            .map(e -> new AttributeChange(typeCode, code, e.getKey(), e.getValue()))
            .toList();
    if (proposed.isEmpty()) {
      throw new BusinessRuleException(
          "BCL_ATTRIBUTES_UNCHANGED", "The attributes of " + value.getLabel() + " are unchanged");
    }
    changes.saveAll(proposed);
    audit.record(ENTITY, typeCode + ":" + code, AuditAction.SUBMIT, describe(proposed));
    return single(typeCode, code);
  }

  private void apply(AttributeChange change) {
    var existing =
        attributes.findByTypeCodeAndCodeAndAttribute(
            change.getTypeCode(), change.getCode(), change.getAttribute());
    if (change.getNewValue() == null) {
      existing.ifPresent(attributes::delete);
    } else if (existing.isPresent()) {
      existing.get().changeValue(change.getNewValue());
    } else {
      attributes.save(
          new ClaimLovAttribute(
              change.getTypeCode(), change.getCode(), change.getAttribute(), change.getNewValue()));
    }
  }

  private List<AttributeChange> pending(String typeCode, String code) {
    requireType(typeCode);
    List<AttributeChange> pending =
        changes.findByTypeCodeAndCodeAndRecordStatus(
            typeCode, code, RecordStatus.PENDING_AUTHORIZATION);
    if (pending.isEmpty()) {
      throw new BusinessRuleException(
          "RECORD_NOT_PENDING", "No attribute change of " + code + " waits for authorization");
    }
    return pending;
  }

  private ValueAttributes single(String typeCode, String code) {
    LovValue value = requireValue(typeCode, code);
    List<AttributeChange> pending =
        changes.findByTypeCodeAndCodeAndRecordStatus(
            typeCode, code, RecordStatus.PENDING_AUTHORIZATION);
    return view(value, currentOf(typeCode, code), pending.isEmpty() ? null : pending);
  }

  private Map<String, String> currentOf(String typeCode, String code) {
    Map<String, String> current = new LinkedHashMap<>();
    attributes.findByTypeCodeOrderByCodeAscAttributeAsc(typeCode).stream()
        .filter(a -> a.getCode().equals(code))
        .forEach(a -> current.put(a.getAttribute(), a.getValue()));
    return current;
  }

  private LovValue requireValue(String typeCode, String code) {
    requireType(typeCode);
    return lovs.values(typeCode).stream()
        .filter(v -> v.getCode().equals(code))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException(typeCode, code));
  }

  private static void requireType(String typeCode) {
    if (!ClaimCodes.LOV_STATUS.equals(typeCode)
        && !ClaimCodes.LOV_SETTLEMENT_TYPE.equals(typeCode)) {
      throw new BusinessRuleException(
          "BCL_ATTRIBUTE_LIST_INVALID", typeCode + " has no Claims attributes");
    }
  }

  private static ValueAttributes view(
      LovValue value, Map<String, String> current, List<AttributeChange> pending) {
    Map<String, String> proposed = new LinkedHashMap<>();
    String maker = null;
    if (pending != null) {
      for (AttributeChange change : pending) {
        proposed.put(change.getAttribute(), change.getNewValue());
        maker = change.getMaker();
      }
    }
    return new ValueAttributes(
        value.getCode(),
        value.getLabel(),
        value.getRecordStatus(),
        Map.copyOf(current),
        proposed,
        maker);
  }

  private static String describe(List<AttributeChange> list) {
    return list.stream()
        .map(c -> c.getAttribute() + "=" + (c.getNewValue() == null ? "(none)" : c.getNewValue()))
        .collect(Collectors.joining(", "));
  }

  /**
   * A value with its attributes.
   *
   * @param code value code
   * @param label label
   * @param valueStatus record status of the list value
   * @param attributes current attributes
   * @param pending proposed attributes waiting for authorization (null value = removed)
   * @param pendingBy maker of the proposal
   */
  public record ValueAttributes(
      String code,
      String label,
      RecordStatus valueStatus,
      Map<String, String> attributes,
      Map<String, String> pending,
      String pendingBy) {}

  /**
   * The attributes of a status.
   *
   * @param phase NEW, IN_PROGRESS or TEMP_CLOSED
   * @param waitingOn INSURER, CLAIMANT, ASSURED, ADJUSTER or BDOI
   * @param followUpDays whole number 1-365, blank for the parameter
   * @param awaitingPremiumRemittance whether the status awaits the premium remittance
   */
  public record StatusAttributes(
      String phase, String waitingOn, String followUpDays, boolean awaitingPremiumRemittance) {}

  /**
   * The attributes of a settlement type.
   *
   * @param outcome SETTLED or CLOSED_WITHOUT_PAYMENT
   * @param closesClaim whether the type closes the claim
   * @param requiresSettlementAmount whether amount and date are required
   */
  public record SettlementAttributes(
      String outcome, boolean closesClaim, boolean requiresSettlementAmount) {}
}

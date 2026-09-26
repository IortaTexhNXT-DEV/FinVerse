package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimLovAttribute;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimLovAttributeRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The behaviour of the BDOI statuses and settlement types, read from their attributes in {@code
 * bcl_lov_attribute} (BRCLM.010/014, FR-CL-040/043; CLAIMS_BROKING_DESIGN 5.3): the phase a status
 * belongs to, its follow-up days (else parameter {@code BCL_FOLLOW_UP_DAYS}), whether it awaits the
 * premium remittance, and the outcome, closure and amount rules of a settlement type. Nothing
 * business-specific is coded: the Unit Head changes the attributes on Claims Setup.
 */
@Service
@Transactional(readOnly = true)
public class StatusRules {

  private static final int DEFAULT_FOLLOW_UP_DAYS = 7;

  private final ClaimLovAttributeRepository attributes;
  private final SystemParameterService parameters;
  private final LovService lovs;

  /**
   * Creates the rules.
   *
   * @param attributes list value attributes
   * @param parameters business parameters
   * @param lovs lists of values (labels)
   */
  public StatusRules(
      ClaimLovAttributeRepository attributes, SystemParameterService parameters, LovService lovs) {
    this.attributes = attributes;
    this.parameters = parameters;
    this.lovs = lovs;
  }

  /**
   * The phase of a status; a status without a phase (or with phase CLOSED, which only a settlement
   * type reaches) cannot be set (FR-CL-040 R1).
   *
   * @param statusCode status
   * @return phase NEW, IN_PROGRESS or TEMP_CLOSED
   */
  public ClaimPhase phaseOf(String statusCode) {
    Optional<ClaimPhase> phase =
        attribute(ClaimCodes.LOV_STATUS, statusCode, ClaimCodes.ATTR_PHASE)
            .flatMap(StatusRules::phase)
            .filter(p -> p != ClaimPhase.CLOSED);
    return phase.orElseThrow(
        () ->
            new BusinessRuleException(
                "BCL_STATUS_WITHOUT_PHASE",
                "Set the phase of the status " + statusLabel(statusCode)));
  }

  /**
   * Whether a status has a usable phase.
   *
   * @param statusCode status
   * @return true when it has NEW, IN_PROGRESS or TEMP_CLOSED
   */
  public boolean hasPhase(String statusCode) {
    return attribute(ClaimCodes.LOV_STATUS, statusCode, ClaimCodes.ATTR_PHASE)
        .flatMap(StatusRules::phase)
        .filter(p -> p != ClaimPhase.CLOSED)
        .isPresent();
  }

  /**
   * Days from a status change to the next follow-up (BRCLM.019, FR-CL-050 R1).
   *
   * @param statusCode status
   * @return the status's follow-up days, else {@code BCL_FOLLOW_UP_DAYS}
   */
  public int followUpDays(String statusCode) {
    return attribute(ClaimCodes.LOV_STATUS, statusCode, ClaimCodes.ATTR_FOLLOW_UP_DAYS)
        .flatMap(StatusRules::wholeNumber)
        .orElseGet(
            () -> parameters.intValue(ClaimCodes.PARAM_FOLLOW_UP_DAYS, DEFAULT_FOLLOW_UP_DAYS));
  }

  /**
   * Whether a status means the claim waits for BDOI to remit the premium (status 11, OQ46).
   *
   * @param statusCode status
   * @return true when flagged {@code awaiting_premium_remittance}
   */
  public boolean awaitsPremiumRemittance(String statusCode) {
    return statusCode != null
        && attributes
            .findByTypeCodeAndCodeAndAttribute(
                ClaimCodes.LOV_STATUS, statusCode, ClaimCodes.ATTR_AWAITING_PREMIUM_REMITTANCE)
            .map(ClaimLovAttribute::isTrue)
            .orElse(false);
  }

  /**
   * The statuses flagged as awaiting premium remittance.
   *
   * @return status codes
   */
  public List<String> premiumRemittanceStatuses() {
    return attributes
        .findByTypeCodeAndAttributeAndValueIgnoreCase(
            ClaimCodes.LOV_STATUS, ClaimCodes.ATTR_AWAITING_PREMIUM_REMITTANCE, "true")
        .stream()
        .map(ClaimLovAttribute::getCode)
        .toList();
  }

  /**
   * The rule of a settlement type (FR-CL-043/044); a type without an outcome cannot be used.
   *
   * @param typeCode settlement type
   * @return outcome, closure and amount flags
   */
  public SettlementRule settlementRule(String typeCode) {
    String outcome =
        attribute(ClaimCodes.LOV_SETTLEMENT_TYPE, typeCode, ClaimCodes.ATTR_OUTCOME)
            .filter(v -> !v.isBlank())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "BCL_SETTLEMENT_WITHOUT_OUTCOME",
                        "Set the outcome of the settlement type " + settlementLabel(typeCode)));
    return new SettlementRule(
        outcome,
        flag(typeCode, ClaimCodes.ATTR_CLOSES_CLAIM),
        flag(typeCode, ClaimCodes.ATTR_REQUIRES_SETTLEMENT_AMOUNT));
  }

  /**
   * Label of a status.
   *
   * @param statusCode status, may be null
   * @return label (the code when unknown), empty for null
   */
  public String statusLabel(String statusCode) {
    return statusCode == null ? "" : lovs.label(ClaimCodes.LOV_STATUS, statusCode);
  }

  /**
   * Label of a settlement type.
   *
   * @param typeCode settlement type, may be null
   * @return label (the code when unknown), empty for null
   */
  public String settlementLabel(String typeCode) {
    return typeCode == null ? "" : lovs.label(ClaimCodes.LOV_SETTLEMENT_TYPE, typeCode);
  }

  private boolean flag(String typeCode, String attribute) {
    return attributes
        .findByTypeCodeAndCodeAndAttribute(ClaimCodes.LOV_SETTLEMENT_TYPE, typeCode, attribute)
        .map(ClaimLovAttribute::isTrue)
        .orElse(false);
  }

  private Optional<String> attribute(String type, String code, String attribute) {
    return attributes
        .findByTypeCodeAndCodeAndAttribute(type, code, attribute)
        .map(ClaimLovAttribute::getValue);
  }

  private static Optional<ClaimPhase> phase(String value) {
    try {
      return Optional.of(ClaimPhase.valueOf(value.strip().toUpperCase(Locale.ROOT)));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  private static Optional<Integer> wholeNumber(String value) {
    try {
      return Optional.of(Integer.parseInt(value.strip()));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }

  /**
   * How a settlement type behaves.
   *
   * @param outcome SETTLED or CLOSED_WITHOUT_PAYMENT
   * @param closesClaim whether the type closes the claim permanently
   * @param requiresAmount whether settlement amount and date are required
   */
  public record SettlementRule(String outcome, boolean closesClaim, boolean requiresAmount) {}
}

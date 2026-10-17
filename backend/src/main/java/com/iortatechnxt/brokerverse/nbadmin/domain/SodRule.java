package com.iortatechnxt.brokerverse.nbadmin.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * A separation-of-duties rule: two group profiles that one user may not hold together (V1065).
 * Created and deactivated under maker-checker: a new rule is Pending Authorization until another
 * user authorises it; the deactivation of an active rule waits for the same authorisation.
 */
@Entity
@Table(name = "nba_sod_rule")
public class SodRule extends AuthorizableEntity {

  /** What the pending authorisation decides. */
  public enum PendingAction {
    /** A new rule waits for its authorisation. */
    CREATE,
    /** The deactivation of an active rule waits for its authorisation. */
    DEACTIVATE,
    /** Nothing waits. */
    NONE
  }

  private static final String NOT_PENDING = "SOD_RULE_NOT_PENDING";

  @Column(name = "rule_code", nullable = false, length = 30, updatable = false)
  private String ruleCode;

  @Column(name = "profile_a", nullable = false, length = 40, updatable = false)
  private String profileA;

  @Column(name = "profile_b", nullable = false, length = 40, updatable = false)
  private String profileB;

  @Column(nullable = false, length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "rule_kind", nullable = false, length = 15, updatable = false)
  private SodRuleKind kind = SodRuleKind.PROFILES;

  @Enumerated(EnumType.STRING)
  @Column(name = "pending_action", nullable = false, length = 20)
  private PendingAction pendingAction = PendingAction.CREATE;

  protected SodRule() {}

  /**
   * A new rule of a kind, pending authorisation.
   *
   * @param kind two group profiles or two permissions
   * @param ruleCode rule number
   * @param first first group profile or permission
   * @param second second group profile or permission
   * @param description why the two may not be held together
   */
  public SodRule(
      SodRuleKind kind, String ruleCode, String first, String second, String description) {
    this(ruleCode, first, second, description);
    this.kind = kind;
  }

  /**
   * Creates a rule between two group profiles, pending authorisation.
   *
   * @param ruleCode rule code
   * @param profileA first group profile
   * @param profileB second group profile
   * @param description why they may not be held together
   */
  public SodRule(String ruleCode, String profileA, String profileB, String description) {
    this.ruleCode = ruleCode;
    this.profileA = profileA;
    this.profileB = profileB;
    this.description = description;
  }

  /**
   * Whether the rule forbids holding both profiles of the set.
   *
   * @param roleCodes group profiles of a user
   * @return true when both profiles of the rule are in the set
   */
  public boolean forbids(Set<String> roleCodes) {
    return roleCodes.contains(profileA) && roleCodes.contains(profileB);
  }

  /**
   * Whether the rule is about the same pair of profiles, in either order.
   *
   * @param a first profile
   * @param b second profile
   * @return true for the same pair
   */
  public boolean samePair(String a, String b) {
    return profileA.equals(a) && profileB.equals(b) || profileA.equals(b) && profileB.equals(a);
  }

  /**
   * Asks for the deactivation of an active rule; it waits for the authorisation.
   *
   * @throws BusinessRuleException when the rule is not active or a change already waits
   */
  public void requestDeactivation() {
    if (!isActive() || pendingAction != PendingAction.NONE) {
      throw new BusinessRuleException(
          NOT_PENDING, "Rule " + ruleCode + " is not active or already waits for an authorisation");
    }
    this.pendingAction = PendingAction.DEACTIVATE;
  }

  /**
   * Authorises the pending creation or deactivation; never by its maker.
   *
   * @param checker authorising user
   * @param maker user who created the rule or asked for the deactivation
   * @param when time
   */
  public void authorizePending(String checker, String maker, Instant when) {
    if (Objects.equals(maker, checker)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
    switch (pendingAction) {
      case CREATE -> authorize(checker, when);
      case DEACTIVATE -> deactivate();
      default ->
          throw new BusinessRuleException(
              NOT_PENDING, "Rule " + ruleCode + " does not wait for an authorisation");
    }
    this.pendingAction = PendingAction.NONE;
  }

  /**
   * Rejects the pending creation (the rule is closed) or deactivation (the rule stays active).
   *
   * @param checker rejecting user
   * @param maker maker of the pending change
   */
  public void rejectPending(String checker, String maker) {
    if (Objects.equals(maker, checker)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A record cannot be authorized by the user who maintained it");
    }
    if (pendingAction == PendingAction.NONE) {
      throw new BusinessRuleException(
          NOT_PENDING, "Rule " + ruleCode + " does not wait for an authorisation");
    }
    if (pendingAction == PendingAction.CREATE) {
      deactivate();
    }
    this.pendingAction = PendingAction.NONE;
  }

  /**
   * Whether the rule waits for an authorisation.
   *
   * @return true when a creation or deactivation waits
   */
  public boolean isPending() {
    return pendingAction != PendingAction.NONE && getRecordStatus() != RecordStatus.INACTIVE;
  }

  /**
   * What the rule pairs: two group profiles or two permissions.
   *
   * @return kind
   */
  public SodRuleKind getKind() {
    return kind;
  }

  public String getRuleCode() {
    return ruleCode;
  }

  public String getProfileA() {
    return profileA;
  }

  public String getProfileB() {
    return profileB;
  }

  public String getDescription() {
    return description;
  }

  public PendingAction getPendingAction() {
    return pendingAction;
  }
}

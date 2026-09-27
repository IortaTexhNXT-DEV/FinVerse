package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeStage;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A Disbursement payee (DIS 2.2.0-2.2.8): the payment profile of a party code with its class (LOV
 * {@code PAYEE_CLASS}), default and allowed modes of payment, disbursement types, currency, default
 * cost centre (DIS 3.30.0) and bank accounts. Maintained under workflow {@code DISB_PAYEE}: a draft
 * is authorised by a checker before it can be paid; deactivation and reactivation are authorised
 * the same way. A payee that was never used may be deleted (DIS 2.2.4), otherwise it is
 * deactivated. Taxes (TIN, ATC, VAT) come from the tax profile of the party.
 */
@Entity
@Table(name = "dsb_payee")
public class Payee extends BaseEntity {

  private static final String SEPARATOR = ",";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "payee_code", nullable = false, length = 30, updatable = false)
  private String payeeCode;

  @Column(name = "payee_class", nullable = false, length = 20)
  private String payeeClass;

  @Column(nullable = false, length = 250)
  private String name;

  @Column(length = 500)
  private String address;

  @Column(length = 200)
  private String email;

  @Column(length = 30)
  private String tin;

  @Enumerated(EnumType.STRING)
  @Column(name = "default_mode", nullable = false, length = 20)
  private DisbursementMode defaultMode;

  @Column(name = "allowed_modes", nullable = false, length = 200)
  private String allowedModes;

  @Column(name = "disbursement_types", length = 300)
  private String disbursementTypes;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "default_cost_center", length = 20)
  private String defaultCostCenter;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private PayeeSource source;

  @Column(name = "source_ref", length = 80, updatable = false)
  private String sourceRef;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private PayeeStage stage = PayeeStage.DRAFT;

  @Column(nullable = false)
  private boolean used;

  @Column(length = 500)
  private String remarks;

  @OneToMany(mappedBy = "payee", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<PayeeAccount> accounts = new ArrayList<>();

  protected Payee() {}

  /**
   * A new payee in DRAFT.
   *
   * @param companyId company
   * @param payeeCode party code
   * @param source where it came from
   * @param sourceRef source reference (request, upload), may be null
   */
  public Payee(Long companyId, String payeeCode, PayeeSource source, String sourceRef) {
    this.companyId = companyId;
    this.payeeCode = payeeCode;
    this.source = source;
    this.sourceRef = sourceRef;
  }

  /**
   * Sets the maintained details (DIS 2.2.2-2.2.6); the default mode must be one of the allowed
   * modes.
   *
   * @param details details
   */
  public void update(PayeeDetails details) {
    if (!details.allowedModes().contains(details.defaultMode())) {
      throw new BusinessRuleException(
          "PAYEE_MODE", "The default mode of payment must be one of the allowed modes");
    }
    payeeClass = details.payeeClass();
    name = details.name();
    address = details.address();
    email = details.email();
    tin = details.tin();
    defaultMode = details.defaultMode();
    allowedModes = String.join(SEPARATOR, details.allowedModes().stream().map(Enum::name).toList());
    disbursementTypes =
        details.disbursementTypes().isEmpty()
            ? null
            : String.join(SEPARATOR, details.disbursementTypes());
    currency = details.currency();
    defaultCostCenter = details.defaultCostCenter();
    remarks = details.remarks();
  }

  /**
   * Adds a bank account; the first one (or one flagged primary) becomes the primary account.
   *
   * @param account account
   */
  public void addAccount(PayeeAccount account) {
    boolean firstActive = accounts.stream().noneMatch(PayeeAccount::isActive);
    if (account.isPrimaryAccount() || firstActive) {
      accounts.forEach(a -> a.setPrimaryAccount(false));
      account.setPrimaryAccount(true);
    }
    accounts.add(account);
  }

  /**
   * The primary active bank account.
   *
   * @return account, empty when the payee has none
   */
  public Optional<PayeeAccount> primaryAccount() {
    return accounts.stream()
        .filter(PayeeAccount::isActive)
        .sorted((a, b) -> Boolean.compare(b.isPrimaryAccount(), a.isPrimaryAccount()))
        .findFirst();
  }

  /**
   * Moves the payee to a workflow stage (mirror of {@code DISB_PAYEE}).
   *
   * @param next stage
   */
  public void markStage(PayeeStage next) {
    stage = next;
  }

  /** The payee was used by a voucher: it can no longer be deleted (DIS 2.2.4). */
  public void markUsed() {
    used = true;
  }

  /**
   * Whether the mode is allowed for this payee.
   *
   * @param mode mode
   * @return true when allowed
   */
  public boolean allows(DisbursementMode mode) {
    return getAllowedModes().contains(mode);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getPayeeCode() {
    return payeeCode;
  }

  public String getPayeeClass() {
    return payeeClass;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  public String getEmail() {
    return email;
  }

  public String getTin() {
    return tin;
  }

  public DisbursementMode getDefaultMode() {
    return defaultMode;
  }

  /**
   * The allowed modes of payment.
   *
   * @return modes
   */
  public List<DisbursementMode> getAllowedModes() {
    return allowedModes == null
        ? List.of()
        : Arrays.stream(allowedModes.split(SEPARATOR)).map(DisbursementMode::valueOf).toList();
  }

  /**
   * The disbursement types the payee is set up for (empty = any).
   *
   * @return type codes
   */
  public List<String> getDisbursementTypes() {
    return disbursementTypes == null ? List.of() : List.of(disbursementTypes.split(SEPARATOR));
  }

  public String getCurrency() {
    return currency;
  }

  public String getDefaultCostCenter() {
    return defaultCostCenter;
  }

  public PayeeSource getSource() {
    return source;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public PayeeStage getStage() {
    return stage;
  }

  public boolean isUsed() {
    return used;
  }

  public String getRemarks() {
    return remarks;
  }

  public List<PayeeAccount> getAccounts() {
    return List.copyOf(accounts);
  }

  /**
   * Maintained details of a payee.
   *
   * @param payeeClass payee class (LOV {@code PAYEE_CLASS})
   * @param name name
   * @param address address
   * @param email e-mail for payment advices
   * @param tin tax identification number
   * @param defaultMode default mode of payment
   * @param allowedModes allowed modes
   * @param disbursementTypes disbursement types (LOV {@code DISBURSEMENT_TYPE}), empty = any
   * @param currency currency
   * @param defaultCostCenter default cost centre of expense lines (DIS 3.30.0)
   * @param remarks remarks
   */
  public record PayeeDetails(
      String payeeClass,
      String name,
      String address,
      String email,
      String tin,
      DisbursementMode defaultMode,
      List<DisbursementMode> allowedModes,
      List<String> disbursementTypes,
      String currency,
      String defaultCostCenter,
      String remarks) {

    /** Defensive copies. */
    public PayeeDetails {
      allowedModes = allowedModes == null ? List.of() : List.copyOf(allowedModes);
      disbursementTypes = disbursementTypes == null ? List.of() : List.copyOf(disbursementTypes);
    }
  }
}

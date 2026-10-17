package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.EmailAddresses;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceRecipient;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceRecipientRepository;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Insurance Advice recipient set-up (FR-NB-107): per mortgagee bank, and where needed per
 * market segment, the recipient addresses and the enrolment for automatic sending. Every change
 * waits for a checker other than its maker; only authorized set-ups in force are used.
 */
@Service
@Transactional
public class AdviceRecipientService {

  /** Audit entity. */
  public static final String ENTITY = "InsuranceAdviceRecipient";

  private static final String MORTGAGEE_LOV = "MORTGAGEE_BANK";
  private static final String SEGMENT_LOV = "MARKET_SEGMENT";

  private final AdviceRecipientRepository rows;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param rows recipient set-ups
   * @param lovs lists of values (mortgagee banks, market segments)
   * @param audit audit trail
   * @param currentUser signed-in user
   * @param clock clock
   */
  public AdviceRecipientService(
      AdviceRecipientRepository rows,
      LovService lovs,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.rows = rows;
    this.lovs = lovs;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The set-ups of a company.
   *
   * @param companyId company
   * @return set-ups by bank and segment
   */
  @Transactional(readOnly = true)
  public List<AdviceRecipient> list(Long companyId) {
    return rows.findByCompanyIdOrderByMortgageeBankAscMarketSegmentAscIdAsc(companyId);
  }

  /**
   * Adds the set-up of a mortgagee bank (or of one segment of it), pending authorization.
   *
   * @param companyId company
   * @param data values
   * @return set-up
   */
  public AdviceRecipient create(Long companyId, AdviceRecipient.Data data) {
    AdviceRecipient.Data checked = check(data);
    LocalDate today = BusinessClock.today(clock);
    lovs.requireValid(MORTGAGEE_LOV, checked.mortgageeBank(), today);
    lovs.validateOptional(SEGMENT_LOV, checked.marketSegment(), today);
    AdviceRecipient row = rows.save(new AdviceRecipient(companyId, checked));
    audit.record(ENTITY, row.getId(), AuditAction.CREATE, describe(row));
    return row;
  }

  /**
   * Changes the addresses, enrolment or dates of a set-up; it must be authorized again.
   *
   * @param companyId company
   * @param id set-up
   * @param data new values
   * @return set-up
   */
  public AdviceRecipient update(Long companyId, Long id, AdviceRecipient.Data data) {
    AdviceRecipient row = row(companyId, id);
    row.update(check(data));
    audit.record(ENTITY, id, AuditAction.UPDATE, describe(row));
    return row;
  }

  /**
   * Authorizes a set-up (a checker other than its maker).
   *
   * @param companyId company
   * @param id set-up
   * @return set-up
   */
  public AdviceRecipient authorize(Long companyId, Long id) {
    AdviceRecipient row = row(companyId, id);
    row.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, id, AuditAction.AUTHORIZE, describe(row));
    return row;
  }

  /**
   * Deactivates a set-up.
   *
   * @param companyId company
   * @param id set-up
   * @return set-up
   */
  public AdviceRecipient deactivate(Long companyId, Long id) {
    AdviceRecipient row = row(companyId, id);
    row.deactivate();
    audit.record(ENTITY, id, AuditAction.DEACTIVATE, describe(row));
    return row;
  }

  /**
   * The set-up that applies to an advice: the authorized set-up in force of the segment of the
   * account, else the one of the bank as a whole.
   *
   * @param companyId company
   * @param mortgageeBank mortgagee bank of the account
   * @param marketSegment market segment of the account, may be null
   * @return set-up, empty when the bank has none
   */
  @Transactional(readOnly = true)
  public Optional<AdviceRecipient> applicable(
      Long companyId, String mortgageeBank, String marketSegment) {
    LocalDate today = BusinessClock.today(clock);
    return rows.findByCompanyIdAndMortgageeBank(companyId, mortgageeBank).stream()
        .filter(r -> r.inForce(today))
        .filter(r -> r.getMarketSegment() == null || r.getMarketSegment().equals(marketSegment))
        .min(Comparator.comparing(r -> r.getMarketSegment() == null ? 1 : 0));
  }

  /**
   * Name of a mortgagee bank for messages.
   *
   * @param code mortgagee bank code
   * @return label
   */
  @Transactional(readOnly = true)
  public String bankName(String code) {
    return lovs.label(MORTGAGEE_LOV, code);
  }

  private AdviceRecipient row(Long companyId, Long id) {
    return rows.findById(id)
        .filter(r -> r.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException("Insurance Advice recipient", id));
  }

  private static AdviceRecipient.Data check(AdviceRecipient.Data data) {
    if (blank(data.mortgageeBank()) || data.effectiveFrom() == null) {
      throw new BusinessRuleException(
          "IA_RECIPIENT_INCOMPLETE", "Choose the mortgagee bank and the start date");
    }
    return new AdviceRecipient.Data(
        data.mortgageeBank().strip(),
        blank(data.marketSegment()) ? null : data.marketSegment().strip(),
        addresses(data.to()),
        addresses(data.cc()),
        data.autoSend(),
        data.effectiveFrom(),
        data.effectiveTo());
  }

  private static List<String> addresses(List<String> raw) {
    List<String> cleaned =
        raw.stream().filter(Objects::nonNull).map(String::strip).filter(a -> !a.isEmpty()).toList();
    cleaned.stream()
        .filter(a -> !EmailAddresses.isValid(a))
        .findFirst()
        .ifPresent(
            a -> {
              throw new BusinessRuleException(
                  "IA_RECIPIENT_EMAIL", a + " is not a valid e-mail address");
            });
    return cleaned;
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }

  private static String describe(AdviceRecipient r) {
    return "Insurance Advice recipient of "
        + r.getMortgageeBank()
        + (r.getMarketSegment() == null ? "" : " / " + r.getMarketSegment())
        + ": "
        + String.join(", ", r.getTo())
        + (r.isAutoSend() ? " (automatic sending)" : "");
  }
}

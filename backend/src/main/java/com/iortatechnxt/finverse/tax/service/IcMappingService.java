package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.tax.domain.IcLineItem;
import com.iortatechnxt.finverse.tax.domain.IcLineItemRepository;
import com.iortatechnxt.finverse.tax.domain.IcSchedule;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintenance of the Insurance Commission schedule mapping (maker-checker, audited): which ledger
 * accounts (code range or report group) make up each line of each statutory schedule, and the RBC
 * factors.
 */
@Service
@Transactional
public class IcMappingService {

  /** Audit entity name. */
  public static final String ENTITY = "IcLineItem";

  private final IcLineItemRepository items;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items repository
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public IcMappingService(
      IcLineItemRepository items, AuditTrailService audit, CurrentUser currentUser, Clock clock) {
    this.items = items;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists the mapping of a company.
   *
   * @param companyId company
   * @return lines ordered by schedule and line order
   */
  @Transactional(readOnly = true)
  public List<IcLineItem> list(Long companyId) {
    return items.findByCompanyIdOrderByScheduleAscLineOrderAsc(companyId);
  }

  /**
   * Gets a line.
   *
   * @param id id
   * @return line
   */
  @Transactional(readOnly = true)
  public IcLineItem get(Long id) {
    return items.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Creates a line (pending authorization).
   *
   * @param c values
   * @return line
   */
  public IcLineItem create(IcLineCommand c) {
    if (items.existsByCompanyIdAndScheduleAndLineCode(c.companyId(), c.schedule(), c.lineCode())) {
      throw new DuplicateResourceException(ENTITY, c.schedule() + "/" + c.lineCode());
    }
    IcLineItem item = new IcLineItem(c.companyId(), c.schedule(), c.lineCode());
    apply(item, c);
    IcLineItem saved = items.save(item);
    audit.record(ENTITY, key(saved), AuditAction.CREATE, "Created IC line " + c.description());
    return saved;
  }

  /**
   * Updates a line; it returns to pending authorization.
   *
   * @param id id
   * @param c values (schedule and line code are immutable)
   * @return line
   */
  public IcLineItem update(Long id, IcLineCommand c) {
    IcLineItem item = get(id);
    apply(item, c);
    item.markModified();
    audit.record(ENTITY, key(item), AuditAction.UPDATE, "Updated IC line");
    return item;
  }

  /**
   * Authorizes a line (checker).
   *
   * @param id id
   * @return line
   */
  public IcLineItem authorize(Long id) {
    IcLineItem item = get(id);
    item.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, key(item), AuditAction.AUTHORIZE, "Authorized IC line");
    return item;
  }

  private static void apply(IcLineItem item, IcLineCommand c) {
    String from = TaxMasterSupport.blankToNull(c.accountFrom());
    String to = TaxMasterSupport.blankToNull(c.accountTo());
    String group = TaxMasterSupport.blankToNull(c.reportGroup());
    boolean range = from != null && to != null;
    validateMapping(range, from, to, group);
    validateFactors(c);
    item.setDescription(c.description());
    item.setLineOrder(c.lineOrder());
    item.setAccountFrom(range ? from : null);
    item.setAccountTo(range ? to : null);
    item.setReportGroup(group);
    item.setNormalBalance(c.normalBalance());
    item.setSignFactor(c.signFactor());
    item.setMeasure(c.measure() == null ? c.schedule().defaultMeasure() : c.measure());
    item.setRbcFactor(c.rbcFactor());
  }

  private static void validateMapping(boolean range, String from, String to, String group) {
    if (!range && group == null) {
      throw new BusinessRuleException(
          "IC_LINE_UNMAPPED", "Map the line to an account range or a report group");
    }
    if (range && from.compareTo(to) > 0) {
      throw new BusinessRuleException("INVALID_ACCOUNT_RANGE", "Account from is after account to");
    }
  }

  private static void validateFactors(IcLineCommand c) {
    if (c.signFactor() != 1 && c.signFactor() != -1) {
      throw new BusinessRuleException("INVALID_SIGN", "Sign factor is +1 or -1");
    }
    if (c.rbcFactor() != null && c.schedule() != IcSchedule.RBC) {
      throw new BusinessRuleException(
          "RBC_FACTOR_NOT_ALLOWED", "Only RBC schedule lines carry an RBC factor");
    }
  }

  private static String key(IcLineItem item) {
    return item.getSchedule() + "/" + item.getLineCode();
  }
}

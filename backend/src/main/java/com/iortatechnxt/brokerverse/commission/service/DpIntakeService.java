package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Sanitation;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import com.iortatechnxt.brokerverse.commission.domain.DpItem.Submission;
import com.iortatechnxt.brokerverse.commission.domain.DpItemRepository;
import com.iortatechnxt.brokerverse.commission.domain.DpList;
import com.iortatechnxt.brokerverse.commission.domain.DpList.FileKey;
import com.iortatechnxt.brokerverse.commission.domain.DpList.Origin;
import com.iortatechnxt.brokerverse.commission.domain.DpListRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Intake of direct payment lists (CMRID.001/002/008/013): opens a list per file (a file already
 * taken in is refused), takes each account in with its validation and sanitation, and closes the
 * list with its counts; the reviewer confirms valid accounts as fully paid to the insurer ("DP for
 * billing") or excludes them, and can validate an account again after the ledger changed.
 */
@Service
@Transactional
public class DpIntakeService {

  /** Module name on movements, files and hand-offs. */
  public static final String MODULE = "COMMISSION";

  private static final String LIST = "DpList";
  private static final String ITEM = "DpItem";

  private final DpListRepository lists;
  private final DpItemRepository items;
  private final DpValidator validator;
  private final InvoiceLedgerQueryService ledger;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param lists DP lists
   * @param items DP accounts
   * @param validator validation and sanitation
   * @param ledger Operations ledger
   * @param numbers list numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public DpIntakeService(
      DpListRepository lists,
      DpItemRepository items,
      DpValidator validator,
      InvoiceLedgerQueryService ledger,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.lists = lists;
    this.items = items;
    this.validator = validator;
    this.ledger = ledger;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Opens a list.
   *
   * @param companyId company
   * @param origin source, branch and submission date
   * @param file file name and checksum
   * @return the list
   */
  public DpList open(Long companyId, Origin origin, FileKey file) {
    if (file.sha256() != null) {
      lists
          .findFirstByCompanyIdAndSha256(companyId, file.sha256())
          .ifPresent(
              earlier -> {
                throw new BusinessRuleException(
                    "DP_LIST_DUPLICATE",
                    file.fileName() + " was already taken in as " + earlier.getListNo());
              });
    }
    DpList list =
        lists.save(
            new DpList(
                companyId, numbers.next("DPL-" + origin.submissionDate().getYear()), origin, file));
    audit.record(
        LIST,
        list.getListNo(),
        AuditAction.CREATE,
        origin.source() + " list of " + origin.branchCode() + " for " + origin.submissionDate());
    return list;
  }

  /**
   * Takes one account of a list in, validated and sanitised.
   *
   * @param listId list
   * @param rowNo row
   * @param submission submitted fields
   * @return the account's tag and id
   */
  public String add(Long listId, int rowNo, Submission submission) {
    DpList list = requireList(listId);
    DpItem item = items.save(new DpItem(list.getCompanyId(), listId, rowNo, submission));
    validator.validate(item);
    return item.getTag() + " #" + item.getId();
  }

  /**
   * Closes a list with its counts.
   *
   * @param listId list
   * @param runNo flow-in run
   * @return the list
   */
  public DpList finish(Long listId, String runNo) {
    DpList list = requireList(listId);
    List<DpItem> all = items.findByListIdOrderByRowNoAscIdAsc(listId);
    long valid = all.stream().filter(i -> i.getSanitation() == Sanitation.VALID).count();
    list.counted(runNo, all.size(), (int) valid, all.size() - (int) valid);
    audit.record(
        LIST,
        list.getListNo(),
        AuditAction.UPDATE,
        all.size() + " account(s), " + valid + " valid");
    return list;
  }

  /**
   * The company of an invoice (lists received without a company context).
   *
   * @param invoiceNo invoice
   * @return company, empty when the invoice is not booked
   */
  @Transactional(readOnly = true)
  public Optional<Long> companyOf(String invoiceNo) {
    return ledger.find(invoiceNo).map(OpsInvoice::getCompanyId);
  }

  /**
   * Confirms accounts as fully paid to the insurer: they become "DP for billing" (CMRID.013).
   *
   * @param ids accounts
   * @return the accounts
   */
  public List<DpItem> confirm(List<Long> ids) {
    List<DpItem> done = new ArrayList<>();
    for (Long id : ids) {
      DpItem item = require(id);
      item.confirm(currentUser.username(), clock.instant());
      audit.record(ITEM, id, AuditAction.UPDATE, "Confirmed for billing: " + item.getInvoiceNo());
      done.add(item);
    }
    return done;
  }

  /**
   * Excludes accounts by hand.
   *
   * @param ids accounts
   * @param reason reason
   * @return the accounts
   */
  public List<DpItem> exclude(List<Long> ids, String reason) {
    List<DpItem> done = new ArrayList<>();
    for (Long id : ids) {
      DpItem item = require(id);
      item.exclude(reason);
      audit.record(ITEM, id, AuditAction.UPDATE, "Excluded: " + reason);
      done.add(item);
    }
    return done;
  }

  /**
   * Validates an account again (after the ledger or another list changed).
   *
   * @param id account
   * @return the account
   */
  public DpItem revalidate(Long id) {
    DpItem item = require(id);
    if (item.getTag() != DpTag.DP_FOR_CONFIRMATION && item.getTag() != DpTag.EXCLUDED) {
      throw new BusinessRuleException(
          "DP_ITEM_STATE", "Account " + item.getInvoiceNo() + " is already " + item.getTag());
    }
    validator.validate(item);
    audit.record(ITEM, id, AuditAction.UPDATE, "Validated again: " + item.getSanitation());
    return item;
  }

  /**
   * An account.
   *
   * @param id account
   * @return account
   */
  @Transactional(readOnly = true)
  public DpItem require(Long id) {
    return items.findById(id).orElseThrow(() -> new ResourceNotFoundException(ITEM, id));
  }

  /**
   * A list.
   *
   * @param id list
   * @return list
   */
  @Transactional(readOnly = true)
  public DpList requireList(Long id) {
    return lists.findById(id).orElseThrow(() -> new ResourceNotFoundException(LIST, id));
  }

  /**
   * Accounts of a company (validation and sanitation list).
   *
   * @param companyId company
   * @param filter filters
   * @param pageable page
   * @return accounts
   */
  @Transactional(readOnly = true)
  public Page<DpItem> items(Long companyId, DpItemFilter filter, Pageable pageable) {
    return items.findAll(filter.specification(companyId), pageable);
  }

  /**
   * Accounts of a company per tag.
   *
   * @param companyId company
   * @return tag to count
   */
  @Transactional(readOnly = true)
  public Map<DpTag, Long> countsByTag(Long companyId) {
    Map<DpTag, Long> counts = new EnumMap<>(DpTag.class);
    for (DpTag tag : DpTag.values()) {
      counts.put(tag, items.countByCompanyIdAndTag(companyId, tag));
    }
    return counts;
  }
}

package com.iortatechnxt.brokerverse.collections.common.service;

import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItemRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read and edit contract of the collection items for every Collections sub-module (worklist,
 * dispositions, plans and escalation, unapplied payments; COLLECTIONS_DESIGN 12): look-ups by
 * invoice and client, and the edit guard of the record lock "&lt;user&gt; is editing" (NFR).
 */
@Service
@Transactional(readOnly = true)
public class CollectionItems {

  /** Entity name of an item in the audit trail and the change log. */
  public static final String ENTITY = "CollectionItem";

  private final CollectionItemRepository items;
  private final ClxSettings settings;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items items
   * @param settings parameters
   * @param currentUser signed-in user
   * @param clock clock
   */
  public CollectionItems(
      CollectionItemRepository items, ClxSettings settings, CurrentUser currentUser, Clock clock) {
    this.items = items;
    this.settings = settings;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The item of an invoice.
   *
   * @param invoiceNo invoice
   * @return item
   */
  public CollectionItem require(String invoiceNo) {
    return find(invoiceNo).orElseThrow(() -> new ResourceNotFoundException(ENTITY, invoiceNo));
  }

  /**
   * The item of an invoice, if it was ever listed.
   *
   * @param invoiceNo invoice
   * @return item
   */
  public Optional<CollectionItem> find(String invoiceNo) {
    return items.findFirstByInvoiceNo(invoiceNo);
  }

  /**
   * Items of several invoices of a company.
   *
   * @param companyId company
   * @param invoiceNos invoices
   * @return items found
   */
  public List<CollectionItem> of(Long companyId, Collection<String> invoiceNos) {
    return items.findByCompanyIdAndInvoiceNoIn(companyId, invoiceNos);
  }

  /**
   * Items of a client (client view, BRCLXN.003).
   *
   * @param companyId company
   * @param clientCode client
   * @return items, newest booking first
   */
  public List<CollectionItem> ofClient(Long companyId, String clientCode) {
    return items.ofClient(companyId, clientCode);
  }

  /**
   * The item of an invoice, locked in the database for a change, refusing the change while another
   * user holds the edit lock (NFR "&lt;user&gt; is editing").
   *
   * @param invoiceNo invoice
   * @return item
   */
  @Transactional
  public CollectionItem requireEditable(String invoiceNo) {
    CollectionItem item = requireForUpdate(invoiceNo);
    if (item.lockedByOther(currentUser.username(), clock.instant(), settings.editLock())) {
      throw new BusinessRuleException(
          "CLX_ITEM_LOCKED", item.getEditingBy() + " is editing " + invoiceNo);
    }
    return item;
  }

  /**
   * The item of an invoice, locked in the database for a change by the system (refresh, inbox),
   * which is not held back by a user's edit lock.
   *
   * @param invoiceNo invoice
   * @return item
   */
  @Transactional
  public CollectionItem requireForUpdate(String invoiceNo) {
    return items
        .lockByInvoiceNo(invoiceNo)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, invoiceNo));
  }
}

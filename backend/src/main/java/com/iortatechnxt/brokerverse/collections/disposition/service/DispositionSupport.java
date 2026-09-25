package com.iortatechnxt.brokerverse.collections.disposition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Checks shared by the collector's work on accounts (dispositions and efforts, BRCLXN.016-023,
 * 051): the account selection (several accounts need the bulk permission and get a bulk reference),
 * the accounts that can be worked on, the signed-in user and the business date.
 */
@Component
public class DispositionSupport {

  /** Most accounts in one bulk action. */
  static final int MAX_ACCOUNTS = 500;

  private static final Set<ItemStatus> WORKABLE = Set.of(ItemStatus.OPEN, ItemStatus.CREDIT);

  private final CurrentUser currentUser;
  private final DocumentNumberService numbers;
  private final ObjectMapper mapper;
  private final Clock clock;

  /**
   * Creates the helper.
   *
   * @param currentUser signed-in user
   * @param numbers bulk references
   * @param mapper JSON mapper
   * @param clock clock
   */
  public DispositionSupport(
      CurrentUser currentUser, DocumentNumberService numbers, ObjectMapper mapper, Clock clock) {
    this.currentUser = currentUser;
    this.numbers = numbers;
    this.mapper = mapper;
    this.clock = clock;
  }

  /**
   * The business date.
   *
   * @return today
   */
  public LocalDate today() {
    return LocalDate.now(clock);
  }

  /**
   * The signed-in user.
   *
   * @return user name
   */
  public String username() {
    return currentUser.username();
  }

  /**
   * The accounts of an action, without duplicates; several need the bulk permission (BRCLXN.051).
   *
   * @param invoiceNos selected invoices
   * @param bulkPermission permission for several accounts
   * @return invoices
   */
  public List<String> accounts(List<String> invoiceNos, String bulkPermission) {
    Set<String> unique =
        invoiceNos.stream()
            .filter(no -> no != null && !no.isBlank())
            .map(String::strip)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (unique.isEmpty()) {
      throw new BusinessRuleException("CLX_NO_ACCOUNT", "Select at least one account");
    }
    if (unique.size() > MAX_ACCOUNTS) {
      throw new BusinessRuleException(
          "CLX_TOO_MANY_ACCOUNTS", "Select at most " + MAX_ACCOUNTS + " accounts at once");
    }
    if (unique.size() > 1 && !currentUser.hasAuthority(bulkPermission)) {
      throw new BusinessRuleException(
          "CLX_BULK_NOT_ALLOWED", "Updating several accounts at once needs " + bulkPermission);
    }
    return List.copyOf(unique);
  }

  /**
   * The bulk reference of an action on several accounts.
   *
   * @param count accounts
   * @param today business date
   * @return reference, null for one account
   */
  public String bulkRef(int count, LocalDate today) {
    return count > 1 ? numbers.next("CLXBU-" + today.getYear()) : null;
  }

  /**
   * Refuses accounts of another company or that are closed.
   *
   * @param item item
   * @param companyId company of the request
   * @return the item
   */
  public CollectionItem workable(CollectionItem item, Long companyId) {
    if (!item.getCompanyId().equals(companyId)) {
      throw new BusinessRuleException(
          "CLX_ITEM_OTHER_COMPANY", item.getInvoiceNo() + " belongs to another company");
    }
    if (!WORKABLE.contains(item.getStatus())) {
      throw new BusinessRuleException(
          "CLX_ITEM_CLOSED", item.getInvoiceNo() + " is " + item.getStatus());
    }
    return item;
  }

  /**
   * The details of a disposition as JSON.
   *
   * @param details details
   * @return JSON, null when empty
   */
  public String json(Map<String, String> details) {
    if (details.isEmpty()) {
      return null;
    }
    try {
      return mapper.writeValueAsString(details);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Details cannot be written", ex);
    }
  }
}

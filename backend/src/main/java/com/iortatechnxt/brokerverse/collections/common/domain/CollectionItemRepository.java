package com.iortatechnxt.brokerverse.collections.common.domain;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** Collection items (BRCLXN.001, 022). */
public interface CollectionItemRepository
    extends JpaRepository<CollectionItem, Long>, JpaSpecificationExecutor<CollectionItem> {

  /**
   * The item of an invoice.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @return item
   */
  Optional<CollectionItem> findByCompanyIdAndInvoiceNo(Long companyId, String invoiceNo);

  /**
   * The item of an invoice in any company (invoice numbers are unique in the ledger).
   *
   * @param invoiceNo invoice
   * @return item
   */
  Optional<CollectionItem> findFirstByInvoiceNo(String invoiceNo);

  /**
   * The item of an invoice, locked for an update.
   *
   * @param invoiceNo invoice
   * @return item
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from CollectionItem i where i.invoiceNo = :invoiceNo")
  Optional<CollectionItem> lockByInvoiceNo(String invoiceNo);

  /**
   * Items of several invoices.
   *
   * @param companyId company
   * @param invoiceNos invoices
   * @return items
   */
  List<CollectionItem> findByCompanyIdAndInvoiceNoIn(Long companyId, Collection<String> invoiceNos);

  /**
   * Items of a client, newest booking first (client view, BRCLXN.003).
   *
   * @param companyId company
   * @param clientCode client
   * @return items
   */
  @Query(
      "select i from CollectionItem i where i.companyId = :companyId"
          + " and i.parties.clientCode = :clientCode"
          + " order by i.classification.bookingDate desc, i.id desc")
  List<CollectionItem> ofClient(Long companyId, String clientCode);

  /**
   * Items in a status without a handler (default assignment).
   *
   * @param companyId company
   * @param status status
   * @return items
   */
  List<CollectionItem> findByCompanyIdAndStatusAndCurrentHandlerIsNullOrderByIdAsc(
      Long companyId, ItemStatus status);

  /**
   * Counts the items of a handler in a status.
   *
   * @param companyId company
   * @param handler handler
   * @param status status
   * @return count
   */
  long countByCompanyIdAndCurrentHandlerIgnoreCaseAndStatus(
      Long companyId, String handler, ItemStatus status);

  /**
   * Counts items in a status.
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, ItemStatus status);

  /**
   * Counts open items with a disposition.
   *
   * @param companyId company
   * @param status status
   * @param code disposition code
   * @return count
   */
  long countByCompanyIdAndStatusAndDispositionCode(Long companyId, ItemStatus status, String code);
}

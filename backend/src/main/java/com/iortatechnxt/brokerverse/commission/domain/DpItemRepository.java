package com.iortatechnxt.brokerverse.commission.domain;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Direct payment accounts. */
public interface DpItemRepository
    extends JpaRepository<DpItem, Long>, JpaSpecificationExecutor<DpItem> {

  /**
   * Accounts of a billing.
   *
   * @param billingId billing
   * @return accounts
   */
  List<DpItem> findByBillingIdOrderByIdAsc(Long billingId);

  /**
   * Accounts of a list.
   *
   * @param listId list
   * @return accounts in row order
   */
  List<DpItem> findByListIdOrderByRowNoAscIdAsc(Long listId);

  /**
   * Accounts of an invoice (invoice 360).
   *
   * @param invoiceNo invoice
   * @return accounts, newest first
   */
  List<DpItem> findByInvoiceNoOrderByIdDesc(String invoiceNo);

  /**
   * Another active account of the same invoice (consolidation without duplicates, CMRID.001).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param tags active tags
   * @param id this account
   * @return the first other account
   */
  Optional<DpItem> findFirstByCompanyIdAndInvoiceNoAndTagInAndIdNotOrderByIdAsc(
      Long companyId, String invoiceNo, Collection<DpTag> tags, Long id);

  /**
   * Accounts of a company with a tag, oldest first.
   *
   * @param companyId company
   * @param tag tag
   * @return accounts
   */
  List<DpItem> findByCompanyIdAndTagOrderByIdAsc(Long companyId, DpTag tag);

  /**
   * Accounts of a company with a tag.
   *
   * @param companyId company
   * @param tag tag
   * @return count
   */
  long countByCompanyIdAndTag(Long companyId, DpTag tag);

  /**
   * OR numbers of the commission collected from an insurer (certificate tagging, CMRID.015).
   *
   * @param companyId company
   * @param insurerCode insurer
   * @return OR numbers with their collected amounts
   */
  @Query(
      "select i.orNo, sum(i.collectedAmount) from DpItem i where i.companyId = :companyId"
          + " and i.insurerCode = :insurer and i.orNo is not null group by i.orNo order by i.orNo")
  List<Object[]> receiptsOf(
      @Param("companyId") Long companyId, @Param("insurer") String insurerCode);
}

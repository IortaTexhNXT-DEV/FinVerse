package com.iortatechnxt.brokerverse.issuance.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Insurance Advices. */
public interface InsuranceAdviceRepository extends JpaRepository<InsuranceAdvice, Long> {

  /**
   * Advices of an account, newest first.
   *
   * @param arn Account Reference Number
   * @return advices
   */
  List<InsuranceAdvice> findByArnOrderByIdDesc(String arn);

  /**
   * Advices of a client, newest first (client 360 view).
   *
   * @param clientId client
   * @return advices
   */
  List<InsuranceAdvice> findByClientIdOrderByIdDesc(Long clientId);

  /**
   * Whether an account has an advice.
   *
   * @param accountId account
   * @return true when generated
   */
  boolean existsByAccountId(Long accountId);

  /**
   * Accounts among the given ones that have an advice.
   *
   * @param accountIds accounts
   * @return ids with an advice
   */
  @Query("select distinct a.accountId from InsuranceAdvice a where a.accountId in :ids")
  Set<Long> withAdvice(@Param("ids") Collection<Long> accountIds);

  /**
   * The register: advices of a company whose number, ARN or client contains a text, newest first.
   *
   * @param companyId company
   * @param text lower-case fragment with wildcards
   * @param pageable page
   * @return advices
   */
  @Query(
      "select a from InsuranceAdvice a where a.companyId = :companyId and (lower(a.iaNo) like :text"
          + " or lower(a.arn) like :text or lower(a.clientName) like :text) order by a.id desc")
  Page<InsuranceAdvice> search(
      @Param("companyId") Long companyId, @Param("text") String text, Pageable pageable);
}

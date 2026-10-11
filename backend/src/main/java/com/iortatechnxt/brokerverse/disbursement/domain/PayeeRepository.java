package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeStage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Disbursement payees (DIS 2.2.x). */
public interface PayeeRepository
    extends JpaRepository<Payee, Long>, JpaSpecificationExecutor<Payee> {

  /**
   * A payee with its bank accounts.
   *
   * @param id id
   * @return payee
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "accounts")
  Optional<Payee> findWithAccountsById(Long id);

  /**
   * The payee of a party code (DIS 3.25.2 first match).
   *
   * @param companyId company
   * @param payeeCode party code
   * @return payee
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "accounts")
  Optional<Payee> findByCompanyIdAndPayeeCode(Long companyId, String payeeCode);

  /**
   * Payees with the same name, ignoring case (DIS 3.25.2 second match).
   *
   * @param companyId company
   * @param name name
   * @return payees
   */
  @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "accounts")
  @Query("select p from Payee p where p.companyId = :companyId and lower(p.name) = lower(:name)")
  List<Payee> findByName(@Param("companyId") Long companyId, @Param("name") String name);

  /**
   * Payees in some stages, oldest first (pending authorisations).
   *
   * @param stages stages
   * @return payees
   */
  List<Payee> findByStageInOrderByIdAsc(Collection<PayeeStage> stages);

  /**
   * Payees of a company in some stages.
   *
   * @param companyId company
   * @param stages stages
   * @return count
   */
  long countByCompanyIdAndStageIn(Long companyId, Collection<PayeeStage> stages);
}

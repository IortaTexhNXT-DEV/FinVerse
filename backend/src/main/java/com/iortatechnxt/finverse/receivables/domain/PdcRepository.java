package com.iortatechnxt.finverse.receivables.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link PostDatedCheque}. */
public interface PdcRepository extends JpaRepository<PostDatedCheque, Long> {

  /**
   * Lists the cheques of a company in the given statuses, by cheque date.
   *
   * @param companyId company
   * @param statuses statuses
   * @return cheques
   */
  List<PostDatedCheque> findByCompanyIdAndStatusInOrderByChequeDateAscIdAsc(
      Long companyId, Collection<PdcStatus> statuses);

  /**
   * Lists every cheque of a company, newest first.
   *
   * @param companyId company
   * @return cheques
   */
  List<PostDatedCheque> findByCompanyIdOrderByChequeDateDescIdDesc(Long companyId);

  /**
   * Lists held cheques whose cheque date has been reached.
   *
   * @param companyId company
   * @param status status
   * @param asOf date
   * @return cheques
   */
  List<PostDatedCheque> findByCompanyIdAndStatusAndChequeDateLessThanEqual(
      Long companyId, PdcStatus status, LocalDate asOf);

  /**
   * Lists the cheques received up to a date (register reports).
   *
   * @param companyId company
   * @param to received date bound
   * @return cheques
   */
  List<PostDatedCheque> findByCompanyIdAndReceivedDateLessThanEqualOrderByChequeDateAscIdAsc(
      Long companyId, LocalDate to);

  /**
   * Lists the cheques of a party.
   *
   * @param companyId company
   * @param partyId party
   * @return cheques
   */
  List<PostDatedCheque> findByCompanyIdAndPartyId(Long companyId, Long partyId);
}

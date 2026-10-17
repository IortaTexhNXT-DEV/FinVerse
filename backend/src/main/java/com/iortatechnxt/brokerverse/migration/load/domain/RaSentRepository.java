package com.iortatechnxt.brokerverse.migration.load.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Renewal advices already sent before go-live. */
public interface RaSentRepository extends JpaRepository<RaSent, Long> {

  /**
   * The live advice of a header.
   *
   * @param companyId company
   * @param legacyPolicyRef header
   * @return advice
   */
  Optional<RaSent> findByCompanyIdAndLegacyPolicyRefAndRolledBackFalse(
      Long companyId, String legacyPolicyRef);

  /**
   * Live advices of headers.
   *
   * @param companyId company
   * @param refs headers
   * @return advices
   */
  List<RaSent> findByCompanyIdAndLegacyPolicyRefInAndRolledBackFalse(
      Long companyId, Collection<String> refs);

  /**
   * Live advices of a company.
   *
   * @param companyId company
   * @return advices
   */
  List<RaSent> findByCompanyIdAndRolledBackFalseOrderByExpiryDateAsc(Long companyId);

  /**
   * Live advices of a batch.
   *
   * @param batchId batch
   * @return advices
   */
  List<RaSent> findByBatchIdAndRolledBackFalse(Long batchId);
}

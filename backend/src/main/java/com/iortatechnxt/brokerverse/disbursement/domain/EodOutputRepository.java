package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.OutputKind;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Files of end-of-day runs (DIS 2.16.1-2.16.6). */
public interface EodOutputRepository extends JpaRepository<EodOutput, Long> {

  /**
   * The outputs of a run.
   *
   * @param eodRunId run
   * @return outputs
   */
  List<EodOutput> findByEodRunIdOrderByIdAsc(Long eodRunId);

  /**
   * Whether a run already has an output of a code.
   *
   * @param eodRunId run
   * @param kind kind
   * @param code code
   * @return true when present
   */
  boolean existsByEodRunIdAndKindAndCode(Long eodRunId, OutputKind kind, String code);
}

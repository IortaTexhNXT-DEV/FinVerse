package com.iortatechnxt.brokerverse.placement.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurer returns. */
public interface InsurerReturnRepository extends JpaRepository<InsurerReturn, Long> {

  /**
   * The open (unresolved) return of an account.
   *
   * @param arn Account Reference Number
   * @return return
   */
  Optional<InsurerReturn> findFirstByArnAndResolvedAtIsNullOrderByIdDesc(String arn);

  /**
   * Returns of an account, newest first.
   *
   * @param arn ARN
   * @return returns
   */
  List<InsurerReturn> findByArnOrderByIdDesc(String arn);
}

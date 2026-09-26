package com.iortatechnxt.brokerverse.coa.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link CoaNumbering}. */
public interface CoaNumberingRepository extends JpaRepository<CoaNumbering, Long> {

  /**
   * The scheme of a parent account.
   *
   * @param companyId company
   * @param parentCode parent account code
   * @return scheme if any
   */
  Optional<CoaNumbering> findByCompanyIdAndParentCode(Long companyId, String parentCode);

  /**
   * The schemes of a company.
   *
   * @param companyId company
   * @return schemes by parent code
   */
  List<CoaNumbering> findByCompanyIdOrderByParentCode(Long companyId);
}

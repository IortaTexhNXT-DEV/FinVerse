package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Comments of the comparatives. */
public interface EbCommentRepository extends JpaRepository<EbComment, Long> {

  /**
   * Comments of a comparative.
   *
   * @param comparativeId comparative
   * @return comments, oldest first
   */
  List<EbComment> findByComparativeIdOrderByIdAsc(Long comparativeId);
}

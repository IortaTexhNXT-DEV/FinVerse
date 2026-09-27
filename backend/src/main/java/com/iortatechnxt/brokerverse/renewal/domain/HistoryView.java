package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A view of the account history of a renewal (BRRN.027 AC 4): who opened it (created by), when, and
 * which sections; the disposition is refused until the user has viewed it.
 */
@Entity
@Table(name = "rnw_history_view")
public class HistoryView extends BaseEntity {

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(nullable = false, length = 200, updatable = false)
  private String sections;

  protected HistoryView() {}

  /**
   * Records a view.
   *
   * @param candidateId candidate
   * @param sections sections shown
   */
  public HistoryView(Long candidateId, String sections) {
    this.candidateId = candidateId;
    this.sections = sections;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getSections() {
    return sections;
  }
}

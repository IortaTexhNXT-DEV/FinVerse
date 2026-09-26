package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.StatusEditStage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Instrument status edits (DIS 2.8.5). */
public interface StatusEditRepository extends JpaRepository<StatusEdit, Long> {

  /**
   * Edits in a stage, oldest first.
   *
   * @param stage stage
   * @return edits
   */
  List<StatusEdit> findByStageOrderByIdAsc(StatusEditStage stage);

  /**
   * Edits of an instrument, newest first.
   *
   * @param instrumentId instrument
   * @return edits
   */
  List<StatusEdit> findByInstrumentIdOrderByIdDesc(Long instrumentId);

  /**
   * Whether an instrument has an edit in a stage.
   *
   * @param instrumentId instrument
   * @param stage stage
   * @return true when one exists
   */
  boolean existsByInstrumentIdAndStage(Long instrumentId, StatusEditStage stage);
}

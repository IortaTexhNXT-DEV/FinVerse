package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Payment instruments (DIS 2.8, 3.26). */
public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

  /**
   * The instrument of a voucher.
   *
   * @param voucherId voucher
   * @return instrument
   */
  Optional<Instrument> findByVoucherId(Long voucherId);

  /**
   * Instruments of a mode with a number (check number, form number).
   *
   * @param mode mode
   * @param instrumentNo number
   * @return instruments
   */
  List<Instrument> findByModeAndInstrumentNo(DisbursementMode mode, String instrumentNo);

  /**
   * Instruments of a mode in some statuses.
   *
   * @param mode mode
   * @param statuses statuses
   * @return instruments, oldest first
   */
  List<Instrument> findByModeAndStatusInOrderByIdAsc(
      DisbursementMode mode, Collection<InstrumentStatus> statuses);

  /**
   * Checks printed on or before a date and still printed or released (DIS 3.26.2).
   *
   * @param mode CHECK
   * @param statuses PRINTED, RELEASED
   * @param printedOn last print date that is stale
   * @return instruments
   */
  List<Instrument> findByModeAndStatusInAndPrintedOnLessThanEqualOrderByIdAsc(
      DisbursementMode mode, Collection<InstrumentStatus> statuses, LocalDate printedOn);

  /**
   * Instruments with a bank reference (BOB voucher reference).
   *
   * @param reference reference
   * @return instruments
   */
  List<Instrument> findByReference(String reference);
}

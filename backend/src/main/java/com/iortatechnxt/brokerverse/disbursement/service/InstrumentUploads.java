package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.EventSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.Instrument;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherRepository;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentService.Change;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Instrument statuses from bank files (DIS 2.22.0, 3.26.1, 3.26.4, 3.26.7; layouts AQ09): the
 * deposited-checks file tags checks NEGOTIATED by check number, the credited-accounts file tags
 * credits to account CREDITED by DV reference, and the BOB approval report tags online banking
 * payments DEBITED by voucher reference. The amount of each row must equal the instrument amount.
 */
@Service
@Transactional
public class InstrumentUploads {

  private final InstrumentRepository instruments;
  private final VoucherRepository vouchers;
  private final InstrumentService service;

  /**
   * Creates the service.
   *
   * @param instruments instruments
   * @param vouchers vouchers
   * @param service instrument life cycle
   */
  public InstrumentUploads(
      InstrumentRepository instruments, VoucherRepository vouchers, InstrumentService service) {
    this.instruments = instruments;
    this.vouchers = vouchers;
    this.service = service;
  }

  /**
   * A check of the deposited-checks file (DIS 3.26.1).
   *
   * @param checkNo check number
   * @param amount amount deposited
   * @param note deposit date or bank remarks
   * @param jobNo upload
   * @return the check
   */
  public Instrument negotiated(String checkNo, BigDecimal amount, String note, String jobNo) {
    List<Instrument> found =
        instruments.findByModeAndInstrumentNo(DisbursementMode.CHECK, checkNo.strip()).stream()
            .filter(
                i ->
                    EnumSet.of(InstrumentStatus.PRINTED, InstrumentStatus.RELEASED)
                        .contains(i.getStatus()))
            .toList();
    if (found.size() != 1) {
      throw new BusinessRuleException(
          "CHECK_NOT_FOUND",
          "No printed or released check " + checkNo + " (" + found.size() + " found)");
    }
    Instrument check = amountMatches(found.get(0), amount);
    return service.negotiated(check, new Change(EventSource.UPLOAD, note, jobNo));
  }

  /**
   * A credit of the credited-accounts file (DIS 3.26.4).
   *
   * @param dvNo DV number (DCTF reference)
   * @param amount amount credited
   * @param jobNo upload
   * @return the credit
   */
  public Instrument credited(String dvNo, BigDecimal amount, String jobNo) {
    Instrument i = byDv(dvNo, DisbursementMode.CTA);
    return service.advance(
        amountMatches(i, amount),
        InstrumentStatus.CREDITED,
        new Change(EventSource.UPLOAD, "Credited", jobNo));
  }

  /**
   * An approved transaction of the BOB approval report (DIS 3.26.7).
   *
   * @param dvNo voucher reference
   * @param amount amount
   * @param bobReference BOB transaction reference
   * @param jobNo upload
   * @return the payment
   */
  public Instrument bobApproved(String dvNo, BigDecimal amount, String bobReference, String jobNo) {
    Instrument i = amountMatches(byDv(dvNo, DisbursementMode.ONLINE_BANKING), amount);
    if (bobReference != null && !bobReference.isBlank()) {
      i.referenced(bobReference.strip());
    }
    return service.advance(
        i, InstrumentStatus.DEBITED, new Change(EventSource.UPLOAD, "BOB approved", jobNo));
  }

  private Instrument byDv(String dvNo, DisbursementMode mode) {
    Voucher v =
        vouchers
            .findByDvNo(dvNo.strip())
            .orElseThrow(() -> new BusinessRuleException("DV_NOT_FOUND", "No DV " + dvNo));
    Instrument i = service.forVoucher(v.getId());
    if (i.getMode() != mode) {
      throw new BusinessRuleException(
          "INSTRUMENT_MODE", "DV " + dvNo + " is paid by " + i.getMode() + ", not " + mode);
    }
    return i;
  }

  private static Instrument amountMatches(Instrument i, BigDecimal amount) {
    if (amount == null || amount.compareTo(i.getAmount()) != 0) {
      throw new BusinessRuleException(
          "INSTRUMENT_AMOUNT",
          "Amount "
              + amount
              + " differs from "
              + i.getAmount().toPlainString()
              + " of "
              + i.label());
    }
    return i;
  }
}

package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.Instrument;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentEvent;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentLifecycle;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount;
import com.iortatechnxt.brokerverse.disbursement.domain.StatusEdit;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherTag;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.service.BankAccountQueryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The voucher page (DIS 2.7.3; design 11): the voucher with its request, paying and payee accounts,
 * what is still missing, the instrument with its status history and edits, and the tags.
 */
@Service
@Transactional(readOnly = true)
public class VoucherViewService {

  private final VoucherService vouchers;
  private final RequestIntakeService requests;
  private final InstrumentRepository instruments;
  private final InstrumentService instrumentService;
  private final StatusEditService edits;
  private final TagService tags;
  private final BankAccountQueryService banks;

  /**
   * Creates the service.
   *
   * @param vouchers vouchers
   * @param requests payment requests
   * @param instruments instruments
   * @param instrumentService instrument history
   * @param edits status edits
   * @param tags tags
   * @param banks bank accounts
   */
  public VoucherViewService(
      VoucherService vouchers,
      RequestIntakeService requests,
      InstrumentRepository instruments,
      InstrumentService instrumentService,
      StatusEditService edits,
      TagService tags,
      BankAccountQueryService banks) {
    this.vouchers = vouchers;
    this.requests = requests;
    this.instruments = instruments;
    this.instrumentService = instrumentService;
    this.edits = edits;
    this.tags = tags;
    this.banks = banks;
  }

  /**
   * The page of a voucher.
   *
   * @param id voucher
   * @return view
   */
  public VoucherView view(Long id) {
    Voucher v = vouchers.get(id);
    Instrument i = instruments.findByVoucherId(id).orElse(null);
    return new VoucherView(
        v,
        requests.get(v.getRequestId()),
        v.getBankAccountId() == null ? null : banks.get(v.getBankAccountId()),
        vouchers.payeeAccount(v),
        vouchers.missing(v),
        i,
        i == null ? List.of() : instrumentService.history(i.getId()),
        i == null ? List.of() : edits.of(i.getId()),
        i == null ? List.of() : InstrumentLifecycle.statusesOf(i.getMode()),
        tags.of(id));
  }

  /**
   * Everything the voucher page shows.
   *
   * @param voucher voucher with its lines
   * @param request payment request
   * @param bank paying account, may be null
   * @param payeeAccount payee account, may be null
   * @param missing fields still missing
   * @param instrument instrument, null before approval
   * @param history instrument status history
   * @param edits instrument status edits
   * @param statuses statuses of the instrument's mode (for status edits)
   * @param tags OR / AR and CWT tags
   */
  public record VoucherView(
      Voucher voucher,
      IntakeRequest request,
      BankAccount bank,
      PayeeAccount payeeAccount,
      List<String> missing,
      Instrument instrument,
      List<InstrumentEvent> history,
      List<StatusEdit> edits,
      List<InstrumentStatus> statuses,
      List<VoucherTag> tags) {}
}

package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherTag;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherTag.CwtTag;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherTag.ReceiptTag;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherTagRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OR / AR and CWT tagging of approved vouchers (DIS 2.10.0-2.11.2): the official or acknowledgement
 * receipt received from the payee, and the creditable withholding tax certificates received from
 * insurers (commission, incentives) or released to suppliers (BDOI's BIR 2307). The
 * received-certificate register of {@code tax} and the insurer OR uploaded in remittance are parked
 * (AQ16, AQ17): the tag keeps the certificate reference as text.
 */
@Service
@Transactional
public class TagService {

  private final VoucherService vouchers;
  private final VoucherTagRepository tags;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param vouchers vouchers
   * @param tags tags
   * @param audit audit trail
   */
  public TagService(VoucherService vouchers, VoucherTagRepository tags, AuditTrailService audit) {
    this.vouchers = vouchers;
    this.tags = tags;
    this.audit = audit;
  }

  /**
   * Tags the OR / AR received for an approved voucher (DIS 2.10.2).
   *
   * @param voucherId voucher
   * @param receipt receipt
   * @return tag
   */
  public VoucherTag receipt(Long voucherId, ReceiptTag receipt) {
    Voucher v = approved(voucherId);
    VoucherTag tag = tags.save(VoucherTag.receipt(v.getId(), receipt));
    audit.record(
        DisbursementSettings.VOUCHER,
        v.getDvNo(),
        AuditAction.UPDATE,
        "OR / AR " + receipt.receiptNo() + " received " + receipt.receivedOn());
    return tag;
  }

  /**
   * Tags a CWT certificate received or released (DIS 2.11.1-2.11.2).
   *
   * @param voucherId voucher
   * @param cwt certificate
   * @return tag
   */
  public VoucherTag cwt(Long voucherId, CwtTag cwt) {
    Voucher v = approved(voucherId);
    if (cwt.periodFrom() != null
        && cwt.periodTo() != null
        && cwt.periodTo().isBefore(cwt.periodFrom())) {
      throw new BusinessRuleException("CWT_PERIOD", "The period covered ends before it starts");
    }
    VoucherTag tag = tags.save(VoucherTag.cwt(v.getId(), cwt));
    audit.record(
        DisbursementSettings.VOUCHER,
        v.getDvNo(),
        AuditAction.UPDATE,
        "CWT " + cwt.direction() + " " + cwt.certificateNo() + " " + cwt.amount());
    return tag;
  }

  /**
   * The tags of a voucher.
   *
   * @param voucherId voucher
   * @return tags, oldest first
   */
  @Transactional(readOnly = true)
  public List<VoucherTag> of(Long voucherId) {
    return tags.findByVoucherIdOrderByIdAsc(voucherId);
  }

  private Voucher approved(Long voucherId) {
    Voucher v = vouchers.get(voucherId);
    if (v.getStage() != VoucherStage.APPROVED) {
      throw new BusinessRuleException(
          "DV_NOT_APPROVED",
          "Only an approved DV is tagged; DV " + v.getDvNo() + " is " + v.getStage());
    }
    return v;
  }
}

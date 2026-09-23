package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.payables.domain.BankAccount;
import com.iortatechnxt.finverse.payables.domain.NotificationFormat;
import com.iortatechnxt.finverse.payables.domain.PaymentMode;
import com.iortatechnxt.finverse.payables.domain.PaymentVoucher;
import com.iortatechnxt.finverse.payables.domain.PaymentVoucherRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment notification to the bank (FIN-BRS-PAYNOTIFY): the approved payments of one bank account
 * in a date range, written in the bank's layout (see {@link PaymentNotificationFormatter}).
 *
 * <p>By default only bank transfers are advised (the bank executes them); with {@code
 * includeCheques} cheques and PDCs are added so the bank can verify presented cheques (positive
 * pay).
 */
@Service
@Transactional(readOnly = true)
public class PaymentNotificationService {

  private static final int MAX_DAYS = 31;

  private final PaymentVoucherRepository vouchers;
  private final BankAccountQueryService banks;
  private final PartyService parties;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param vouchers vouchers
   * @param banks bank accounts
   * @param parties party master
   * @param audit audit trail
   */
  public PaymentNotificationService(
      PaymentVoucherRepository vouchers,
      BankAccountQueryService banks,
      PartyService parties,
      AuditTrailService audit) {
    this.vouchers = vouchers;
    this.banks = banks;
    this.parties = parties;
    this.audit = audit;
  }

  /**
   * Collects the payments to advise.
   *
   * @param bankAccountId bank account
   * @param from from date
   * @param to to date
   * @param includeCheques also advise cheques and PDCs
   * @return records by date and voucher number
   */
  public List<NotificationRecord> records(
      Long bankAccountId, LocalDate from, LocalDate to, boolean includeCheques) {
    if (to.isBefore(from) || from.plusDays(MAX_DAYS).isBefore(to)) {
      throw new BusinessRuleException(
          "INVALID_DATE_RANGE", "Choose a date range of at most " + MAX_DAYS + " days");
    }
    Set<PaymentMode> modes =
        includeCheques ? EnumSet.allOf(PaymentMode.class) : EnumSet.of(PaymentMode.BANK_TRANSFER);
    return vouchers.findApproved(bankAccountId, modes, from, to).stream()
        .map(this::record)
        .toList();
  }

  /**
   * Generates the file.
   *
   * @param bankAccountId bank account
   * @param from from date
   * @param to to date (also the file date)
   * @param includeCheques also advise cheques and PDCs
   * @param format layout override, null = the bank account's layout
   * @return file
   */
  @Transactional
  public NotificationFile generate(
      Long bankAccountId,
      LocalDate from,
      LocalDate to,
      boolean includeCheques,
      NotificationFormat format) {
    BankAccount bank = banks.requireActive(bankAccountId);
    NotificationFormat layout = format != null ? format : bank.getNotificationFormat();
    List<NotificationRecord> records = records(bankAccountId, from, to, includeCheques);
    String text = PaymentNotificationFormatter.format(layout, bank.getAccountNo(), to, records);
    String extension = layout == NotificationFormat.CSV ? ".csv" : ".txt";
    String fileName =
        "PAYNOTIFY-" + bank.getCode() + "-" + to.toString().replace("-", "") + extension;
    audit.record(
        "PaymentNotification",
        bank.getCode(),
        AuditAction.EXPORT,
        "Generated " + fileName + " with " + records.size() + " payments");
    return new NotificationFile(
        fileName,
        layout == NotificationFormat.CSV ? "text/csv" : "text/plain",
        text.getBytes(StandardCharsets.US_ASCII),
        records.size());
  }

  private NotificationRecord record(PaymentVoucher v) {
    Party party = parties.get(v.getPartyId());
    return new NotificationRecord(
        v.getVoucherNo(),
        v.getChequeNo(),
        v.getVoucherDate(),
        v.getPartyCode(),
        v.getPayeeName(),
        party.getBankAccountNo(),
        v.getCurrency(),
        v.getAmount());
  }

  /**
   * Generated file.
   *
   * @param fileName suggested file name
   * @param contentType MIME type
   * @param content bytes (US-ASCII)
   * @param recordCount number of payments
   */
  public record NotificationFile(
      String fileName, String contentType, byte[] content, int recordCount) {}
}

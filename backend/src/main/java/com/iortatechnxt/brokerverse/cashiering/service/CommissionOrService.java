package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CommissionLine;
import com.iortatechnxt.brokerverse.cashiering.domain.CommissionLine.CommissionDetail;
import com.iortatechnxt.brokerverse.cashiering.domain.CommissionLineRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt.ReceiptTender;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptLine;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService.OrIssue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commission official receipts from the commission payment details uploaded from Collection
 * (CSHID.007, OQ49): the lines are staged by the {@code COMMISSION_PAYMENT} upload, then one OR is
 * issued per insurer, certificate and payment; a group whose lines name different payees is
 * rejected. The OR is a cash receipt: bank and creditable withholding tax against the commission
 * receivable, with the deferred VAT made due ({@code OPS_OR_ISSUE}).
 */
@Service
@Transactional
public class CommissionOrService {

  private final CommissionLineRepository lines;
  private final CashReceiptService receipts;
  private final CashieringSettings settings;

  /**
   * Creates the service.
   *
   * @param lines commission payment lines
   * @param receipts receipts
   * @param settings settings
   */
  public CommissionOrService(
      CommissionLineRepository lines, CashReceiptService receipts, CashieringSettings settings) {
    this.lines = lines;
    this.receipts = receipts;
    this.settings = settings;
  }

  /**
   * Stages an uploaded line (idempotent on job and row).
   *
   * @param companyId company
   * @param jobNo upload job
   * @param rowNo row
   * @param detail payment detail
   * @return reference of the line
   */
  public String stage(Long companyId, String jobNo, int rowNo, CommissionDetail detail) {
    if (!lines.existsByJobNoAndRowNo(jobNo, rowNo)) {
      lines.save(new CommissionLine(companyId, jobNo, rowNo, detail));
    }
    return jobNo + "/" + rowNo;
  }

  /**
   * Staged lines of a company.
   *
   * @param companyId company
   * @return lines by job and row
   */
  @Transactional(readOnly = true)
  public List<CommissionLine> staged(Long companyId) {
    return lines.findByCompanyIdAndStatusOrderByJobNoAscRowNoAsc(companyId, CommissionLine.STAGED);
  }

  /**
   * Issues the ORs of the staged lines, one per consolidation group.
   *
   * @param companyId company
   * @return OR numbers issued
   */
  public List<String> issue(Long companyId) {
    Map<String, List<CommissionLine>> groups = new LinkedHashMap<>();
    staged(companyId)
        .forEach(l -> groups.computeIfAbsent(l.groupKey(), k -> new ArrayList<>()).add(l));
    List<String> issued = new ArrayList<>();
    for (List<CommissionLine> group : groups.values()) {
      CommissionLine first = group.get(0);
      boolean samePayee =
          group.stream().allMatch(l -> l.getPayeeName().equals(first.getPayeeName()));
      if (!samePayee) {
        group.forEach(
            l ->
                l.resolve(
                    CommissionLine.REJECTED,
                    null,
                    "Payees differ within one insurer, certificate and payment"));
        continue;
      }
      Receipt or = receipts.issueOr(orOf(companyId, group));
      group.forEach(
          l ->
              l.resolve(
                  CommissionLine.ISSUED,
                  or.getReceiptNo(),
                  group.size() + " line(s) consolidated"));
      issued.add(or.getReceiptNo());
    }
    return issued;
  }

  private OrIssue orOf(Long companyId, List<CommissionLine> group) {
    CommissionLine first = group.get(0);
    List<ReceiptLine> orLines =
        group.stream()
            .map(
                l ->
                    new ReceiptLine(
                        l.getInvoiceNo(),
                        l.getInsurerCode(),
                        l.amounts(),
                        "Commission "
                            + (l.getInvoiceNo() == null ? l.getPaymentRef() : l.getInvoiceNo())))
            .toList();
    return new OrIssue(
        companyId,
        settings.headOffice(companyId).getId(),
        "COMMISSION",
        first.getPaymentDate(),
        first.getInsurerCode(),
        first.getPayeeName(),
        "PHP",
        orLines,
        new ReceiptTender(
            PaymentMode.CHECK,
            first.getPaymentRef(),
            null,
            first.getPaymentDate(),
            first.getCertificateRef(),
            ReceiptSource.COMMISSION_UPLOAD,
            CashieringSettings.MODULE,
            "CPAY:" + first.getId(),
            "Commission payment " + first.getPaymentRef()),
        false);
  }
}

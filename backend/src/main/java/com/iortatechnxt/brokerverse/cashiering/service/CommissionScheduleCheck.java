package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowStatus;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The matching of a Commission Schedule (FRS.CSH.02.02.09): each line's reference number is looked
 * up among the booked accounts and its paid amount (basic commission plus VAT less the tax
 * withheld) compared with the commission outstanding of the account; the line shows the matched
 * reference number and outstanding balance, or "unmatched".
 */
@Component
@Transactional(readOnly = true)
public class CommissionScheduleCheck {

  /** Result of a line that does not match. */
  public static final String UNMATCHED = "unmatched";

  private final BulkService bulk;
  private final PaymentMatcher matcher;

  /**
   * Creates the check.
   *
   * @param bulk rows of the run
   * @param matcher accounts of a reference
   */
  public CommissionScheduleCheck(BulkService bulk, PaymentMatcher matcher) {
    this.bulk = bulk;
    this.matcher = matcher;
  }

  /**
   * The lines of a run that do not match.
   *
   * @param companyId company
   * @param jobId run
   * @return count of unmatched lines
   */
  public long unmatched(Long companyId, Long jobId) {
    return lines(companyId, jobId).stream().filter(l -> !l.matched()).count();
  }

  /**
   * The matched and unmatched list of a run (FRS.CSH.02.02.09.02 to 02.02.09.04).
   *
   * @param companyId company
   * @param jobId run
   * @return one line per row
   */
  public List<Line> lines(Long companyId, Long jobId) {
    List<Line> lines = new ArrayList<>();
    for (BulkRowRecord row :
        bulk.rows(jobId, BulkRowStatus.VALID, Pageable.unpaged()).getContent()) {
      lines.add(line(companyId, row.getRowNo(), bulk.values(row)));
    }
    for (BulkRowRecord row :
        bulk.rows(jobId, BulkRowStatus.COMMITTED, Pageable.unpaged()).getContent()) {
      lines.add(line(companyId, row.getRowNo(), bulk.values(row)));
    }
    return lines;
  }

  private Line line(Long companyId, int rowNo, Map<String, String> values) {
    String reference = values.getOrDefault("Invoice no", "");
    BigDecimal paid =
        ChannelFileReader.number(values.get("Basic commission"))
            .add(ChannelFileReader.number(values.get("VAT")))
            .subtract(ChannelFileReader.number(values.get("WTAX")));
    List<OpsInvoice> invoices =
        reference.isBlank() ? List.of() : matcher.invoices(companyId, reference);
    if (invoices.isEmpty()) {
      return new Line(rowNo, reference, paid, null, UNMATCHED, UNMATCHED, false);
    }
    OpsInvoice invoice = invoices.get(0);
    BigDecimal outstanding = commissionOutstanding(invoice);
    boolean equal = outstanding.compareTo(paid) == 0;
    return new Line(
        rowNo,
        reference,
        paid,
        outstanding,
        invoice.getInvoiceNo(),
        equal ? RecordValidation.formatted(outstanding) : UNMATCHED,
        equal);
  }

  /**
   * The commission still expected from the insurer on an account: commission and VAT on commission
   * less the tax withheld, as booked and not yet collected.
   *
   * @param invoice invoice with its components
   * @return outstanding
   */
  static BigDecimal commissionOutstanding(OpsInvoice invoice) {
    Map<LedgerComponent, BigDecimal> balances = invoice.balances();
    return balances
        .getOrDefault(LedgerComponent.COMMISSION, BigDecimal.ZERO)
        .add(balances.getOrDefault(LedgerComponent.COMMISSION_VAT, BigDecimal.ZERO))
        .subtract(balances.getOrDefault(LedgerComponent.WTAX, BigDecimal.ZERO));
  }

  /**
   * A line of the matched and unmatched list.
   *
   * @param rowNo row of the file
   * @param reference reference number of the line
   * @param paid paid amount
   * @param outstanding outstanding balance of the account, null when unmatched
   * @param referenceResult matched reference number or "unmatched"
   * @param amountResult outstanding balance of the matched reference or "unmatched"
   * @param matched both match
   */
  public record Line(
      int rowNo,
      String reference,
      BigDecimal paid,
      BigDecimal outstanding,
      String referenceResult,
      String amountResult,
      boolean matched) {}
}

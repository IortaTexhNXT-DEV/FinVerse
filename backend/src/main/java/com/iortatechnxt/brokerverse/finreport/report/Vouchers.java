package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.brokerverse.finreport.service.AccountNode;
import com.iortatechnxt.brokerverse.finreport.service.StatusFilter;
import com.iortatechnxt.brokerverse.finreport.service.VoucherLine;
import com.iortatechnxt.brokerverse.finreport.service.VoucherQuery;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Shared selection, ordering, combining and summaries for voucher based reports. */
final class Vouchers {

  static final String MAIN_AC = "mainAc";
  static final String SUB_AC = "subAc";
  static final String ACCOUNT_NAME = "accountName";
  static final String DIVISION = "division";
  static final String DEPARTMENT = "department";
  static final String ACTIVITY = "activity";
  static final String PARTY = "party";
  static final String CURRENCY = "currency";
  static final String FC_VALUE = "fcValue";
  static final String DEBIT = "debit";
  static final String CREDIT = "credit";

  private Vouchers() {}

  /**
   * Builds the voucher selection from the standard parameters.
   *
   * @param p parameters
   * @param h chart of accounts
   * @param status status selection
   * @return query
   */
  static VoucherQuery query(ReportParameters p, AccountHierarchy h, StatusFilter status) {
    return new VoucherQuery(
        p.longValue(FinParams.COMPANY),
        FinParams.branch(p),
        p.date(FinParams.FROM),
        p.date(FinParams.TO),
        status,
        FinReportSupport.journalTypes(p),
        p.optionalText(FinParams.DOC_FROM).orElse(null),
        p.optionalText(FinParams.DOC_TO).orElse(null),
        p.optionalText(FinParams.USER).orElse(null),
        FinReportSupport.selectedAccounts(h, p));
  }

  /**
   * Orders lines by transaction code, then document date or number, then line.
   *
   * @param lines lines
   * @param byNumber true to order by document number
   * @return sorted copy
   */
  static List<VoucherLine> sort(Collection<VoucherLine> lines, boolean byNumber) {
    Comparator<VoucherLine> cmp = Comparator.comparing(VoucherLine::transactionCode);
    if (!byNumber) {
      cmp = cmp.thenComparing(VoucherLine::valueDate);
    }
    cmp = cmp.thenComparing(VoucherLine::batchNo).thenComparingInt(VoucherLine::lineNo);
    return lines.stream().sorted(cmp).toList();
  }

  /**
   * Combines the lines of a voucher that share account, branch, cost centre, line of business,
   * party, currency and side (PREMIA "Combine Transactions").
   *
   * @param lines lines in order
   * @return combined lines (first line of each combination carries the summed amounts)
   */
  static List<VoucherLine> combine(List<VoucherLine> lines) {
    Map<List<Object>, VoucherLine> out = new LinkedHashMap<>();
    for (VoucherLine l : lines) {
      List<Object> key =
          Arrays.asList(
              l.batchId(),
              l.accountId(),
              l.branchId(),
              l.costCenter(),
              l.businessLine(),
              l.partyCode(),
              l.currency(),
              l.debit());
      out.merge(key, l, Vouchers::sum);
    }
    return new ArrayList<>(out.values());
  }

  /**
   * Account, dimension and amount cells common to voucher lines.
   *
   * @param l line
   * @param h chart of accounts
   * @param branches branch code by id
   * @return cells
   */
  static Map<String, Object> lineCells(
      VoucherLine l, AccountHierarchy h, Map<Long, String> branches) {
    Map<String, Object> cells = accountCells(h, l.accountId());
    cells.putAll(
        FinRows.cells(
            DIVISION,
            branches.get(l.branchId()),
            DEPARTMENT,
            l.costCenter(),
            ACTIVITY,
            l.businessLine(),
            PARTY,
            l.partyCode(),
            CURRENCY,
            l.currency(),
            FC_VALUE,
            l.amount(),
            DEBIT,
            l.debitBase(),
            CREDIT,
            l.creditBase()));
    return cells;
  }

  /**
   * Main account, sub account and name cells of an account.
   *
   * @param h chart of accounts
   * @param accountId account
   * @return cells
   */
  static Map<String, Object> accountCells(AccountHierarchy h, Long accountId) {
    AccountNode account = h.node(accountId);
    AccountNode main = h.mainOf(accountId);
    return FinRows.cells(
        MAIN_AC,
        main.code(),
        SUB_AC,
        Objects.equals(main.id(), account.id()) ? "" : account.code(),
        ACCOUNT_NAME,
        account.name());
  }

  /**
   * Summary row: number of vouchers and entries with debit and credit totals.
   *
   * @param kind SUBTOTAL or TOTAL
   * @param level level
   * @param caption caption, e.g. "Transaction-wise Summary JV"
   * @param lines lines summarised
   * @return row
   */
  static ReportRow summary(RowKind kind, int level, String caption, List<VoucherLine> lines) {
    long vouchers = lines.stream().map(VoucherLine::batchId).distinct().count();
    BigDecimal dr =
        lines.stream().map(VoucherLine::debitBase).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal cr =
        lines.stream().map(VoucherLine::creditBase).reduce(BigDecimal.ZERO, BigDecimal::add);
    String label =
        caption + " : Total No. of Vouchers " + vouchers + ", Total No. of Entries " + lines.size();
    return FinRows.row(kind, level, label, FinRows.cells(DEBIT, dr, CREDIT, cr));
  }

  /**
   * Groups ordered lines by a key, keeping order.
   *
   * @param lines lines
   * @param keyOf key
   * @param <K> key type
   * @return groups
   */
  static <K> Map<K, List<VoucherLine>> group(
      List<VoucherLine> lines, Function<VoucherLine, K> keyOf) {
    Map<K, List<VoucherLine>> out = new LinkedHashMap<>();
    lines.forEach(l -> out.computeIfAbsent(keyOf.apply(l), k -> new ArrayList<>()).add(l));
    return out;
  }

  /**
   * Voucher header caption.
   *
   * @param l any line of the voucher
   * @return caption
   */
  static String header(VoucherLine l) {
    StringBuilder sb = new StringBuilder();
    sb.append(l.valueDate()).append("  ").append(l.batchNo()).append("  ").append(l.narration());
    if (l.reference() != null) {
      sb.append("  Ref: ").append(l.reference());
    }
    return sb.toString();
  }

  private static VoucherLine sum(VoucherLine a, VoucherLine b) {
    return new VoucherLine(
        a.batchId(),
        a.batchNo(),
        a.journalType(),
        a.status(),
        a.valueDate(),
        a.narration(),
        a.reference(),
        a.sourceModule(),
        a.createdBy(),
        a.createdAt(),
        a.submittedBy(),
        a.authorizedBy(),
        a.authorizedAt(),
        a.lineNo(),
        a.accountId(),
        a.branchId(),
        a.debit(),
        a.currency(),
        a.amount().add(b.amount()),
        a.baseAmount().add(b.baseAmount()),
        a.costCenter(),
        a.businessLine(),
        a.partyCode(),
        a.lineReference(),
        a.lineNarration());
  }
}

package com.iortatechnxt.finverse.finreport.report;

import com.iortatechnxt.finverse.finreport.service.LedgerLine;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportRow;
import com.iortatechnxt.finverse.report.core.RowKind;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Row layout of the ledger reports: one block per account (and party / currency) with the opening
 * balance, the entries with a running balance, the control account summary, the totals and the
 * closing balance; group and grand totals for sub-ledgers.
 */
final class LedgerBlocks {

  private static final String DATE = "date";
  private static final String TC = "tc";
  private static final String DOC_NO = "docNo";
  private static final String REFERENCE = "reference";
  private static final String NARRATION = "narration";
  private static final String BALANCE = "balance";
  private static final String SIDE = "side";

  private LedgerBlocks() {}

  static List<ReportRow> writeGroups(
      Map<String, Block> blocks, boolean foreign, Map<Long, String> branches) {
    List<ReportRow> rows = new ArrayList<>();
    BigDecimal[] grand = zero();
    String control = null;
    BigDecimal[] group = zero();
    for (Block b : blocks.values().stream().filter(Block::hasContent).toList()) {
      if (!b.outer.equals(control)) {
        closeGroup(rows, control, group, foreign);
        control = b.outer;
        group = zero();
        rows.add(FinRows.label(RowKind.GROUP_HEADER, 0, "Control A/c " + control));
      }
      BigDecimal[] totals = writeBlock(rows, b, foreign, branches);
      add(group, totals);
      add(grand, totals);
    }
    closeGroup(rows, control, group, foreign);
    if (!foreign && control != null) {
      rows.add(FinRows.row(RowKind.TOTAL, 0, "** GRAND TOTAL **", totalCells(grand)));
    }
    return rows;
  }

  private static void closeGroup(
      List<ReportRow> rows, String control, BigDecimal[] group, boolean foreign) {
    if (control != null && !foreign) {
      rows.add(FinRows.row(RowKind.SUBTOTAL, 0, "** GROUP TOTAL ** " + control, totalCells(group)));
    }
  }

  private static Map<String, Object> totalCells(BigDecimal[] t) {
    return FinRows.cells(
        Vouchers.DEBIT, t[0], Vouchers.CREDIT, t[1], BALANCE, t[2], SIDE, FinRows.drCr(t[2]));
  }

  private static BigDecimal[] zero() {
    return new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
  }

  private static void add(BigDecimal[] acc, BigDecimal[] t) {
    for (int i = 0; i < acc.length; i++) {
      acc[i] = acc[i].add(t[i]);
    }
  }

  static BigDecimal[] writeBlock(
      List<ReportRow> rows, Block b, boolean foreign, Map<Long, String> branches) {
    rows.add(FinRows.label(RowKind.GROUP_HEADER, 1, b.caption));
    rows.add(FinRows.row(RowKind.DETAIL, 1, "Opening Balance", balanceCells(b.opening)));
    BigDecimal running = b.opening;
    BigDecimal dr = BigDecimal.ZERO;
    BigDecimal cr = BigDecimal.ZERO;
    for (LedgerLine l : b.lines) {
      BigDecimal d = debit(l, foreign);
      BigDecimal c = credit(l, foreign);
      running = running.add(d).subtract(c);
      dr = dr.add(d);
      cr = cr.add(c);
      Map<String, Object> cells =
          FinRows.cells(
              DATE,
              l.valueDate(),
              TC,
              l.transactionCode(),
              DOC_NO,
              l.batchNo(),
              REFERENCE,
              l.reference(),
              Vouchers.DIVISION,
              branches.get(l.branchId()),
              Vouchers.DEPARTMENT,
              l.costCenter(),
              NARRATION,
              l.narration());
      cells.putAll(amountCells(d, c, running));
      rows.add(ReportRow.detail(cells));
    }
    if (b.controlDebit.signum() != 0 || b.controlCredit.signum() != 0) {
      running = running.add(b.controlDebit).subtract(b.controlCredit);
      dr = dr.add(b.controlDebit);
      cr = cr.add(b.controlCredit);
      rows.add(
          FinRows.row(
              RowKind.DETAIL,
              1,
              "Control account summary",
              amountCells(b.controlDebit, b.controlCredit, running)));
    }
    rows.add(
        FinRows.row(
            RowKind.SUBTOTAL,
            1,
            "Total transactions",
            FinRows.cells(Vouchers.DEBIT, dr, Vouchers.CREDIT, cr)));
    rows.add(FinRows.row(RowKind.SUBTOTAL, 1, "Closing balance", balanceCells(running)));
    return new BigDecimal[] {dr, cr, running};
  }

  private static Map<String, Object> amountCells(BigDecimal d, BigDecimal c, BigDecimal running) {
    Map<String, Object> cells = FinRows.cells(Vouchers.DEBIT, d, Vouchers.CREDIT, c);
    cells.putAll(balanceCells(running));
    return cells;
  }

  private static Map<String, Object> balanceCells(BigDecimal balance) {
    return FinRows.cells(BALANCE, balance, SIDE, FinRows.drCr(balance));
  }

  static BigDecimal debit(LedgerLine l, boolean foreign) {
    return foreign ? l.debitFc() : l.debitBase();
  }

  static BigDecimal credit(LedgerLine l, boolean foreign) {
    return foreign ? l.creditFc() : l.creditBase();
  }

  static List<ReportColumn> columns() {
    return List.of(
        ReportColumn.date(DATE, "Document Date"),
        ReportColumn.text(TC, "Txn Code"),
        ReportColumn.text(DOC_NO, "Document Number"),
        ReportColumn.text(REFERENCE, "Reference Number"),
        ReportColumn.text(Vouchers.DIVISION, "Division"),
        ReportColumn.text(Vouchers.DEPARTMENT, "Department"),
        ReportColumn.text(NARRATION, "Narration"),
        ReportColumn.amount(Vouchers.DEBIT, "Debits"),
        ReportColumn.amount(Vouchers.CREDIT, "Credits"),
        ReportColumn.amountNoTotal(BALANCE, "Balance Amount"),
        ReportColumn.text(SIDE, "Dr/Cr"));
  }

  /** Mutable accumulation of one ledger block. */
  static final class Block {
    private final String caption;
    private final String outer;
    private final List<LedgerLine> lines = new ArrayList<>();
    private BigDecimal opening = BigDecimal.ZERO;
    private BigDecimal controlDebit = BigDecimal.ZERO;
    private BigDecimal controlCredit = BigDecimal.ZERO;

    Block(String caption, String outer) {
      this.caption = caption;
      this.outer = outer;
    }

    void addOpening(BigDecimal amount) {
      opening = opening.add(amount);
    }

    void addControl(BigDecimal debit, BigDecimal credit) {
      controlDebit = controlDebit.add(debit);
      controlCredit = controlCredit.add(credit);
    }

    void addLine(LedgerLine line) {
      lines.add(line);
    }

    boolean hasContent() {
      return opening.signum() != 0
          || !lines.isEmpty()
          || controlDebit.signum() != 0
          || controlCredit.signum() != 0;
    }
  }
}

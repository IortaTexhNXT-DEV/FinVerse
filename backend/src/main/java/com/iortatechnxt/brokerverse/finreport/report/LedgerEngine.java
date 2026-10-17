package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.brokerverse.finreport.service.AccountNode;
import com.iortatechnxt.brokerverse.finreport.service.FinReportQueries;
import com.iortatechnxt.brokerverse.finreport.service.LedgerLine;
import com.iortatechnxt.brokerverse.finreport.service.LedgerQuery;
import com.iortatechnxt.brokerverse.finreport.service.MovementRow;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Engine of the four ledgers: FIN-GL-LEDGER-LC / -FC (per main account, FGL010B / FGL010A) and
 * FIN-GL-SUBLEDGER-LC / -FC (per control account and party, FGL011B / FGL011A). Each block prints
 * the opening balance (rule R-OPEN), the entries with a running balance (R-RUN), the totals and the
 * closing balance. FC ledgers are kept per currency and never add different currencies.
 */
@Component
public class LedgerEngine {

  /** Level parameter. */
  static final String LEVEL = "level";

  private static final String COMPANY_LEVEL = "COMPANY";
  private static final String DIVISION_LEVEL = "DIVISION";
  private static final String DEPARTMENT_LEVEL = "DEPARTMENT";
  private static final String PARTY_FROM = "partyFrom";
  private static final String PARTY_TO = "partyTo";
  private static final String NONE = "-";
  private static final String SEP = "|";

  private final FinReportQueries queries;
  private final FinReportSupport support;

  /**
   * Creates the engine.
   *
   * @param queries finance queries
   * @param support finance report helpers
   */
  public LedgerEngine(FinReportQueries queries, FinReportSupport support) {
    this.queries = queries;
    this.support = support;
  }

  /**
   * Parameters of the general ledgers.
   *
   * @return specs
   */
  static List<ParameterSpec> generalParameters() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(FinParams.company());
    params.add(
        ParameterSpec.select(
            LEVEL,
            "Level",
            List.of(COMPANY_LEVEL, DIVISION_LEVEL, DEPARTMENT_LEVEL),
            COMPANY_LEVEL));
    params.add(FinParams.from());
    params.add(FinParams.to());
    params.addAll(FinParams.mainRange());
    params.add(FinParams.division());
    params.add(FinParams.department());
    return params;
  }

  /**
   * Parameters of the sub-ledgers.
   *
   * @return specs
   */
  static List<ParameterSpec> subParameters() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(FinParams.company());
    params.add(FinParams.from());
    params.add(FinParams.to());
    params.addAll(FinParams.mainRange());
    params.addAll(FinParams.subRange());
    params.add(ParameterSpec.optional(PARTY_FROM, "Party (Sub A/c) Code From", ParameterType.TEXT));
    params.add(ParameterSpec.optional(PARTY_TO, "Party (Sub A/c) Code To", ParameterType.TEXT));
    params.add(FinParams.division());
    return params;
  }

  /**
   * General ledger per main account (and division / department when the level asks for it).
   *
   * @param p parameters
   * @param foreign true for transaction currency amounts (per currency)
   * @return result
   */
  public ReportResult generalLedger(ReportParameters p, boolean foreign) {
    Long companyId = p.longValue(FinParams.COMPANY);
    AccountHierarchy h = support.hierarchy(companyId);
    Map<Long, String> branches = support.branchCodes(companyId);
    String level = p.text(LEVEL);
    LedgerQuery q = FinReportSupport.dimensionQuery(p, h);
    boolean needsLedger = DEPARTMENT_LEVEL.equals(level) || q.costCenter() != null;
    List<MovementRow> openings = needsLedger ? queries.dimensionMovements(q) : queries.movements(q);
    Function<Key3, String> caption = generalCaption(h, level, foreign);
    Map<String, LedgerBlocks.Block> blocks = new TreeMap<>();
    for (MovementRow r : openings) {
      Key3 k =
          new Key3(
              h.mainOf(r.accountId()).id(),
              levelValue(level, r.branchId(), r.costCenter(), branches),
              currency(foreign, r.currency()));
      LedgerBlocks.Block b = block(blocks, h, k, caption);
      b.addOpening(foreign ? r.openFc() : r.openBase());
    }
    for (LedgerLine l : queries.ledgerLines(q, false)) {
      Key3 k =
          new Key3(
              h.mainOf(l.accountId()).id(),
              levelValue(level, l.branchId(), l.costCenter(), branches),
              currency(foreign, l.currency()));
      LedgerBlocks.Block b = block(blocks, h, k, caption);
      if (h.node(l.accountId()).controlAccount()) {
        b.addControl(LedgerBlocks.debit(l, foreign), LedgerBlocks.credit(l, foreign));
      } else {
        b.addLine(l);
      }
    }
    List<ReportRow> rows = new ArrayList<>();
    blocks.values().stream()
        .filter(LedgerBlocks.Block::hasContent)
        .forEach(b -> LedgerBlocks.writeBlock(rows, b, foreign, branches));
    return result(
        p,
        rows,
        "Balances are debit positive, credit in brackets. Postings to sub-ledger control"
            + " accounts are summarised in one Control account summary line.",
        foreign
            ? "Amounts in transaction currency, one block per currency."
            : "Amounts in local (base) currency.");
  }

  /**
   * Sub-ledger per control account and party.
   *
   * @param p parameters
   * @param foreign true for transaction currency amounts (per currency)
   * @return result
   */
  public ReportResult subLedger(ReportParameters p, boolean foreign) {
    Long companyId = p.longValue(FinParams.COMPANY);
    AccountHierarchy h = support.hierarchy(companyId);
    Map<Long, String> branches = support.branchCodes(companyId);
    LedgerQuery q =
        new LedgerQuery(
            companyId,
            FinParams.branch(p),
            null,
            p.date(FinParams.FROM),
            p.date(FinParams.TO),
            FinReportSupport.selectedAccounts(h, p),
            p.optionalText(PARTY_FROM).orElse(null),
            p.optionalText(PARTY_TO).orElse(null));
    List<MovementRow> openings = queries.partyMovements(q);
    List<LedgerLine> lines = queries.ledgerLines(q, true);
    Set<String> parties = new HashSet<>();
    openings.forEach(r -> parties.add(r.partyCode()));
    lines.forEach(l -> parties.add(l.partyCode()));
    Map<String, String> names = queries.partyNames(companyId, parties);
    Function<Key3, String> caption =
        k -> {
          String text = "Sub A/c " + k.dimension() + " - " + names.getOrDefault(k.dimension(), "");
          return foreign ? text + " / " + k.currency() : text;
        };
    Map<String, LedgerBlocks.Block> blocks = new TreeMap<>();
    for (MovementRow r : openings) {
      Key3 k = new Key3(r.accountId(), r.partyCode(), currency(foreign, r.currency()));
      LedgerBlocks.Block b = block(blocks, h, k, caption);
      b.addOpening(foreign ? r.openFc() : r.openBase());
    }
    for (LedgerLine l : lines) {
      block(
              blocks,
              h,
              new Key3(l.accountId(), l.partyCode(), currency(foreign, l.currency())),
              caption)
          .addLine(l);
    }
    List<ReportRow> rows = LedgerBlocks.writeGroups(blocks, foreign, branches);
    return result(p, rows, "Sub account = party (business partner) code on sub-ledger postings.");
  }

  private static ReportResult result(ReportParameters p, List<ReportRow> rows, String... notes) {
    var meta = p.metadata();
    return new ReportResult(
        meta.code(), meta.title(), p.echo(), LedgerBlocks.columns(), rows, List.of(notes));
  }

  private static Function<Key3, String> generalCaption(
      AccountHierarchy h, String level, boolean foreign) {
    return k -> {
      AccountNode main = h.mainOf(k.accountId());
      StringBuilder sb = new StringBuilder("Main A/c ").append(main.caption());
      if (!COMPANY_LEVEL.equals(level)) {
        sb.append(" / ").append(levelLabel(level)).append(' ').append(k.dimension());
      }
      return foreign ? sb.append(" / ").append(k.currency()).toString() : sb.toString();
    };
  }

  private static LedgerBlocks.Block block(
      Map<String, LedgerBlocks.Block> blocks,
      AccountHierarchy h,
      Key3 k,
      Function<Key3, String> caption) {
    AccountNode account = h.node(k.accountId());
    AccountNode main = h.mainOf(k.accountId());
    String sortKey = main.code() + SEP + account.code() + SEP + k.dimension() + SEP + k.currency();
    return blocks.computeIfAbsent(
        sortKey, s -> new LedgerBlocks.Block(caption.apply(k), account.caption()));
  }

  private static String currency(boolean foreign, String currency) {
    return foreign ? currency : "";
  }

  private static String levelValue(
      String level, Long branchId, String costCenter, Map<Long, String> branches) {
    return switch (level) {
      case DIVISION_LEVEL -> branches.getOrDefault(branchId, NONE);
      case DEPARTMENT_LEVEL -> costCenter == null ? NONE : costCenter;
      default -> "";
    };
  }

  private static String levelLabel(String level) {
    return DIVISION_LEVEL.equals(level) ? "Division" : "Department";
  }

  /**
   * Block key: posting account, dimension value (division, department or party) and currency.
   *
   * @param accountId account
   * @param dimension dimension value
   * @param currency currency or empty for LC
   */
  private record Key3(Long accountId, String dimension, String currency) {}
}

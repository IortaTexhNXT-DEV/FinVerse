package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.tax.domain.NormalBalance;
import com.iortatechnxt.finverse.tax.domain.ReturnFigures;
import com.iortatechnxt.finverse.tax.domain.ReturnLineValues;
import com.iortatechnxt.finverse.tax.domain.TaxPeriod;
import com.iortatechnxt.finverse.tax.domain.TaxType;
import com.iortatechnxt.finverse.tax.domain.Taxpayer;
import com.iortatechnxt.finverse.tax.domain.WorksheetKind;
import com.iortatechnxt.finverse.tax.service.TaxpayerDirectory.Lookup;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * Worksheet of a premium levy: documentary stamp tax (Form 2000, monthly), premium tax (2551Q,
 * quarterly, on business not subject to VAT), local government tax (LGU business tax) and fire
 * service tax (BFP). The tax is the amount charged on each approved policy or endorsement by the
 * product's rate, on the company's net premium; lines are summarised by line of business.
 *
 * <p>Assumptions: DST is paid by the insurer on the policies it issues (Sec. 184 NIRC) and reported
 * in the month of approval; LGT is computed on the premiums of the quarter although the Local
 * Government Code bases the tax on the preceding year's gross receipts — the LGU assessment is
 * entered as the filed amount if it differs.
 */
@Component
public class LevyWorksheetBuilder {

  private static final String LOB_PREFIX = "LOB:";
  private static final String UNASSIGNED = "UNASSIGNED";

  private final PremiumTaxSource premiums;
  private final TaxpayerDirectory directory;
  private final WorksheetSupport support;

  /**
   * Creates the builder.
   *
   * @param premiums premium documents
   * @param directory customer facts
   * @param support shared helpers
   */
  public LevyWorksheetBuilder(
      PremiumTaxSource premiums, TaxpayerDirectory directory, WorksheetSupport support) {
    this.premiums = premiums;
    this.directory = directory;
    this.support = support;
  }

  /**
   * Builds the worksheet.
   *
   * @param companyId company
   * @param type DST, PREMIUM_TAX, LGT or FST
   * @param period month or quarter
   * @return worksheet
   */
  public TaxWorksheet build(Long companyId, TaxType type, TaxPeriod period) {
    List<PremiumDocument> docs =
        premiums.documents(companyId, period).stream()
            .filter(d -> d.levy(type).signum() != 0)
            .toList();
    Set<String> parties = new TreeSet<>();
    docs.forEach(d -> parties.add(d.customerCode()));
    Lookup lookup = directory.lookup(companyId, parties);
    List<TaxDocumentLine> documents = docs.stream().map(d -> line(d, type, lookup)).toList();
    BigDecimal base = WorksheetSupport.sum(documents, TaxDocumentLine::taxableAmount);
    BigDecimal tax = WorksheetSupport.sum(documents, TaxDocumentLine::taxAmount);
    ReturnFigures figures = ReturnFigures.of(base, tax, BigDecimal.ZERO);

    Map<String, List<TaxDocumentLine>> byLob = new TreeMap<>();
    documents.forEach(
        d -> byLob.computeIfAbsent(lob(d.businessLine()), k -> new ArrayList<>()).add(d));
    List<ReturnLineValues> lines = new ArrayList<>();
    byLob.forEach(
        (lob, list) ->
            lines.add(
                new ReturnLineValues(
                    LOB_PREFIX + lob,
                    "Premiums - " + lob,
                    WorksheetSupport.sum(list, TaxDocumentLine::taxableAmount),
                    WorksheetSupport.sum(list, TaxDocumentLine::taxAmount))));
    lines.add(new ReturnLineValues("TAX_DUE", label(type) + " due", base, tax));
    List<LedgerControl> controls =
        support
            .control(companyId, type, label(type) + " payable", tax, period, NormalBalance.CREDIT)
            .stream()
            .toList();
    return new TaxWorksheet(
        WorksheetKind.ofLevy(type), period, lines, figures, documents, controls, List.of());
  }

  /**
   * Readable name of a premium levy.
   *
   * @param type levy
   * @return name
   */
  public static String label(TaxType type) {
    return switch (type) {
      case DST -> "Documentary stamp tax";
      case PREMIUM_TAX -> "Premium tax";
      case LGT -> "Local government tax";
      case FST -> "Fire service tax";
      default -> type.name();
    };
  }

  private static TaxDocumentLine line(PremiumDocument d, TaxType type, Lookup lookup) {
    Taxpayer t = lookup.taxpayer(d.customerCode(), d.customerName());
    BigDecimal tax = d.levy(type);
    return new TaxDocumentLine(
        TaxDocumentLine.PREMIUMS,
        d.sourceType(),
        d.policyId(),
        d.documentNo(),
        d.date(),
        d.customerCode(),
        t.name(),
        t.formattedTin(),
        type.name(),
        null,
        d.businessLine(),
        d.premium(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        tax,
        ReturnFigures.effectiveRate(d.premium(), tax));
  }

  private static String lob(String businessLine) {
    return businessLine == null ? UNASSIGNED : businessLine;
  }
}

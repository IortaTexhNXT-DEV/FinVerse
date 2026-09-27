package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.load.service.XrefService;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceData;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceOriginSnapshot;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceOriginSnapshot.Line;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceShare;
import com.iortatechnxt.brokerverse.opsledger.service.LegacyInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.LegacyInvoiceIntake;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Loader of the open legacy invoices (object F01 with its insurer shares F01S and component
 * positions F01C; DATA_MIGRATION_DESIGN 14.1 and 14.2). Each invoice is created in the Operations
 * ledger through {@link LegacyInvoiceIntake} (origin MIGRATED, ledger context LEGACY), on the
 * migrated client and, when the policy header was migrated, on its account; its opening entry and
 * open items are posted by {@link LegacyInvoicePosting} on the opening value date. Financial
 * objects are not updated by deltas: a changed invoice is corrected by a rollback and a new load
 * before the freeze, or by a year-end adjustment after go-live.
 */
@Component
public class LegacyInvoiceLoader implements MigrationLoader {

  /** Entity type of a ledger invoice in the cross-reference. */
  public static final String ENTITY = "OpsInvoice";

  private static final String OBJECT = "F01";
  private static final String SHARES = "F01S";
  private static final String POSITIONS = "F01C";
  private static final String GROSS = "gross_premium";
  private static final String COMMISSION = "commission";
  private static final String VAT = "vat_on_commission";
  private static final String OPEN = "open_balance";
  private static final String PARENT = "parent_invoice_no";
  private static final int ARN_LENGTH = 30;

  private final LegacyInvoiceIntake intake;
  private final LegacyInvoicePosting posting;
  private final OpsInvoiceRepository invoices;
  private final BranchRepository branches;
  private final XrefService xrefs;
  private final MigrationParameters parameters;

  /**
   * Creates the loader.
   *
   * @param intake legacy invoice intake of the ledger
   * @param posting opening entry and open items
   * @param invoices ledger invoices (read)
   * @param branches branches
   * @param xrefs cross-references (clients, policy headers)
   * @param parameters migration parameters
   */
  public LegacyInvoiceLoader(
      LegacyInvoiceIntake intake,
      LegacyInvoicePosting posting,
      OpsInvoiceRepository invoices,
      BranchRepository branches,
      XrefService xrefs,
      MigrationParameters parameters) {
    this.intake = intake;
    this.posting = posting;
    this.invoices = invoices;
    this.branches = branches;
    this.xrefs = xrefs;
    this.parameters = parameters;
  }

  @Override
  public String objectCode() {
    return OBJECT;
  }

  @Override
  public List<String> childLayouts() {
    return List.of(SHARES, POSITIONS);
  }

  /** An invoice family is loaded in one partition, the original first. */
  @Override
  public String partitionKey(LoadUnit unit) {
    String parent = Values.code(unit.value(PARENT));
    return parent == null ? unit.legacyKey() : parent;
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    LegacyInvoice legacy = request(unit, ctx);
    OpsInvoice invoice = intake.record(legacy);
    posting.open(invoice, legacy.header(), parameters.openingValueDate());
    return LoadOutcome.of(ENTITY, invoice.getId(), invoice.getInvoiceNo(), invoice.getVersion());
  }

  private LegacyInvoice request(LoadUnit unit, LoadContext ctx) {
    Map<String, String> v = unit.values();
    String source = unit.sourceSystem();
    String legacyNo = unit.legacyKey();
    String client = xref(ctx, source, "C01", Values.code(v.get("legacy_client_no")), "Client");
    String policyRef = Values.text(v.get("legacy_policy_ref"));
    Optional<KeyXref> header =
        policyRef == null
            ? Optional.empty()
            : xrefs.live(ctx.companyId(), source, "P01", policyRef);
    String arn =
        header
            .map(KeyXref::getTargetCode)
            .orElseGet(() -> arnOf(source, policyRef, Values.text(v.get("policy_no"))));
    OpsInvoiceData data =
        new OpsInvoiceData(
            new OpsInvoiceData.Keys(
                ctx.companyId(),
                branchId(ctx, Values.code(v.get("branch_code"))),
                intake.numberFor(legacyNo, source, parameters.invoiceNoCollisionPrefix()),
                arn,
                header.map(KeyXref::getTargetId).orElse(null),
                kind(v),
                Values.text(v.get("endorsement_no")),
                parent(source, Values.code(v.get(PARENT))),
                null,
                Values.text(v.get("policy_no")),
                Values.decimal(v.get("policy_year")).map(BigDecimal::intValue).orElse(1)),
            new OpsInvoiceData.Parties(
                client,
                Values.text(v.get("assured_name")),
                Optional.ofNullable(Values.text(v.get("payor_name")))
                    .orElse(Values.text(v.get("assured_name"))),
                leadInsurer(unit)),
            classification(v),
            new OpsInvoiceData.Amounts(
                Values.amount(v.get(GROSS)),
                Values.amount(v.get(COMMISSION)),
                Values.amount(v.get(VAT)),
                Values.decimal(v.get("wtax_rate_pct")).orElse(BigDecimal.ZERO)),
            new OpsInvoiceData.Flags(
                Values.flag(v.get("dp_flag")),
                Values.flag(v.get("cwt_flag")),
                Values.flag(v.get("incentive_eligible_flag"))));
    List<Line> positions = positions(unit);
    return new LegacyInvoice(
        data,
        shares(unit),
        RecordOrigin.migrated(source, policyRef, ctx.batchNo()),
        legacyNo,
        new OpsInvoiceOriginSnapshot.Header(
            Values.text(v.get("legacy_service_invoice_no")),
            Values.date(v.get("invoice_date")).orElse(null),
            Values.date(v.get("due_date")).orElse(null),
            Values.amount(v.get("commission_realised")),
            Values.amount(v.get("deferred_vat_open")),
            openBalanceMode(positions)),
        positions,
        Values.flag(v.get("hold_flag")));
  }

  private static OpsInvoiceData.Classification classification(Map<String, String> v) {
    return new OpsInvoiceData.Classification(
        Values.code(v.get("currency")),
        Values.date(v.get("booking_date")).orElse(null),
        Values.date(v.get("inception_date")).orElse(null),
        Values.date(v.get("expiry_date")).orElse(null),
        Values.text(v.get("risk_code")),
        Values.text(v.get("line_code")),
        Values.text(v.get("market_segment")),
        Values.text(v.get("ao_user_id")),
        Values.text(v.get("sales_unit_code")),
        Values.text(v.get("cost_center")));
  }

  private String xref(LoadContext ctx, String source, String object, String key, String what) {
    return xrefs
        .live(ctx.companyId(), source, object, key)
        .map(KeyXref::getTargetCode)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "MIG_" + object + "_NOT_LOADED", what + " " + key + " is not migrated"));
  }

  private Long branchId(LoadContext ctx, String code) {
    return branches
        .findByCompanyIdAndCode(ctx.companyId(), code)
        .orElseThrow(
            () -> new BusinessRuleException("MIG_BRANCH_UNKNOWN", "Branch " + code + " not found"))
        .getId();
  }

  private String parent(String source, String legacyParent) {
    if (legacyParent == null) {
      return null;
    }
    return intake
        .findByLegacyNo(legacyParent, source)
        .map(OpsInvoice::getInvoiceNo)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "MIG_PARENT_NOT_LOADED",
                    "Original invoice " + legacyParent + " is not loaded"));
  }

  private static String arnOf(String source, String policyRef, String policyNo) {
    String arn = "LGY-" + source + "-" + (policyRef == null ? policyNo : policyRef);
    return arn.length() <= ARN_LENGTH ? arn : arn.substring(0, ARN_LENGTH);
  }

  private static InvoiceKind kind(Map<String, String> v) {
    return InvoiceKind.valueOf(
        Optional.ofNullable(Values.code(v.get("invoice_kind")))
            .orElse(InvoiceKind.BOOKING.name())
            .toUpperCase(Locale.ROOT));
  }

  private static String leadInsurer(LoadUnit unit) {
    return shares(unit).stream()
        .filter(OpsInvoiceShare::lead)
        .findFirst()
        .map(OpsInvoiceShare::insurerCode)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "MIG_NO_INSURER", "Invoice " + unit.legacyKey() + " has no insurer share"));
  }

  private static List<OpsInvoiceShare> shares(LoadUnit unit) {
    List<StageRow> rows = unit.rows(SHARES);
    boolean leadGiven =
        rows.stream().anyMatch(r -> Values.flag(r.getMappedPayload().get("lead_flag")));
    return rows.stream()
        .map(StageRow::getMappedPayload)
        .map(
            s ->
                new OpsInvoiceShare(
                    Values.text(s.get("insurer_code")),
                    Values.decimal(s.get("share_pct")).orElse(BigDecimal.ZERO),
                    leadGiven
                        ? Values.flag(s.get("lead_flag"))
                        : s.equals(rows.get(0).getMappedPayload())))
        .toList();
  }

  private static List<Line> positions(LoadUnit unit) {
    return unit.rows(POSITIONS).stream()
        .map(StageRow::getMappedPayload)
        .map(
            p ->
                new Line(
                    LedgerComponent.valueOf(Values.code(p.get("component"))),
                    Values.amount(p.get("booked")),
                    Values.amount(p.get("adjusted")),
                    Values.amount(p.get("paid")),
                    Values.amount(p.get("remitted")),
                    Values.amount(p.get("written_off")),
                    Values.amount(p.get(OPEN))))
        .toList();
  }

  /** Legacy gave the open balances only: every original amount is zero. */
  private static boolean openBalanceMode(List<Line> positions) {
    return !positions.isEmpty()
        && positions.stream()
            .allMatch(
                l ->
                    l.booked().signum() == 0
                        && l.paid().signum() == 0
                        && l.remitted().signum() == 0
                        && l.writtenOff().signum() == 0
                        && l.adjusted().signum() == 0);
  }

  @Override
  public List<String> reconciledColumns() {
    return List.of(GROSS, COMMISSION, VAT);
  }

  @Override
  public Map<String, String> readBack(KeyXref entry) {
    return invoices
        .findById(entry.getTargetId())
        .map(
            i -> {
              Map<String, String> m = new HashMap<>();
              m.put(GROSS, plain(i.getGrossPremium()));
              m.put(COMMISSION, plain(i.getCommission()));
              m.put(VAT, plain(i.getVatOnCommission()));
              return m;
            })
        .orElse(Map.of());
  }

  private static String plain(BigDecimal value) {
    return value == null ? null : value.stripTrailingZeros().toPlainString();
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean changedSinceLoad(KeyXref entry) {
    return intake.workedAfterLoad(entry.getTargetId());
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    OpsInvoice invoice =
        invoices
            .findByInvoiceNo(entry.getTargetCode())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_INVOICE_MISSING", "Invoice " + entry.getTargetCode() + " not found"));
    OpsInvoiceOriginSnapshot s =
        intake
            .snapshot(invoice.getId())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_SNAPSHOT_MISSING",
                        "Invoice " + invoice.getInvoiceNo() + " has no snapshot"));
    posting.reverse(
        invoice,
        new OpsInvoiceOriginSnapshot.Header(
            s.getLegacyServiceInvoiceNo(),
            s.getInvoiceDate(),
            s.getDueDate(),
            s.getCommissionRealised(),
            s.getDeferredVatOpen(),
            s.isOpenBalanceMode()),
        ctx.businessDate());
    intake.rollback(invoice.getId(), ctx.batchNo());
    return true;
  }

  @Override
  public Optional<BigDecimal> targetTotal(AmountMeasure measure, List<KeyXref> loaded) {
    boolean header =
        OBJECT.equals(measure.layoutCode()) && reconciledColumns().contains(measure.column());
    boolean open = POSITIONS.equals(measure.layoutCode()) && OPEN.equals(measure.column());
    if (!header && !open) {
      return Optional.empty();
    }
    BigDecimal total = BigDecimal.ZERO;
    for (OpsInvoice i : invoices.findAllById(loaded.stream().map(KeyXref::getTargetId).toList())) {
      boolean currency = measure.currency() == null || measure.currency().equals(i.getCurrency());
      if (currency) {
        total = total.add(header ? headerValue(i, measure.column()) : openValue(i, measure));
      }
    }
    return Optional.of(total);
  }

  private static BigDecimal headerValue(OpsInvoice i, String column) {
    return switch (column) {
      case GROSS -> i.getGrossPremium();
      case COMMISSION -> i.getCommission();
      default -> i.getVatOnCommission();
    };
  }

  private static BigDecimal openValue(OpsInvoice i, AmountMeasure measure) {
    Map<LedgerComponent, BigDecimal> balances = i.balances();
    if ("component".equals(measure.filterColumn()) && measure.filterValue() != null) {
      return balances.getOrDefault(
          LedgerComponent.valueOf(measure.filterValue().toUpperCase(Locale.ROOT)), BigDecimal.ZERO);
    }
    return balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}

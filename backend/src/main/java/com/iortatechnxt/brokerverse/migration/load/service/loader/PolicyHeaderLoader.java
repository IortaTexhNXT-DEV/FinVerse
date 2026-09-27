package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.AccountLegacyHeader.LegacyPolicy;
import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.SalesStamp;
import com.iortatechnxt.brokerverse.account.service.LegacyAccount;
import com.iortatechnxt.brokerverse.account.service.LegacyAccountImport;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.load.service.XrefService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Loader of the in-force policy headers (object P01 with its insurer shares P01S;
 * DATA_MIGRATION_DESIGN sections 10 and 15): each header is imported as a booked account of origin
 * MIGRATED through {@link LegacyAccountImport}, on the migrated client, with the lead insurer, the
 * premium and policy numbers of legacy and the legacy package kept as given (remapped at Renewal
 * sanitation). Delta rows update the account before the freeze; a rolled-back batch cancels it.
 */
@Component
public class PolicyHeaderLoader implements MigrationLoader {

  /** Entity type of an account in the cross-reference. */
  public static final String ENTITY = "Account";

  private static final String SHARES = "P01S";
  private static final String GROSS = "gross_premium";
  private static final String SUM_INSURED = "sum_insured";
  private static final String EXPIRY = "expiry_date";
  private static final String LEGACY_BASIS = "LEGACY";

  private final LegacyAccountImport imports;
  private final AccountRepository accounts;
  private final XrefService xrefs;

  /**
   * Creates the loader.
   *
   * @param imports legacy account import
   * @param accounts accounts (read)
   * @param xrefs cross-references (migrated clients)
   */
  public PolicyHeaderLoader(
      LegacyAccountImport imports, AccountRepository accounts, XrefService xrefs) {
    this.imports = imports;
    this.accounts = accounts;
    this.xrefs = xrefs;
  }

  @Override
  public String objectCode() {
    return "P01";
  }

  @Override
  public List<String> childLayouts() {
    return List.of(SHARES);
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    Account a = imports.importLegacy(request(unit, ctx));
    return LoadOutcome.of(ENTITY, a.getId(), a.getArn(), a.getVersion());
  }

  @Override
  public Optional<LoadOutcome> update(LoadUnit unit, KeyXref entry, LoadContext ctx) {
    Account a = imports.update(entry.getTargetId(), request(unit, ctx));
    return Optional.of(LoadOutcome.of(ENTITY, a.getId(), a.getArn(), a.getVersion()));
  }

  private LegacyAccount request(LoadUnit unit, LoadContext ctx) {
    Map<String, String> v = unit.values();
    String legacyClient = Values.code(v.get("legacy_client_no"));
    KeyXref client =
        xrefs
            .live(ctx.companyId(), unit.sourceSystem(), "C01", legacyClient)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_CLIENT_NOT_LOADED", "Client " + legacyClient + " is not migrated"));
    Map<String, String> lead = leadShare(unit);
    List<String> pns = Values.items(v.get("pn_nos"));
    return new LegacyAccount(
        ctx.companyId(),
        client.getTargetCode(),
        Values.text(v.get("risk_code")),
        "RB".equalsIgnoreCase(Values.code(v.get("business_type")))
            ? BusinessType.RENEWAL
            : BusinessType.NEW_BUSINESS,
        new LegacyAccount.Cover(
            Values.text(lead.get("insurer_code")),
            Values.text(lead.get("insurer_branch_code")),
            Values.date(v.get("inception_date")).orElse(null),
            Values.date(v.get(EXPIRY)).orElse(null),
            Values.code(v.get("currency")),
            Values.text(v.get("market_segment")),
            Values.text(v.get("source_channel")),
            "DIRECT".equalsIgnoreCase(v.get("payment_arrangement"))
                ? PaymentArrangement.DIRECT_TO_INSURER
                : PaymentArrangement.VIA_BDOI,
            new Mortgage(
                Values.text(v.get("mortgagee_bank")),
                Values.text(v.get("loan_application_no")),
                pns)),
        new SalesStamp(
            null,
            null,
            Values.text(v.get("sales_unit_code")),
            Values.text(v.get("ao_user_id")),
            null),
        Values.amount(v.get(SUM_INSURED)),
        Values.text(v.get("risk_description")),
        premium(v),
        List.of(Values.text(v.get("policy_no"))),
        new LegacyPolicy(
            unit.sourceSystem(),
            unit.legacyKey(),
            Values.text(v.get("policy_no")),
            Values.text(v.get("cover_no")),
            Values.decimal(v.get("cover_version")).map(BigDecimal::intValue).orElse(null),
            legacyClient,
            Values.text(v.get("package_code")),
            Values.decimal(v.get("package_version")).map(BigDecimal::intValue).orElse(null),
            Values.code(v.get("policy_status")),
            Values.text(v.get("assured_name")),
            ctx.batchNo()));
  }

  private static Map<String, String> leadShare(LoadUnit unit) {
    List<StageRow> shares = unit.rows(SHARES);
    return shares.stream()
        .filter(r -> Values.flag(r.getMappedPayload().get("lead_flag")))
        .findFirst()
        .or(() -> shares.stream().findFirst())
        .map(StageRow::getMappedPayload)
        .orElse(Map.of());
  }

  private static AccountPremium premium(Map<String, String> v) {
    BigDecimal net = Values.amount(v.get("net_premium"));
    BigDecimal gross = Values.amount(v.get(GROSS));
    return new AccountPremium(
        LEGACY_BASIS,
        net,
        null,
        null,
        null,
        null,
        null,
        gross.subtract(net),
        gross,
        Values.decimal(v.get("commission_rate_pct")).orElse(null),
        null,
        null,
        false);
  }

  @Override
  public List<String> reconciledColumns() {
    return List.of(GROSS, SUM_INSURED, EXPIRY);
  }

  @Override
  public Map<String, String> readBack(KeyXref entry) {
    return accounts
        .findById(entry.getTargetId())
        .map(
            a -> {
              Map<String, String> m = new HashMap<>();
              m.put(GROSS, plain(a.getPremium().grossPremium()));
              m.put(SUM_INSURED, plain(a.getTotalSumInsured()));
              m.put(EXPIRY, a.getPeriodTo() == null ? null : a.getPeriodTo().toString());
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
    return accounts
        .findById(entry.getTargetId())
        .map(a -> !Long.valueOf(a.getVersion()).equals(entry.getTargetVersion()))
        .orElse(false);
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    imports.rollback(entry.getTargetId(), ctx.batchNo());
    return true;
  }

  @Override
  public Optional<BigDecimal> targetTotal(AmountMeasure measure, List<KeyXref> loaded) {
    if (!"P01".equals(measure.layoutCode()) || !GROSS.equals(measure.column())) {
      return Optional.empty();
    }
    BigDecimal total = BigDecimal.ZERO;
    for (Account a : accounts.findAllById(loaded.stream().map(KeyXref::getTargetId).toList())) {
      boolean currency = measure.currency() == null || measure.currency().equals(a.getCurrency());
      if (currency && a.getPremium().grossPremium() != null) {
        total = total.add(a.getPremium().grossPremium());
      }
    }
    return Optional.of(total);
  }
}

package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeSource;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee.PayeeDetails;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount.AccountDetails;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRepository;
import com.iortatechnxt.brokerverse.disbursement.service.PayeeService;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Loader of the payees (object R09; DATA_MIGRATION_DESIGN 10): each payee is created in the
 * Disbursement payee master through {@link PayeeService} with source MIGRATION, origin MIGRATED and
 * its legacy code, as a draft: the payee maintainer of Disbursement reviews and submits it and the
 * checker authorises it (maker-checker of the master stays with Disbursement). A rolled-back batch
 * removes the payees not yet authorised and puts the others up for deactivation.
 */
@Component
public class PayeeLoader implements MigrationLoader {

  /** Entity of a payee in the cross-reference. */
  public static final String ENTITY = "Payee";

  private static final String NAME = "name";

  private final PayeeService payees;
  private final PayeeRepository repository;

  /**
   * Creates the loader.
   *
   * @param payees payee maintenance
   * @param repository payees (read)
   */
  public PayeeLoader(PayeeService payees, PayeeRepository repository) {
    this.payees = payees;
    this.repository = repository;
  }

  @Override
  public String objectCode() {
    return "R09";
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    Map<String, String> v = unit.values();
    List<DisbursementMode> modes =
        Arrays.stream(Values.text(v.get("allowed_modes")).split("[|,]"))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .map(s -> DisbursementMode.valueOf(s.toUpperCase(Locale.ROOT)))
            .toList();
    String currency = Values.code(v.get("currency"));
    String accountNo = Values.text(v.get("account_no"));
    List<AccountDetails> accounts =
        accountNo == null
            ? List.of()
            : List.of(
                new AccountDetails(
                    Values.text(v.get("bank")),
                    null,
                    accountNo,
                    Values.text(v.get(NAME)),
                    currency,
                    modes.contains(DisbursementMode.CTA)
                        ? DisbursementMode.CTA
                        : DisbursementMode.TT,
                    true));
    Payee payee =
        payees.create(
            ctx.companyId(),
            unit.legacyKey(),
            new PayeeDetails(
                Values.code(v.get("payee_class")),
                Values.text(v.get(NAME)),
                Values.text(v.get("address")),
                Values.text(v.get("email")),
                Values.text(v.get("tin")),
                DisbursementMode.valueOf(
                    Values.code(v.get("default_mode")).toUpperCase(Locale.ROOT)),
                modes,
                List.of(),
                currency,
                null,
                "Migrated from " + unit.sourceSystem() + " (batch " + ctx.batchNo() + ")"),
            accounts,
            PayeeSource.MIGRATION);
    payee.markMigrated(RecordOrigin.migrated(unit.sourceSystem(), unit.legacyKey(), ctx.batchNo()));
    return LoadOutcome.of(ENTITY, payee.getId(), payee.getPayeeCode(), payee.getVersion());
  }

  @Override
  public List<String> reconciledColumns() {
    return List.of(NAME);
  }

  @Override
  public Map<String, String> readBack(KeyXref entry) {
    return repository
        .findById(entry.getTargetId())
        .map(
            p -> {
              Map<String, String> m = new HashMap<>();
              m.put(NAME, p.getName());
              return m;
            })
        .orElse(Map.of());
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    if (repository.existsById(entry.getTargetId())) {
      payees.rollbackMigrated(entry.getTargetId());
    }
    return true;
  }
}

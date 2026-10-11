package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.ClientPayoutAccount;
import com.iortatechnxt.brokerverse.crm.domain.ClientPayoutAccountRepository;
import com.iortatechnxt.brokerverse.crm.domain.PayoutDetails;
import com.iortatechnxt.brokerverse.crm.domain.PayoutMode;
import com.iortatechnxt.brokerverse.crm.service.ClientPayoutAccounts;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.load.service.XrefService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Loader of the client payout accounts used for refunds (object C03; DATA_MIGRATION_DESIGN section
 * 10): each account is recorded on the migrated client through {@link ClientPayoutAccounts}, with
 * the migration batch as its source; a rolled-back batch deactivates the accounts it added.
 */
@Component
public class PayoutAccountLoader implements MigrationLoader {

  /** Entity type of a payout account in the cross-reference. */
  public static final String ENTITY = "ClientPayoutAccount";

  private static final String SOURCE_MODULE = "MIGRATION";
  private static final String ACCOUNT_NO = "account_no";

  private final ClientPayoutAccounts payouts;
  private final ClientPayoutAccountRepository accounts;
  private final XrefService xrefs;

  /**
   * Creates the loader.
   *
   * @param payouts payout accounts
   * @param accounts payout accounts (read)
   * @param xrefs cross-references (client of the account)
   */
  public PayoutAccountLoader(
      ClientPayoutAccounts payouts, ClientPayoutAccountRepository accounts, XrefService xrefs) {
    this.payouts = payouts;
    this.accounts = accounts;
    this.xrefs = xrefs;
  }

  @Override
  public String objectCode() {
    return "C03";
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    Map<String, String> v = unit.values();
    String legacyClient = Values.code(v.get("legacy_client_no"));
    KeyXref client =
        xrefs
            .live(ctx.companyId(), unit.sourceSystem(), "C01", legacyClient)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_CLIENT_NOT_LOADED", "Client " + legacyClient + " is not migrated"));
    PayoutMode mode = "CHECK".equalsIgnoreCase(v.get("mode")) ? PayoutMode.CHECK : PayoutMode.CTA;
    ClientPayoutAccounts.Recorded r =
        payouts.record(
            ctx.companyId(),
            client.getTargetCode(),
            new PayoutDetails(
                mode, Values.text(v.get("payee_name")), Values.text(v.get(ACCOUNT_NO))),
            SOURCE_MODULE,
            ctx.batchNo());
    ClientPayoutAccount a = r.account();
    return new LoadOutcome(
        new KeyXref.Target(ENTITY, a.getId(), client.getTargetCode(), a.getVersion()),
        r.created() ? null : "The client already had this account");
  }

  @Override
  public List<String> reconciledColumns() {
    return List.of(ACCOUNT_NO);
  }

  @Override
  public Map<String, String> readBack(KeyXref entry) {
    return accounts
        .findById(entry.getTargetId())
        .map(
            a ->
                a.getAccountNo() == null
                    ? Map.<String, String>of()
                    : Map.of(ACCOUNT_NO, a.getAccountNo()))
        .orElse(Map.of());
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    accounts
        .findById(entry.getTargetId())
        .filter(
            a ->
                SOURCE_MODULE.equals(a.getSourceModule()) && ctx.batchNo().equals(a.getSourceRef()))
        .ifPresent(a -> payouts.deactivate(a.getClientId(), a.getId()));
    return true;
  }
}

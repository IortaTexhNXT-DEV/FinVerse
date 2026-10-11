package com.iortatechnxt.brokerverse.fixedasset.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.domain.GlAccountRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.fixedasset.domain.AssetCategory;
import com.iortatechnxt.brokerverse.fixedasset.domain.FixedAsset;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Publishes fixed asset events to the accounting engine. The asset category supplies the account
 * roles {@code ASSET}, {@code ACCUM_DEPRECIATION} and {@code DEPRECIATION_EXPENSE}; transactions
 * add {@code SETTLEMENT} or {@code BANK}. The engine's rules decide everything else.
 */
@Component
public class AssetAccounting {

  /** Source module recorded on every fixed asset journal. */
  public static final String MODULE = "FIXED_ASSETS";

  private final AccountingEventPublisher publisher;
  private final GlAccountRepository accounts;
  private final OrganizationService organization;

  /**
   * Creates the helper.
   *
   * @param publisher accounting engine
   * @param accounts chart of accounts
   * @param organization organization service
   */
  public AssetAccounting(
      AccountingEventPublisher publisher,
      GlAccountRepository accounts,
      OrganizationService organization) {
    this.publisher = publisher;
    this.accounts = accounts;
    this.organization = organization;
  }

  /**
   * Fails unless the account exists and accepts postings.
   *
   * @param companyId company
   * @param code account code
   */
  public void requirePostable(Long companyId, String code) {
    GlAccount account =
        accounts
            .findByCompanyIdAndCode(companyId, code)
            .orElseThrow(
                () -> new BusinessRuleException("UNKNOWN_ACCOUNT", "Unknown GL account " + code));
    if (!account.isPostable()) {
      throw new BusinessRuleException(
          "ACCOUNT_NOT_POSTABLE", "GL account " + code + " is a heading and cannot be posted to");
    }
  }

  /**
   * Posts an event of a single asset.
   *
   * @param asset asset
   * @param posting event details
   * @return posted journal
   */
  public JournalBatch publish(FixedAsset asset, Posting posting) {
    return publish(
        asset.getCompanyId(),
        asset.getCategory(),
        asset.getCostCenter(),
        asset.getSupplierCode(),
        posting);
  }

  /**
   * Posts an event for a category (one asset or a group of assets).
   *
   * @param companyId company
   * @param category category supplying the account roles
   * @param costCenter cost centre dimension
   * @param partyCode sub-ledger party
   * @param p event details
   * @return posted journal
   */
  public JournalBatch publish(
      Long companyId, AssetCategory category, String costCenter, String partyCode, Posting p) {
    Map<String, String> roles = new HashMap<>(p.accounts());
    roles.put("ASSET", category.getAssetAccount());
    roles.put("ACCUM_DEPRECIATION", category.getAccumulatedDepreciationAccount());
    roles.put("DEPRECIATION_EXPENSE", category.getDepreciationExpenseAccount());
    return publisher.publish(
        new BusinessEvent(
            p.eventType(),
            companyId,
            p.branchId(),
            p.valueDate(),
            organization.getCompany(companyId).getBaseCurrency(),
            MODULE,
            p.sourceReference(),
            p.reference(),
            partyCode,
            null,
            costCenter,
            p.narration(),
            p.amounts(),
            roles));
  }

  /**
   * Details of a fixed asset accounting event.
   *
   * @param eventType event type code
   * @param branchId branch that books the journal
   * @param valueDate value date
   * @param sourceReference idempotency key
   * @param reference business reference (tag no., run period)
   * @param narration narration
   * @param amounts amount components
   * @param accounts extra account roles (SETTLEMENT, BANK)
   */
  public record Posting(
      String eventType,
      Long branchId,
      LocalDate valueDate,
      String sourceReference,
      String reference,
      String narration,
      Map<String, BigDecimal> amounts,
      Map<String, String> accounts) {

    /** Canonical constructor copying the maps. */
    public Posting {
      amounts = Map.copyOf(amounts);
      accounts = Map.copyOf(accounts);
    }
  }
}

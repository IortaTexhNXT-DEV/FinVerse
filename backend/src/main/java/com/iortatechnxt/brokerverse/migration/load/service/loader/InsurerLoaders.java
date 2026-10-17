package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate;
import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate.RateValidity;
import com.iortatechnxt.brokerverse.catalog.domain.CommissionRateRepository;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.InsurerDetails;
import com.iortatechnxt.brokerverse.catalog.domain.PlacementChannel;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService.PartyContact;
import com.iortatechnxt.brokerverse.catalog.service.RateTableService;
import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.mapping.domain.EntryAction;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMapLoader;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMaps;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loaders of the insurers (R04) and their commission rates (R07; DATA_MIGRATION_DESIGN section 2).
 * A legacy insurer the INSURER map points to an existing insurer is only checked; one the map
 * CREATEs is added through {@link InsurerService} pending authorisation, with origin MIGRATED.
 * Commission rates are added through {@link RateTableService}, pending authorisation, with origin
 * MIGRATED. A rolled-back batch deactivates its insurers and removes its rates.
 */
@Configuration(proxyBeanMethods = false)
public class InsurerLoaders {

  private static final String INSURER_MAP = "INSURER";
  private static final String DEFAULT_CREDIT_DAYS = "INSURER_DEFAULT_CREDIT_DAYS";

  /**
   * R04 insurers.
   *
   * @param maps approved code maps
   * @param insurers insurer set-up
   * @return loader
   */
  @Bean
  public MigrationLoader insurerMigrationLoader(
      CodeMapLoader maps, InsurerService insurers, SystemParameterService parameters) {
    return new MigrationLoader() {
      @Override
      public String objectCode() {
        return "R04";
      }

      @Override
      public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
        String legacy = unit.legacyKey();
        CodeMaps.Resolution r =
            maps.approved(Set.of(INSURER_MAP))
                .resolve(INSURER_MAP, unit.sourceSystem(), legacy)
                .orElseThrow(
                    () ->
                        new BusinessRuleException(
                            "MIG_INSURER_NOT_MAPPED",
                            "Insurer " + legacy + " is not in the approved INSURER map"));
        if (r.action() != EntryAction.CREATE) {
          InsurerProfile existing = insurers.requireInsurer(ctx.companyId(), r.target());
          return LoadOutcome.of("Insurer", existing.getId(), existing.getPartyCode(), null);
        }
        InsurerProfile created =
            insurers.create(
                ctx.companyId(),
                r.target(),
                contact(unit),
                details(unit, parameters.requiredInt(DEFAULT_CREDIT_DAYS)));
        created.markMigrated(RecordOrigin.migrated(unit.sourceSystem(), legacy, ctx.batchNo()));
        return LoadOutcome.of("Insurer", created.getId(), created.getPartyCode(), null);
      }

      @Override
      public boolean reversible() {
        return true;
      }

      @Override
      public boolean compensate(KeyXref entry, LoadContext ctx) {
        InsurerProfile insurer = insurers.get(entry.getTargetId());
        RecordOrigin origin = insurer.getRecordOrigin();
        if (origin.isMigrated() && ctx.batchNo().equals(origin.migrationBatch())) {
          insurer.deactivate();
        }
        return true;
      }
    };
  }

  private static PartyContact contact(LoadUnit unit) {
    Map<String, String> v = unit.values();
    return new PartyContact(
        Values.text(v.get("tin")),
        Values.text(v.get("address")),
        Values.text(v.get("placement_email")),
        null);
  }

  private static InsurerDetails details(LoadUnit unit, int defaultCreditDays) {
    Map<String, String> v = unit.values();
    return new InsurerDetails(
        Values.text(v.get("insurer_name")),
        Values.text(v.get("short_name")),
        Values.text(v.get("accreditation_no")),
        Values.date(v.get("accredited_until")).orElse(null),
        PlacementChannel.EMAIL,
        Values.items(v.get("placement_email")),
        Values.decimal(v.get("default_credit_days"))
            .map(BigDecimal::intValue)
            .orElse(defaultCreditDays));
  }

  /**
   * R07 commission rates.
   *
   * @param rates rate tables
   * @param commissions commission rates (rollback)
   * @return loader
   */
  @Bean
  public MigrationLoader commissionRateMigrationLoader(
      RateTableService rates, CommissionRateRepository commissions) {
    return new MigrationLoader() {
      @Override
      public String objectCode() {
        return "R07";
      }

      @Override
      public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
        Map<String, String> v = unit.values();
        String insurer = Values.code(v.get("insurer_code"));
        String product = Values.code(v.get("risk_code"));
        CommissionRate rate =
            rates.createCommission(
                ctx.companyId(),
                insurer,
                product,
                new RateValidity(
                    Values.amount(v.get("rate_pct")),
                    Values.date(v.get("effective_from"))
                        .orElseThrow(
                            () ->
                                new BusinessRuleException(
                                    "MIG_DATE_REQUIRED", "The rate has no effective date")),
                    Values.date(v.get("effective_to")).orElse(null)));
        rate.markMigrated(
            RecordOrigin.migrated(unit.sourceSystem(), unit.legacyKey(), ctx.batchNo()));
        return LoadOutcome.of("CommissionRate", rate.getId(), insurer + "/" + product, null);
      }

      @Override
      public List<String> reconciledColumns() {
        return List.of("rate_pct");
      }

      @Override
      public Map<String, String> readBack(KeyXref entry) {
        return commissions
            .findById(entry.getTargetId())
            .map(c -> Map.of("rate_pct", c.getRate().stripTrailingZeros().toPlainString()))
            .orElse(Map.of());
      }

      @Override
      public boolean reversible() {
        return true;
      }

      @Override
      public boolean compensate(KeyXref entry, LoadContext ctx) {
        Optional<CommissionRate> rate = commissions.findById(entry.getTargetId());
        rate.filter(c -> ctx.batchNo().equals(c.getRecordOrigin().migrationBatch()))
            .ifPresent(commissions::delete);
        return true;
      }
    };
  }
}

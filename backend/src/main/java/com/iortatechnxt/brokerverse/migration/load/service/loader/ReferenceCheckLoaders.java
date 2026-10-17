package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.catalog.domain.SalesOfficer;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMapLoader;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loaders of the reference objects BIBS keeps as they are and only checks (DATA_MIGRATION_DESIGN
 * section 2): branches (R02) and the sales organisation (R03). Each legacy branch, unit and account
 * officer must map through its approved code map (the account officer through the layout's USER
 * map) to a record that exists in BIBS; the cross-reference then points to it, and nothing is
 * created, so a rollback has nothing to undo.
 */
@Configuration(proxyBeanMethods = false)
public class ReferenceCheckLoaders {

  private static final String BRANCH_MAP = "BRANCH";
  private static final String UNIT_MAP = "SALES_UNIT";

  /**
   * R02 branches: each legacy branch maps to a BIBS branch.
   *
   * @param maps approved code maps
   * @param branches branches
   * @return loader
   */
  @Bean
  public MigrationLoader branchCheckLoader(CodeMapLoader maps, BranchRepository branches) {
    return new CheckLoader("R02") {
      @Override
      public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
        String code = target(maps, BRANCH_MAP, unit, Values.code(unit.value("branch_code")));
        Branch branch =
            branches
                .findByCompanyIdAndCode(ctx.companyId(), code)
                .orElseThrow(
                    () ->
                        new BusinessRuleException(
                            "MIG_BRANCH_UNKNOWN", "Branch " + code + " does not exist in BIBS"));
        return LoadOutcome.of("Branch", branch.getId(), branch.getCode(), null);
      }
    };
  }

  /**
   * R03 sales organisation: each unit and account officer maps to BIBS.
   *
   * @param maps approved code maps
   * @param sales sales organisation
   * @return loader
   */
  @Bean
  public MigrationLoader salesOrganisationCheckLoader(
      CodeMapLoader maps, SalesOrganisationService sales) {
    return new CheckLoader("R03") {
      @Override
      public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
        boolean officer = "AO".equals(Values.code(unit.value("record_type")));
        if (officer) {
          String user = Values.text(unit.value("ao_user_id"));
          SalesOfficer found =
              sales.officers(ctx.companyId()).stream()
                  .filter(o -> o.getUsername().equals(user))
                  .findFirst()
                  .orElseThrow(
                      () ->
                          new BusinessRuleException(
                              "MIG_OFFICER_UNKNOWN",
                              "Account officer " + user + " is not in the sales organisation"));
          return LoadOutcome.of("SalesOfficer", found.getId(), user, null);
        }
        String code = target(maps, UNIT_MAP, unit, Values.code(unit.value("unit_code")));
        SalesUnit found =
            sales.units(ctx.companyId()).stream()
                .filter(u -> u.getCode().equals(code))
                .findFirst()
                .orElseThrow(
                    () ->
                        new BusinessRuleException(
                            "MIG_UNIT_UNKNOWN", "Sales unit " + code + " does not exist in BIBS"));
        return LoadOutcome.of("SalesUnit", found.getId(), code, null);
      }
    };
  }

  /** A loader that only checks the target exists. */
  abstract static class CheckLoader implements MigrationLoader {

    private final String object;

    CheckLoader(String object) {
      this.object = object;
    }

    @Override
    public String objectCode() {
      return object;
    }

    @Override
    public Optional<LoadOutcome> update(LoadUnit unit, KeyXref entry, LoadContext ctx) {
      return Optional.of(load(unit, ctx));
    }

    @Override
    public boolean reversible() {
      return true;
    }

    @Override
    public boolean compensate(KeyXref entry, LoadContext ctx) {
      return true;
    }

    static String target(CodeMapLoader maps, String set, LoadUnit unit, String legacy) {
      if (legacy == null) {
        throw new BusinessRuleException("MIG_CODE_REQUIRED", "The legacy code is missing");
      }
      return maps.approved(Set.of(set))
          .resolve(set, unit.sourceSystem(), legacy)
          .map(r -> r.target())
          .orElseThrow(
              () ->
                  new BusinessRuleException(
                      "MIG_CODE_NOT_MAPPED",
                      "Legacy code " + legacy + " is not in the approved " + set + " map"));
    }
  }
}

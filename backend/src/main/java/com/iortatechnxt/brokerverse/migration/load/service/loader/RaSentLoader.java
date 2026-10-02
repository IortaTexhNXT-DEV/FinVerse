package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.domain.RaSent;
import com.iortatechnxt.brokerverse.migration.load.domain.RaSentRepository;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.load.service.XrefService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Loader of the renewal advices already sent before go-live (object P03, the RA-sent file of the
 * Excel trackers; DATA_MIGRATION_DESIGN section 15.1): each advice is stored in {@code mig_ra_sent}
 * linked to the migrated header of its policy (P01), from where the go-live renewal extraction
 * serves it to Renewal, which does not send the advice again. A corrected row of a resubmission
 * replaces the advice; a rolled-back batch withdraws it.
 */
@Component
public class RaSentLoader implements MigrationLoader {

  /** Entity type of an advice in the cross-reference. */
  public static final String ENTITY = "MigRaSent";

  private static final String SENT = "ra_sent_date";

  private final RaSentRepository advices;
  private final XrefService xrefs;

  /**
   * Creates the loader.
   *
   * @param advices advices sent
   * @param xrefs cross-references (migrated headers)
   */
  public RaSentLoader(RaSentRepository advices, XrefService xrefs) {
    this.advices = advices;
    this.xrefs = xrefs;
  }

  @Override
  public String objectCode() {
    return "P03";
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    String ref = unit.legacyKey();
    if (advices
        .findByCompanyIdAndLegacyPolicyRefAndRolledBackFalse(ctx.companyId(), ref)
        .isPresent()) {
      throw new BusinessRuleException(
          "MIG_RA_DUPLICATE", "An advice of " + ref + " is already loaded");
    }
    RaSent saved =
        advices.save(
            new RaSent(
                ctx.companyId(), ref, data(unit, ctx), ctx.batch().getId(), unit.main().getId()));
    return LoadOutcome.of(ENTITY, saved.getId(), ref, saved.getVersion());
  }

  @Override
  public Optional<LoadOutcome> update(LoadUnit unit, KeyXref entry, LoadContext ctx) {
    return advices
        .findById(entry.getTargetId())
        .filter(a -> !a.isRolledBack())
        .map(
            a -> {
              a.apply(data(unit, ctx));
              return LoadOutcome.of(ENTITY, a.getId(), a.getLegacyPolicyRef(), a.getVersion());
            });
  }

  private RaSent.Data data(LoadUnit unit, LoadContext ctx) {
    Map<String, String> v = unit.values();
    String arn =
        xrefs
            .live(ctx.companyId(), unit.sourceSystem(), "P01", unit.legacyKey())
            .map(KeyXref::getTargetCode)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_HEADER_NOT_LOADED",
                        "Policy " + unit.legacyKey() + " is not among the migrated headers"));
    return new RaSent.Data(
        arn,
        Values.text(v.get("cover_no")),
        Values.date(v.get("expiry_date")).orElse(null),
        Values.date(v.get(SENT)).orElse(null),
        Values.text(v.get("ra_ref")),
        Values.code(v.get("ra_channel")),
        Values.text(v.get("sent_to")),
        Values.text(v.get("proposed_insurer_code")),
        Values.code(v.get("currency")),
        Values.decimal(v.get("proposed_premium")).orElse(null),
        Values.text(v.get("sent_by_user_id")),
        Values.text(v.get("tracker_name")),
        Values.text(v.get("remarks")));
  }

  @Override
  public List<String> reconciledColumns() {
    return List.of(SENT);
  }

  @Override
  public Map<String, String> readBack(KeyXref entry) {
    return advices
        .findById(entry.getTargetId())
        .filter(a -> a.getRaSentDate() != null)
        .map(a -> Map.of(SENT, a.getRaSentDate().toString()))
        .orElse(Map.of());
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    advices.findById(entry.getTargetId()).ifPresent(RaSent::rollBack);
    return true;
  }
}

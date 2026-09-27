package com.iortatechnxt.brokerverse.tax.seed;

import com.iortatechnxt.brokerverse.payables.service.SeedActor;
import com.iortatechnxt.brokerverse.tax.domain.IcLineItem;
import com.iortatechnxt.brokerverse.tax.domain.PartyTaxProfile;
import com.iortatechnxt.brokerverse.tax.domain.TaxCode;
import com.iortatechnxt.brokerverse.tax.domain.TaxForm;
import com.iortatechnxt.brokerverse.tax.seed.TaxSeedCatalog.CodeSpec;
import com.iortatechnxt.brokerverse.tax.seed.TaxSeedCatalog.FormSpec;
import com.iortatechnxt.brokerverse.tax.seed.TaxSeedCatalog.IcSpec;
import com.iortatechnxt.brokerverse.tax.seed.TaxSeedCatalog.ProfileSpec;
import com.iortatechnxt.brokerverse.tax.service.IcLineCommand;
import com.iortatechnxt.brokerverse.tax.service.IcMappingService;
import com.iortatechnxt.brokerverse.tax.service.PartyTaxProfileCommand;
import com.iortatechnxt.brokerverse.tax.service.PartyTaxProfileService;
import com.iortatechnxt.brokerverse.tax.service.TaxCodeCommand;
import com.iortatechnxt.brokerverse.tax.service.TaxCodeService;
import com.iortatechnxt.brokerverse.tax.service.TaxFormCommand;
import com.iortatechnxt.brokerverse.tax.service.TaxFormService;
import com.iortatechnxt.brokerverse.tax.service.TaxSourceQueries;
import com.iortatechnxt.brokerverse.tax.service.TaxSourceQueries.PartyFacts;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Creates the tax masters of a company from {@link TaxSeedCatalog} through the services, as maker
 * "accountant" with authorization by "checker", so the maker-checker trail is real. Idempotent:
 * only missing codes, forms, profiles and mapping lines are created; parties absent from the
 * company are skipped. Used by the seed runner and by the tests.
 */
@Component
public class TaxSeedMasters {

  /** First day tracked by the seed filing calendar. */
  static final LocalDate EFFECTIVE_FROM = LocalDate.of(2026, 1, 1);

  private final TaxCodeService codes;
  private final TaxFormService forms;
  private final PartyTaxProfileService profiles;
  private final IcMappingService mappings;
  private final TaxSourceQueries queries;
  private final SeedActor actor;

  /**
   * Creates the loader.
   *
   * @param codes tax codes
   * @param forms tax forms
   * @param profiles party tax profiles
   * @param mappings IC mapping
   * @param queries party facts
   * @param actor SIT/UAT users
   */
  public TaxSeedMasters(
      TaxCodeService codes,
      TaxFormService forms,
      PartyTaxProfileService profiles,
      IcMappingService mappings,
      TaxSourceQueries queries,
      SeedActor actor) {
    this.codes = codes;
    this.forms = forms;
    this.profiles = profiles;
    this.mappings = mappings;
    this.queries = queries;
    this.actor = actor;
  }

  /**
   * Creates and authorizes the missing masters of a company.
   *
   * @param companyId company
   */
  public void ensure(Long companyId) {
    Set<String> existingCodes =
        codes.list(companyId).stream().map(TaxCode::getCode).collect(Collectors.toSet());
    for (CodeSpec c : TaxSeedCatalog.CODES) {
      if (!existingCodes.contains(c.code())) {
        TaxCode created = actor.as(SeedActor.MAKER, () -> codes.create(command(companyId, c)));
        actor.as(SeedActor.CHECKER, () -> codes.authorize(created.getId()));
      }
    }
    Set<String> existingForms =
        forms.list(companyId).stream().map(TaxForm::getCode).collect(Collectors.toSet());
    for (FormSpec f : TaxSeedCatalog.FORMS) {
      if (!existingForms.contains(f.code())) {
        TaxForm created = actor.as(SeedActor.MAKER, () -> forms.create(command(companyId, f)));
        actor.as(SeedActor.CHECKER, () -> forms.authorize(created.getId()));
      }
    }
    ensureProfiles(companyId);
    ensureMappings(companyId);
  }

  private void ensureProfiles(Long companyId) {
    Set<String> existing =
        profiles.list(companyId).stream()
            .map(PartyTaxProfile::getPartyCode)
            .collect(Collectors.toSet());
    Map<String, PartyFacts> parties =
        queries.parties(
            companyId, TaxSeedCatalog.PROFILES.stream().map(ProfileSpec::party).toList());
    for (ProfileSpec p : TaxSeedCatalog.PROFILES) {
      PartyFacts party = parties.get(p.party());
      if (party != null && party.taxId() != null && !existing.contains(p.party())) {
        PartyTaxProfile created =
            actor.as(SeedActor.MAKER, () -> profiles.create(command(companyId, p, party)));
        actor.as(SeedActor.CHECKER, () -> profiles.authorize(created.getId()));
      }
    }
  }

  private void ensureMappings(Long companyId) {
    Set<String> existing =
        mappings.list(companyId).stream()
            .map(i -> i.getSchedule() + "/" + i.getLineCode())
            .collect(Collectors.toSet());
    int order = 0;
    for (IcSpec s : TaxSeedCatalog.IC_LINES) {
      order += 10;
      if (!existing.contains(s.schedule() + "/" + s.code())) {
        IcLineCommand cmd = command(companyId, s, order);
        IcLineItem created = actor.as(SeedActor.MAKER, () -> mappings.create(cmd));
        actor.as(SeedActor.CHECKER, () -> mappings.authorize(created.getId()));
      }
    }
  }

  private static TaxCodeCommand command(Long companyId, CodeSpec c) {
    return new TaxCodeCommand(
        companyId,
        c.code(),
        c.name(),
        c.type(),
        c.atc(),
        c.payee(),
        new BigDecimal(c.rate()),
        c.account(),
        c.nature(),
        EFFECTIVE_FROM,
        null);
  }

  private static TaxFormCommand command(Long companyId, FormSpec f) {
    return new TaxFormCommand(
        companyId,
        f.code(),
        f.name(),
        f.authority(),
        f.frequency(),
        f.worksheet(),
        1,
        f.dueDay(),
        f.payable(),
        f.credit(),
        f.payable() != null,
        EFFECTIVE_FROM);
  }

  private static PartyTaxProfileCommand command(Long companyId, ProfileSpec p, PartyFacts party) {
    return new PartyTaxProfileCommand(
        companyId,
        p.party(),
        party.taxId(),
        null,
        p.payee(),
        p.name(),
        p.lastName(),
        p.firstName(),
        p.middleName(),
        party.address(),
        p.zip(),
        p.vat(),
        p.atc());
  }

  private static IcLineCommand command(Long companyId, IcSpec s, int order) {
    return new IcLineCommand(
        companyId,
        s.schedule(),
        s.code(),
        s.description(),
        order,
        s.from(),
        s.to(),
        null,
        s.side(),
        s.sign(),
        s.measure(),
        s.factor() == null ? null : new BigDecimal(s.factor()));
  }
}

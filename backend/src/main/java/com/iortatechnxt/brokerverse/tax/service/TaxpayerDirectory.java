package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.tax.domain.PartyTaxProfile;
import com.iortatechnxt.brokerverse.tax.domain.PartyTaxProfileRepository;
import com.iortatechnxt.brokerverse.tax.domain.PayeeClass;
import com.iortatechnxt.brokerverse.tax.domain.TaxCode;
import com.iortatechnxt.brokerverse.tax.domain.TaxCodeRepository;
import com.iortatechnxt.brokerverse.tax.domain.TaxType;
import com.iortatechnxt.brokerverse.tax.domain.Taxpayer;
import com.iortatechnxt.brokerverse.tax.domain.VatTreatment;
import com.iortatechnxt.brokerverse.tax.service.TaxSourceQueries.PartyFacts;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves the tax identity, VAT treatment and ATC of the parties appearing on a worksheet, from
 * the authorized party tax profiles, falling back to the party master (TIN captured on the party,
 * corporate payee, regular VAT treatment, ATC "UNMAPPED").
 */
@Component
public class TaxpayerDirectory {

  /** ATC shown for income payments of payees without an authorized default ATC. */
  public static final String UNMAPPED_ATC = "UNMAPPED";

  private static final String INDIVIDUAL_TYPE = "INDIVIDUAL_CLIENT";

  private final PartyTaxProfileRepository profiles;
  private final TaxCodeRepository codes;
  private final TaxSourceQueries queries;

  /**
   * Creates the directory.
   *
   * @param profiles party tax profiles
   * @param codes tax codes
   * @param queries party facts
   */
  public TaxpayerDirectory(
      PartyTaxProfileRepository profiles, TaxCodeRepository codes, TaxSourceQueries queries) {
    this.profiles = profiles;
    this.codes = codes;
    this.queries = queries;
  }

  /**
   * Loads the facts of the given parties.
   *
   * @param companyId company
   * @param partyCodes parties
   * @return lookup
   */
  public Lookup lookup(Long companyId, Collection<String> partyCodes) {
    Map<String, PartyTaxProfile> byCode = new HashMap<>();
    profiles.findByCompanyIdOrderByPartyCode(companyId).stream()
        .filter(p -> p.getRecordStatus() == RecordStatus.ACTIVE)
        .forEach(p -> byCode.put(p.getPartyCode(), p));
    Map<String, TaxCode> ewtCodes = new HashMap<>();
    codes.findByCompanyIdOrderByTaxTypeAscCodeAsc(companyId).stream()
        .filter(c -> c.getTaxType() == TaxType.EWT && c.getRecordStatus() == RecordStatus.ACTIVE)
        .forEach(c -> ewtCodes.put(c.getCode(), c));
    return new Lookup(byCode, ewtCodes, queries.parties(companyId, partyCodes));
  }

  /**
   * ATC facts applied to a payee.
   *
   * @param atc alphanumeric tax code
   * @param incomeNature nature of income
   * @param rate expected rate in percent, null when unmapped
   */
  public record AtcFacts(String atc, String incomeNature, BigDecimal rate) {}

  /**
   * Party facts of one worksheet.
   *
   * @param profiles authorized profiles by party code
   * @param ewtCodes authorized withholding codes by code
   * @param parties party master facts by code
   */
  public record Lookup(
      Map<String, PartyTaxProfile> profiles,
      Map<String, TaxCode> ewtCodes,
      Map<String, PartyFacts> parties) {

    /**
     * Tax identity of a party.
     *
     * @param partyCode party
     * @param fallbackName name when the party is unknown
     * @return taxpayer
     */
    public Taxpayer taxpayer(String partyCode, String fallbackName) {
      PartyTaxProfile p = profiles.get(partyCode);
      if (p != null) {
        return new Taxpayer(
            p.getTin(),
            p.getBranchCode(),
            p.getRegisteredName(),
            p.getRegisteredAddress(),
            p.getZipCode());
      }
      PartyFacts f = parties.get(partyCode);
      return f == null
          ? Taxpayer.parse(null, fallbackName, null, null)
          : Taxpayer.parse(f.taxId(), f.name(), f.address(), null);
    }

    /**
     * Payee class of a party (individual clients default to INDIVIDUAL).
     *
     * @param partyCode party
     * @return class
     */
    public PayeeClass payeeClass(String partyCode) {
      PartyTaxProfile p = profiles.get(partyCode);
      if (p != null) {
        return p.getPayeeClass();
      }
      PartyFacts f = parties.get(partyCode);
      return f != null && INDIVIDUAL_TYPE.equals(f.partyType())
          ? PayeeClass.INDIVIDUAL
          : PayeeClass.CORPORATE;
    }

    /**
     * Name parts of an individual for the alphalists.
     *
     * @param partyCode party
     * @return profile when present
     */
    public Optional<PartyTaxProfile> profile(String partyCode) {
      return Optional.ofNullable(profiles.get(partyCode));
    }

    /**
     * VAT treatment of a customer.
     *
     * @param partyCode party
     * @return treatment (REGULAR without a profile)
     */
    public VatTreatment vatTreatment(String partyCode) {
      PartyTaxProfile p = profiles.get(partyCode);
      return p == null ? VatTreatment.REGULAR : p.getVatTreatment();
    }

    /**
     * ATC of income payments to a payee.
     *
     * @param partyCode payee
     * @return ATC facts ({@link #UNMAPPED_ATC} without an authorized default ATC)
     */
    public AtcFacts atc(String partyCode) {
      PartyTaxProfile p = profiles.get(partyCode);
      TaxCode code = p == null ? null : ewtCodes.get(p.getDefaultAtcCode());
      if (code == null) {
        return new AtcFacts(UNMAPPED_ATC, "Income payment (ATC not mapped)", null);
      }
      String nature = code.getIncomeNature() == null ? code.getName() : code.getIncomeNature();
      return new AtcFacts(code.getAtc(), nature, code.getRate());
    }
  }
}

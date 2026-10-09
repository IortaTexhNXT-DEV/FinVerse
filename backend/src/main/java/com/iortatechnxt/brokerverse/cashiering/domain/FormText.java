package com.iortatechnxt.brokerverse.cashiering.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The texts of an AR or OR form (FRS.CSH.02.06.02 / 02.06.03).
 *
 * @param companyName company name, blank for the company record
 * @param companyDescription company description
 * @param companyAddress company address, blank for the company record
 * @param companyVat company VAT TIN, blank for the company record
 * @param noteLine note line of the AR
 * @param footer1 footer line 1
 * @param footer2 footer line 2
 * @param footer3 footer line 3
 * @param footer4 footer line 4
 */
@Embeddable
public record FormText(
    @Column(name = "company_name", length = 150) String companyName,
    @Column(name = "company_description", length = 200) String companyDescription,
    @Column(name = "company_address", length = 300) String companyAddress,
    @Column(name = "company_vat", length = 40) String companyVat,
    @Column(name = "note_line", length = 500) String noteLine,
    @Column(name = "footer_1", length = 500) String footer1,
    @Column(name = "footer_2", length = 500) String footer2,
    @Column(name = "footer_3", length = 500) String footer3,
    @Column(name = "footer_4", length = 500) String footer4) {

  /** No text. */
  public static final FormText EMPTY =
      new FormText(null, null, null, null, null, null, null, null, null);
}

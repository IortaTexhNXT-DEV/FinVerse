package com.iortatechnxt.brokerverse.submitted.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/**
 * The bank loan of a submitted policy (BRIDSP-04; Report List #151).
 *
 * @param pnNo promissory note number
 * @param loanApplicationNo loan application number
 * @param cif bank client number
 * @param valueDate loan value date
 * @param maturityDate loan maturity date
 * @param referringBranch referring branch
 * @param originatingUnit originating unit
 * @param borrowerName borrower
 */
@Embeddable
public record SbmLoan(
    @Column(name = "pn_no", length = 40) String pnNo,
    @Column(name = "loan_application_no", length = 40) String loanApplicationNo,
    @Column(name = "cif", length = 40) String cif,
    @Column(name = "value_date") LocalDate valueDate,
    @Column(name = "maturity_date") LocalDate maturityDate,
    @Column(name = "referring_branch", length = 60) String referringBranch,
    @Column(name = "originating_unit", length = 60) String originatingUnit,
    @Column(name = "borrower_name", length = 250) String borrowerName) {

  /** No loan data. */
  public static final SbmLoan NONE = new SbmLoan(null, null, null, null, null, null, null, null);
}

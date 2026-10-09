package com.iortatechnxt.brokerverse.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.tax.domain.PartyTaxStatus;
import com.iortatechnxt.brokerverse.tax.domain.TaxType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Withholding status and tax exemption certificate of a party (TX-Q06, TX-Q09). */
class PartyTaxStatusTest {

  private static final LocalDate FROM = LocalDate.of(2027, 1, 1);
  private static final LocalDate TO = LocalDate.of(2027, 12, 31);

  @Test
  void aGovernmentPayorOrTopAgentIsAWithholdingAgent() {
    PartyTaxStatus government = new PartyTaxStatus(false, false, true, " ", null, null).checked();
    assertThat(government.withholdingAgent()).isTrue();
    assertThat(government.hasExemptionCertificate()).isFalse();
    assertThat(
            new PartyTaxStatus(false, true, false, null, null, null).checked().withholdingAgent())
        .isTrue();
    assertThat(PartyTaxStatus.NONE.checked().withholdingAgent()).isFalse();
  }

  @Test
  void aCertificateNeedsItsValidity() {
    assertThatThrownBy(() -> new PartyTaxStatus(false, false, false, "TEC-1", FROM, null).checked())
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("first and last day");
    assertThatThrownBy(() -> new PartyTaxStatus(false, false, false, "TEC-1", TO, FROM).checked())
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("ends before it starts");
    PartyTaxStatus valid = new PartyTaxStatus(false, false, false, " TEC-1 ", FROM, TO).checked();
    assertThat(valid.exemptionCertificateNo()).isEqualTo("TEC-1");
    assertThat(valid.exemptionValidOn(LocalDate.of(2027, 6, 30))).isTrue();
    assertThat(valid.exemptionValidOn(LocalDate.of(2028, 1, 1))).isFalse();
  }

  @Test
  void finalTaxesAreIdentifiedAndWithholdingsNeedTheirAtc() {
    assertThat(TaxType.FWT.isFinal()).isTrue();
    assertThat(TaxType.FINAL_VAT.isFinal()).isTrue();
    assertThat(TaxType.EWT.isFinal()).isFalse();
    assertThat(TaxType.FWT.needsAtc()).isTrue();
    assertThat(TaxType.PERCENTAGE_TAX.needsAtc()).isFalse();
  }
}

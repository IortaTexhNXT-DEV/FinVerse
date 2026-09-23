package com.iortatechnxt.finverse.underwriting;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.underwriting.service.ClaimsFigures;
import com.iortatechnxt.finverse.underwriting.service.PolicyClaimsView;
import com.iortatechnxt.finverse.underwriting.service.PolicyReinsuranceView;
import com.iortatechnxt.finverse.underwriting.service.ReinsuranceFigures;
import com.iortatechnxt.finverse.underwriting.service.TransactionRef;
import com.iortatechnxt.finverse.underwriting.service.UnderwritingPorts;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class UnderwritingPortsTest {

  private static UnderwritingPorts ports(PolicyReinsuranceView ri, PolicyClaimsView claims) {
    DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
    if (ri != null) {
      beans.registerSingleton("reinsuranceView", ri);
    }
    if (claims != null) {
      beans.registerSingleton("claimsView", claims);
    }
    return new UnderwritingPorts(
        beans.getBeanProvider(PolicyReinsuranceView.class),
        beans.getBeanProvider(PolicyClaimsView.class));
  }

  @Test
  void defaultsWhenNoModuleImplementsThePorts() {
    UnderwritingPorts ports = ports(null, null);
    assertThat(ports.reinsurance(List.of(TransactionRef.original(1L)))).isEmpty();
    assertThat(ports.claims(List.of(1L))).isEmpty();
    assertThat(ReinsuranceFigures.noCession(new BigDecimal("100")).netRetention())
        .isEqualByComparingTo("100");
    assertThat(ClaimsFigures.none().claimCount()).isZero();
    assertThat(TransactionRef.original(1L).isOriginal()).isTrue();
    assertThat(new TransactionRef(1L, 2).isOriginal()).isFalse();
  }

  @Test
  void delegatesToImplementations() {
    TransactionRef ref = TransactionRef.original(7L);
    ReinsuranceFigures ceded =
        new ReinsuranceFigures(new BigDecimal("60"), new BigDecimal("10"), new BigDecimal("30"));
    PolicyReinsuranceView ri = refs -> Map.of(ref, ceded);
    ClaimsFigures claim =
        new ClaimsFigures(
            1,
            "CLM-1",
            LocalDate.of(2026, 5, 1),
            BigDecimal.TEN,
            BigDecimal.ONE,
            BigDecimal.TEN,
            BigDecimal.TEN);
    PolicyClaimsView claims = ids -> Map.of(7L, claim);
    UnderwritingPorts ports = ports(ri, claims);

    assertThat(ports.reinsurance(List.of(ref))).containsEntry(ref, ceded);
    assertThat(ports.reinsurance(List.of())).isEmpty();
    assertThat(ports.claims(List.of(7L))).containsEntry(7L, claim);
    assertThat(ports.claims(List.of())).isEmpty();
  }
}

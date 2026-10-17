package com.iortatechnxt.brokerverse.finreport;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.finreport.service.VoucherGapDetector;
import com.iortatechnxt.brokerverse.finreport.service.VoucherGapDetector.VoucherGap;
import java.util.List;
import org.junit.jupiter.api.Test;

class VoucherGapDetectorTest {

  @Test
  void bookExampleOneAndThreeMeansTwoIsMissing() {
    List<VoucherGap> gaps =
        VoucherGapDetector.detect(List.of("JV-HO-2026-000003", "JV-HO-2026-000001"));
    assertThat(gaps)
        .containsExactly(new VoucherGap("JV-HO-2026", "JV-HO-2026-000002", "JV-HO-2026-000002", 1));
  }

  @Test
  void runsOfMissingNumbersAreReportedOnce() {
    List<VoucherGap> gaps =
        VoucherGapDetector.detect(
            List.of("RCT-CEB-2026-000010", "RCT-CEB-2026-000004", "RCT-CEB-2026-000005"));
    assertThat(gaps)
        .containsExactly(
            new VoucherGap("RCT-CEB-2026", "RCT-CEB-2026-000006", "RCT-CEB-2026-000009", 4));
  }

  @Test
  void seriesAreIndependentSoNoGapSpansTwoSeries() {
    List<VoucherGap> gaps =
        VoucherGapDetector.detect(
            List.of(
                "JV-HO-2025-000999",
                "JV-HO-2026-000001",
                "JV-HO-2026-000002",
                "JV-CEB-2026-000001",
                "JV-CEB-2026-000003"));
    assertThat(gaps).extracting(VoucherGap::series).containsExactly("JV-CEB-2026");
    assertThat(gaps.get(0).count()).isEqualTo(1);
  }

  @Test
  void duplicatesAndMalformedNumbersAreIgnored() {
    List<VoucherGap> gaps =
        VoucherGapDetector.detect(
            List.of(
                "JV-HO-2026-000001", "JV-HO-2026-000001", "MANUAL", "JV-HO-2026-X", "-12", "JV-"));
    assertThat(gaps).isEmpty();
  }

  @Test
  void widerNumbersKeepTheirPadding() {
    List<VoucherGap> gaps = VoucherGapDetector.detect(List.of("PAY-1-98", "PAY-1-101"));
    assertThat(gaps).containsExactly(new VoucherGap("PAY-1", "PAY-1-099", "PAY-1-100", 2));
  }
}

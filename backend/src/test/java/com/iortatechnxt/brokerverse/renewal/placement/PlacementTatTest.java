package com.iortatechnxt.brokerverse.renewal.placement;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.renewal.placement.service.PlacementNames;
import com.iortatechnxt.brokerverse.renewal.placement.service.PlacementTat;
import com.iortatechnxt.brokerverse.renewal.service.RenewalWorkingDays;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** The placement turnaround time and the file names of the placement documents. */
class PlacementTatTest {

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
  private static final Predicate<LocalDate> WORKING = RenewalWorkingDays::weekday;

  @Test
  void aPlacementSentAfterThreeInTheAfternoonCountsFromTheNextBusinessDay() {
    LocalDate wednesday = LocalDate.of(2026, 10, 7);
    assertThat(PlacementTat.start(wednesday.atTime(14, 59).atZone(MANILA), 15, WORKING))
        .isEqualTo(wednesday);
    assertThat(PlacementTat.start(wednesday.atTime(15, 1).atZone(MANILA), 15, WORKING))
        .isEqualTo(wednesday.plusDays(1));
    ZonedDateTime fridayEvening = LocalDate.of(2026, 10, 9).atTime(16, 0).atZone(MANILA);
    assertThat(PlacementTat.start(fridayEvening, 15, WORKING))
        .isEqualTo(LocalDate.of(2026, 10, 12));
  }

  @Test
  void theFileNamesFollowTheNamingConventions() {
    LocalDate day = LocalDate.of(2026, 7, 17);
    assertThat(PlacementNames.slip("PGAI", "MTR", "1234567890", day))
        .isEqualTo("PGAI_MTR_PlacementSlip_1234567890_07172026.pdf");
    assertThat(PlacementNames.file("PGAI", "MTR", day))
        .isEqualTo("PGAI_MTR_PlacementFile_07172026.xlsx");
    assertThat(PlacementNames.advice("1234567890", day)).isEqualTo("IA_1234567890_07172026.pdf");
  }
}

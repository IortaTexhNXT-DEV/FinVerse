package com.iortatechnxt.brokerverse.acsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.acsl.api.dto.AcslViews;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** A correction entry shows when it was raised, next to who raised it. */
class CorrectionViewTest {

  @Test
  void theCorrectionCarriesTheDateItWasRaised() {
    RecordComponent raisedAt =
        Arrays.stream(AcslViews.CorrectionView.class.getRecordComponents())
            .filter(c -> c.getName().equals("createdAt"))
            .findFirst()
            .orElseThrow();
    assertThat(raisedAt.getType()).isEqualTo(Instant.class);
  }
}

package com.iortatechnxt.brokerverse.subledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.subledger.service.AgeingService;
import com.iortatechnxt.brokerverse.subledger.service.AgeingSlots;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** The AGEING_BUCKETS parameter drives the default report slots; every test rolls back. */
@IntegrationTest
@Transactional
class AgeingDefaultsIT {

  @Autowired private AgeingService ageing;
  @Autowired private SystemParameterService parameters;

  @Test
  void defaultSlotsComeFromTheSystemParameter() {
    assertThat(ageing.defaultSlots()).isEqualTo(AgeingSlots.STANDARD);

    parameters.update(SystemParameterService.AGEING_BUCKETS, "15,45,75");
    assertThat(ageing.defaultSlots().labels()).containsExactly("0-15", "16-45", "46-75", "Over 75");
    assertThat(ageing.slotsOrDefault(" ").describe()).isEqualTo("15/45/75");
    assertThat(ageing.slotsOrDefault("10").describe()).isEqualTo("10");
  }

  @Test
  void anUnusableParameterFallsBackToTheStandardSlots() {
    parameters.update(SystemParameterService.AGEING_BUCKETS, "10,20,30,40,50,60,70,80,90");
    assertThat(ageing.defaultSlots()).isEqualTo(AgeingSlots.STANDARD);
  }
}

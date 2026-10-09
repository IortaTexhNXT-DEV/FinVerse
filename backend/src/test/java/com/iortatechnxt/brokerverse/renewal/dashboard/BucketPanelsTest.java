package com.iortatechnxt.brokerverse.renewal.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.candidate.service.BucketPanels;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Codes;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Flags;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateFilter.Tab;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The bucket panels of the setting in use (FRRN.002.05) and the In Processing tab of a Processing
 * Officer (FRRN.003.05).
 */
class BucketPanelsTest {

  private static CandidateFilter on(Tab tab) {
    return new CandidateFilter(1L, tab, null, null, null, Codes.NONE, Flags.NONE);
  }

  private static BucketPanels panels(String third, boolean assigner) {
    SystemParameterService parameters = mock(SystemParameterService.class);
    when(parameters.text(anyString(), anyString())).thenReturn(third);
    CurrentUser user = mock(CurrentUser.class);
    when(user.optionalUsername()).thenReturn(Optional.of("proc"));
    when(user.username()).thenReturn("proc");
    when(user.hasAuthority("RNW_PROCESS")).thenReturn(true);
    when(user.hasAuthority("RNW_PROCESS_ASSIGN")).thenReturn(assigner);
    return new BucketPanels(parameters, user);
  }

  @Test
  void theThirdPanelIsNonRenewableOrException() {
    assertThat(panels("NON_RENEWABLE", true).apply(on(Tab.BUCKET_NON_RENEWABLE)).tab())
        .isEqualTo(Tab.BUCKET_NON_RENEWABLE);
    assertThat(panels("EXCEPTION", true).apply(on(Tab.BUCKET_NON_RENEWABLE)).tab())
        .isEqualTo(Tab.EXCEPTIONS);
    assertThat(panels("EXCEPTION", true).apply(on(Tab.BUCKET_REVIEW)).tab())
        .isEqualTo(Tab.BUCKET_REVIEW_ONLY);
    assertThat(panels("EXCEPTION", true).thirdBucket()).isEqualTo("EXCEPTION");
  }

  @Test
  void aProcessingOfficerSeesInInProcessingOnlyTheRenewalsAssignedToHim() {
    assertThat(panels("NON_RENEWABLE", false).apply(on(Tab.IN_PROCESSING)).flags().assignedPo())
        .isEqualTo("proc");
    assertThat(panels("NON_RENEWABLE", true).apply(on(Tab.IN_PROCESSING)).flags().assignedPo())
        .isNull();
    assertThat(panels("NON_RENEWABLE", false).apply(on(Tab.FOR_PROCESSING)).flags().assignedPo())
        .isNull();
  }
}

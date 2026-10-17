package com.iortatechnxt.brokerverse.productmaint.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestType;
import org.junit.jupiter.api.Test;

/** The description of a package request in the work queues (TSU Workbench, My Work). */
class PackageRequestsTitleTest {

  @Test
  void describesTheRequestByTheTypeInWordsAndThePackageTitle() {
    PackageRequest request = mock(PackageRequest.class);
    when(request.getRequestType()).thenReturn(RequestType.NEW);
    when(request.getTitle()).thenReturn("Motor Fleet Plus");
    assertThat(PackageRequests.title(request)).isEqualTo("New package: Motor Fleet Plus");
    when(request.getRequestType()).thenReturn(RequestType.AMEND);
    when(request.getTitle()).thenReturn("Motor package MTR10 - lower deductible");
    assertThat(PackageRequests.title(request))
        .isEqualTo("Amendment: Motor package MTR10 - lower deductible")
        .doesNotContain("AMEND ");
  }
}

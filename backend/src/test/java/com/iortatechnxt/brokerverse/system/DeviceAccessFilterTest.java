package com.iortatechnxt.brokerverse.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.system.service.DeviceAccessFilter;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Use of the system from company-issued devices only (FRS.OPS.001). */
class DeviceAccessFilterTest {

  private final SystemParameterService parameters = mock(SystemParameterService.class);
  private final DeviceAccessFilter filter = new DeviceAccessFilter(parameters);

  private int status(String address, String deviceHeader) throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/v1/cashiering/receipts");
    request.setRemoteAddr(address);
    if (deviceHeader != null) {
      request.addHeader("X-Managed-Device", deviceHeader);
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response.getStatus();
  }

  private void settings(String restriction) {
    when(parameters.text(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
    when(parameters.text(DeviceAccessFilter.RESTRICTION, "OFF")).thenReturn(restriction);
    when(parameters.text(DeviceAccessFilter.HEADER, "")).thenReturn("X-Managed-Device");
    when(parameters.text(DeviceAccessFilter.HEADER_VALUE, "")).thenReturn("issued");
    when(parameters.items(DeviceAccessFilter.NETWORKS)).thenReturn(List.of("10.20.0.0/16"));
  }

  @Test
  void anyDeviceIsAcceptedWhileTheRestrictionIsOff() throws Exception {
    settings("OFF");
    assertThat(status("203.0.113.9", null)).isEqualTo(200);
  }

  @Test
  void withTheRestrictionOnOnlyCompanyNetworksAndIssuedDevicesAreAccepted() throws Exception {
    settings("ON");
    assertThat(status("10.20.4.7", null)).isEqualTo(200);
    assertThat(status("203.0.113.9", "issued")).isEqualTo(200);
    assertThat(status("203.0.113.9", "other")).isEqualTo(403);
    assertThat(status("203.0.113.9", null)).isEqualTo(403);
  }

  @Test
  void networksAreReadAsAddressesOrRanges() {
    assertThat(DeviceAccessFilter.inNetwork("10.20.255.1", "10.20.0.0/16")).isTrue();
    assertThat(DeviceAccessFilter.inNetwork("10.21.0.1", "10.20.0.0/16")).isFalse();
    assertThat(DeviceAccessFilter.inNetwork("192.168.1.5", "192.168.1.5")).isTrue();
    assertThat(DeviceAccessFilter.inNetwork("::1", "10.0.0.0/8")).isFalse();
  }
}

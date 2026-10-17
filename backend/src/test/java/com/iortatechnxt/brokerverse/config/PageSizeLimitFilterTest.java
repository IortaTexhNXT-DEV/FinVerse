package com.iortatechnxt.brokerverse.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PageSizeLimitFilterTest {

  private final PageSizeLimitFilter filter = new PageSizeLimitFilter(500);

  private boolean passes(String uri, String size, MockHttpServletResponse response)
      throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
    if (size != null) {
      request.setParameter("size", size);
    }
    AtomicBoolean reached = new AtomicBoolean();
    filter.doFilter(request, response, (req, res) -> reached.set(true));
    return reached.get();
  }

  @Test
  void aPageUpToTheLimitPasses() throws Exception {
    assertThat(passes("/api/v1/admin/sessions", "500", new MockHttpServletResponse())).isTrue();
    assertThat(passes("/api/v1/admin/sessions", null, new MockHttpServletResponse())).isTrue();
  }

  @Test
  void aLargerOrUnreadablePageIsRefusedWith400() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    assertThat(passes("/api/v1/admin/sessions", "100000", response)).isFalse();
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).contains("\"code\":\"PAGE_SIZE_TOO_LARGE\"");
    assertThat(passes("/api/v1/admin/sessions", "1e9", new MockHttpServletResponse())).isFalse();
  }

  @Test
  void pathsOutsideTheUserApisAreLeftAlone() throws Exception {
    assertThat(passes("/integration/v1/scim/v2/Users", "100000", new MockHttpServletResponse()))
        .isTrue();
  }
}

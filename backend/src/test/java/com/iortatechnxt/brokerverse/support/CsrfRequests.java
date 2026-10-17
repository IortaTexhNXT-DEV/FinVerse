package com.iortatechnxt.brokerverse.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * State-changing MockMvc request builders that carry a CSRF token (required for requests without a
 * bearer token). Static-import these instead of the {@link MockMvcRequestBuilders} variants.
 */
public final class CsrfRequests {

  private CsrfRequests() {}

  public static MockHttpServletRequestBuilder post(String url) {
    return MockMvcRequestBuilders.post(url).with(csrf());
  }

  public static MockHttpServletRequestBuilder put(String url) {
    return MockMvcRequestBuilders.put(url).with(csrf());
  }

  public static MockHttpServletRequestBuilder delete(String url) {
    return MockMvcRequestBuilders.delete(url).with(csrf());
  }

  public static MockMultipartHttpServletRequestBuilder multipart(String url) {
    MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(url);
    builder.with(csrf());
    return builder;
  }
}

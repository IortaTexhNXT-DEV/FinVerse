package com.iortatechnxt.brokerverse.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.api.GlobalExceptionHandler;
import com.iortatechnxt.brokerverse.common.api.RequestBodyTooLargeException;
import jakarta.servlet.FilterChain;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JsonBodyLimitFilterTest {

  private final JsonBodyLimitFilter filter = new JsonBodyLimitFilter("16B");

  private static MockHttpServletRequest request(String contentType, String body) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/journals");
    request.setContentType(contentType);
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    return request;
  }

  @Test
  void aDeclaredLengthAboveTheLimitIsAnsweredWith413() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<Boolean> reached = new AtomicReference<>(false);

    filter.doFilter(
        request("application/json", "{\"text\":\"far too long\"}"),
        response,
        (req, res) -> reached.set(true));

    assertThat(reached.get()).isFalse();
    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentAsString()).contains("\"code\":\"REQUEST_TOO_LARGE\"");
  }

  @Test
  void aBodyWithoutLengthStopsWhenItIsReadBeyondTheLimit() {
    MockHttpServletRequest unknownLength =
        new MockHttpServletRequest("POST", "/api/v1/journals") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    unknownLength.setContentType("application/json");
    unknownLength.setContent("x".repeat(40).getBytes(StandardCharsets.UTF_8));
    FilterChain readAll = (req, res) -> req.getInputStream().readAllBytes();

    assertThatThrownBy(() -> filter.doFilter(unknownLength, new MockHttpServletResponse(), readAll))
        .isInstanceOf(RequestBodyTooLargeException.class);
  }

  @Test
  void smallJsonAndOtherContentTypesPass() throws Exception {
    AtomicReference<String> read = new AtomicReference<>();
    FilterChain readAll =
        (req, res) ->
            read.set(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

    filter.doFilter(
        request("application/json", "{\"a\":1}"), new MockHttpServletResponse(), readAll);
    assertThat(read.get()).isEqualTo("{\"a\":1}");

    String upload = "x".repeat(100);
    filter.doFilter(
        request("application/octet-stream", upload), new MockHttpServletResponse(), readAll);
    assertThat(read.get()).isEqualTo(upload);
  }

  @Test
  void theExceptionHandlerAnswersAnOversizedBodyWith413() throws IOException {
    HttpMessageNotReadableException unreadable =
        new HttpMessageNotReadableException(
            "I/O error",
            new RequestBodyTooLargeException(16),
            new MockHttpInputMessage(new byte[0]));

    ProblemDetail problem = new GlobalExceptionHandler().handleUnreadableRequest(unreadable);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
    assertThat(problem.getProperties()).containsEntry("code", "REQUEST_TOO_LARGE");
  }

  @Test
  void onlyJsonMediaTypesAreLimited() {
    assertThat(JsonBodyLimitFilter.isJson("application/json")).isTrue();
    assertThat(JsonBodyLimitFilter.isJson("Application/JSON; charset=UTF-8")).isTrue();
    assertThat(JsonBodyLimitFilter.isJson("application/merge-patch+json")).isTrue();
    assertThat(JsonBodyLimitFilter.isJson("multipart/form-data; boundary=x")).isFalse();
    assertThat(JsonBodyLimitFilter.isJson(null)).isFalse();
  }
}

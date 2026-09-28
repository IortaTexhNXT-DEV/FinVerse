package com.iortatechnxt.brokerverse.config;

import com.iortatechnxt.brokerverse.common.api.RequestBodyTooLargeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Size limit of JSON request bodies ({@code brokerverse.http.max-json-body-size}, environment
 * {@code BROKERVERSE_MAX_JSON_BODY_SIZE}, default 2 MB), before security and the controllers. A
 * declared {@code Content-Length} above the limit is answered with 413 at once; a body without a
 * length (chunked) is counted while it is read and the read fails beyond the limit, which the
 * exception handler answers with 413 ({@code REQUEST_TOO_LARGE}). File uploads (multipart) have
 * their own limits ({@code spring.servlet.multipart.*}) and are not affected.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class JsonBodyLimitFilter extends OncePerRequestFilter {

  private final long maxBytes;

  /**
   * Creates the filter.
   *
   * @param maxSize largest JSON body, e.g. {@code 2MB}
   */
  public JsonBodyLimitFilter(@Value("${brokerverse.http.max-json-body-size:2MB}") String maxSize) {
    this.maxBytes = DataSize.parse(maxSize.trim()).toBytes();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!isJson(request.getContentType())) {
      chain.doFilter(request, response);
      return;
    }
    if (request.getContentLengthLong() > maxBytes) {
      refuse(response);
      return;
    }
    chain.doFilter(new LimitedRequest(request, maxBytes), response);
  }

  private void refuse(HttpServletResponse response) throws IOException {
    response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(
            "{\"type\":\"about:blank\",\"title\":\"Payload Too Large\",\"status\":413,"
                + "\"detail\":\"The request is larger than "
                + maxBytes
                + " bytes\",\"code\":\""
                + RequestBodyTooLargeException.CODE
                + "\"}");
  }

  static boolean isJson(String contentType) {
    if (contentType == null) {
      return false;
    }
    String type = contentType.toLowerCase(Locale.ROOT);
    int parameters = type.indexOf(';');
    String base = (parameters < 0 ? type : type.substring(0, parameters)).trim();
    return base.equals(MediaType.APPLICATION_JSON_VALUE) || base.endsWith("+json");
  }

  /** Counts the bytes of the body while the application reads it. */
  private static final class LimitedRequest extends HttpServletRequestWrapper {

    private final long maxBytes;
    private ServletInputStream stream;

    LimitedRequest(HttpServletRequest request, long maxBytes) {
      super(request);
      this.maxBytes = maxBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (stream == null) {
        stream = new LimitedStream(super.getInputStream(), maxBytes);
      }
      return stream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
      String encoding = getCharacterEncoding();
      Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
      return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
  }

  private static final class LimitedStream extends ServletInputStream {

    private final ServletInputStream in;
    private final long maxBytes;
    private long read;

    LimitedStream(ServletInputStream in, long maxBytes) {
      this.in = in;
      this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
      int b = in.read();
      if (b >= 0) {
        count(1);
      }
      return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int n = in.read(buffer, offset, length);
      if (n > 0) {
        count(n);
      }
      return n;
    }

    @Override
    public boolean isFinished() {
      return in.isFinished();
    }

    @Override
    public boolean isReady() {
      return in.isReady();
    }

    @Override
    public void setReadListener(ReadListener listener) {
      in.setReadListener(listener);
    }

    @Override
    public void close() throws IOException {
      in.close();
    }

    private void count(int n) throws RequestBodyTooLargeException {
      read += n;
      if (read > maxBytes) {
        throw new RequestBodyTooLargeException(maxBytes);
      }
    }
  }
}

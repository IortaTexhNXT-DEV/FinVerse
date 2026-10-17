package com.iortatechnxt.brokerverse.system.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Use of the system from company-issued devices only (FRS.OPS.001; Appendix R): with {@code
 * ACCESS_DEVICE_RESTRICTION = ON} a request is accepted from the company networks of {@code
 * ACCESS_ALLOWED_NETWORKS} (addresses or CIDR ranges) or with the header that the device-management
 * gateway adds for an issued device ({@code ACCESS_DEVICE_HEADER}, value {@code
 * ACCESS_DEVICE_HEADER_VALUE}); any other request is refused before sign-in. Off by default, so
 * test environments are reached from any device; the screens are the same on every device.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class DeviceAccessFilter extends OncePerRequestFilter {

  /** Setting: ON to restrict the devices. */
  public static final String RESTRICTION = "ACCESS_DEVICE_RESTRICTION";

  /** Setting: company networks. */
  public static final String NETWORKS = "ACCESS_ALLOWED_NETWORKS";

  /** Setting: header of an issued device. */
  public static final String HEADER = "ACCESS_DEVICE_HEADER";

  /** Setting: value of the header. */
  public static final String HEADER_VALUE = "ACCESS_DEVICE_HEADER_VALUE";

  private static final String REFUSAL =
      "{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,"
          + "\"detail\":\"The system can be used only from a device issued by the company\","
          + "\"code\":\"DEVICE_NOT_ALLOWED\"}";
  private static final List<String> OPEN_PATHS = List.of("/actuator/", "/integration/");
  private static final int BITS_PER_BYTE = 8;

  private final SystemParameterService parameters;

  /**
   * Creates the filter.
   *
   * @param parameters settings
   */
  public DeviceAccessFilter(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return OPEN_PATHS.stream().anyMatch(path::startsWith)
        || !"ON".equals(parameters.text(RESTRICTION, "OFF").strip());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (allowed(request)) {
      chain.doFilter(request, response);
      return;
    }
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.getOutputStream().write(REFUSAL.getBytes(StandardCharsets.UTF_8));
  }

  private boolean allowed(HttpServletRequest request) {
    String header = parameters.text(HEADER, "").strip();
    if (!header.isEmpty()) {
      String expected = parameters.text(HEADER_VALUE, "").strip();
      String sent = request.getHeader(header);
      if (sent != null && (expected.isEmpty() || expected.equals(sent.strip()))) {
        return true;
      }
    }
    String address = request.getRemoteAddr();
    return parameters.items(NETWORKS).stream().anyMatch(n -> inNetwork(address, n.strip()));
  }

  /**
   * Whether an address is in a network.
   *
   * @param address client address
   * @param network address or CIDR range, e.g. 10.20.0.0/16
   * @return true when inside
   */
  public static boolean inNetwork(String address, String network) {
    if (address == null || network.isEmpty()) {
      return false;
    }
    try {
      int slash = network.indexOf('/');
      byte[] base =
          InetAddress.getByName(slash < 0 ? network : network.substring(0, slash)).getAddress();
      byte[] client = InetAddress.getByName(address).getAddress();
      if (base.length != client.length) {
        return false;
      }
      int bits =
          slash < 0 ? base.length * BITS_PER_BYTE : Integer.parseInt(network.substring(slash + 1));
      int shift = base.length * BITS_PER_BYTE - bits;
      return new BigInteger(1, base)
          .shiftRight(shift)
          .equals(new BigInteger(1, client).shiftRight(shift));
    } catch (UnknownHostException | NumberFormatException ex) {
      return false;
    }
  }
}

package com.iortatechnxt.brokerverse.audit.domain;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Where an audited action comes from: the source (IP) address of the request and the group profiles
 * (roles) the acting user holds at the time of the action (BDOI FRS FRUM.008.01, conflict C19).
 * Both are read from the current web request; background work has neither.
 *
 * <p>The sign-in filter remembers the role names of the authenticated user on the request ({@link
 * #rememberRoles}); entries written outside a web request (scheduled jobs) carry no address and no
 * role.
 */
public final class ActorContext {

  /** Request attribute holding the role names of the acting user. */
  static final String ROLES_ATTRIBUTE = ActorContext.class.getName() + ".roles";

  /** Longest role text kept on an entry. */
  public static final int MAX_ROLES = 300;

  /** Longest address kept on an entry (IPv6 with zone). */
  public static final int MAX_ADDRESS = 45;

  private ActorContext() {}

  /**
   * Source address of the current web request.
   *
   * @return address, null outside a web request
   */
  public static String address() {
    HttpServletRequest request = request();
    if (request == null || request.getRemoteAddr() == null) {
      return null;
    }
    return clip(request.getRemoteAddr(), MAX_ADDRESS);
  }

  /**
   * Role names of the acting user remembered on the current web request.
   *
   * @return role names separated by commas, null when not known
   */
  public static String roles() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return null;
    }
    Object roles = attributes.getAttribute(ROLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    return roles instanceof String s ? s : null;
  }

  /**
   * Remembers the role names of the user acting in the current web request.
   *
   * @param roleNames role names separated by commas
   */
  public static void rememberRoles(String roleNames) {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes != null && roleNames != null) {
      attributes.setAttribute(
          ROLES_ATTRIBUTE, clip(roleNames, MAX_ROLES), RequestAttributes.SCOPE_REQUEST);
    }
  }

  /**
   * Cuts a text to a length.
   *
   * @param text text, may be null
   * @param max longest length
   * @return the text, cut
   */
  public static String clip(String text, int max) {
    return text == null || text.length() <= max ? text : text.substring(0, max);
  }

  private static HttpServletRequest request() {
    return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servlet
        ? servlet.getRequest()
        : null;
  }
}

package com.iortatechnxt.brokerverse.report.core;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Report permissions of the signed-in user: view (on-screen run) and export (download, print),
 * which a report may split (CSHID.017/018).
 */
public final class ReportAccess {

  private ReportAccess() {}

  /**
   * Whether the user may run a report on screen.
   *
   * @param metadata report
   * @return true when the view permission is granted
   */
  public static boolean mayView(ReportMetadata metadata) {
    return granted().contains(metadata.permission().name());
  }

  /**
   * Whether the user may download or print a report.
   *
   * @param metadata report
   * @return true when the export permission is granted
   */
  public static boolean mayExport(ReportMetadata metadata) {
    return granted().contains(metadata.exportPermission().name());
  }

  private static Set<String> granted() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return Set.of();
    }
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
  }
}

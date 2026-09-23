package com.iortatechnxt.finverse.approval.service;

import java.util.Objects;
import java.util.Set;

/**
 * Who is looking at the inbox: user name and granted permissions. Sources use it to return only
 * items the viewer may act on and to exclude the viewer's own submissions (maker-checker).
 *
 * @param username viewer, or null for the system-wide view used by monitoring (ageing alerts)
 * @param authorities granted permission codes
 * @param systemView true when every pending item is requested regardless of user
 */
public record ApprovalViewer(String username, Set<String> authorities, boolean systemView) {

  /** Canonical constructor copying the authorities. */
  public ApprovalViewer {
    authorities = Set.copyOf(authorities);
  }

  /**
   * Viewer for a signed-in user.
   *
   * @param username user
   * @param authorities permissions
   * @return viewer
   */
  public static ApprovalViewer user(String username, Set<String> authorities) {
    return new ApprovalViewer(username, authorities, false);
  }

  /**
   * System-wide view: all pending items of every user.
   *
   * @return viewer
   */
  public static ApprovalViewer system() {
    return new ApprovalViewer(null, Set.of(), true);
  }

  /**
   * Whether the viewer holds a permission (always true for the system view).
   *
   * @param permission permission code
   * @return true when granted
   */
  public boolean can(String permission) {
    return systemView || authorities.contains(permission);
  }

  /**
   * Whether an item made by {@code maker} may be shown: never the viewer's own items.
   *
   * @param maker maker of the item
   * @return true when the viewer is not the maker
   */
  public boolean mayApproveItemOf(String maker) {
    return systemView || !Objects.equals(username, maker);
  }
}

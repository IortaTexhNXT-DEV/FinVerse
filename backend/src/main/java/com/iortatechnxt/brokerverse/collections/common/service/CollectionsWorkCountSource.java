package com.iortatechnxt.brokerverse.collections.common.service;

import java.util.List;

/**
 * Port of the Collections home (COLLECTIONS_DESIGN 11, CQ21): every Collections sub-module that
 * owns a work queue implements it as a bean and returns its tiles for the signed-in user, e.g.
 * installments due, promises due today, broken promises, escalations with me (plans and escalation
 * wave) or unapplied payments awaiting my disposition (unapplied wave). The worklist itself
 * contributes my open accounts, DP returned and files ready.
 */
public interface CollectionsWorkCountSource {

  /**
   * Tiles of the source for a user.
   *
   * @param companyId company
   * @param username signed-in user
   * @return tiles, empty when the user has no such work
   */
  List<WorkCount> counts(Long companyId, String username);

  /**
   * A tile of the Collections home.
   *
   * @param key stable key
   * @param label Title Case label
   * @param value count
   * @param link route that opens the list
   * @param alert draws attention to a non-zero count
   * @param order position on the home (lower first)
   */
  record WorkCount(String key, String label, long value, String link, boolean alert, int order) {}
}

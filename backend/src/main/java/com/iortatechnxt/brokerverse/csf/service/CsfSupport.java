package com.iortatechnxt.brokerverse.csf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * Platform collaborators shared by the CSF services: document numbers, lists of values, audit
 * trail, activity log, current user, clock and the JSON writer of the payloads.
 */
@Component
public class CsfSupport {

  /** Longest audit summary and hand-off summary. */
  static final int SUMMARY_MAX = 500;

  private final DocumentNumberService numbers;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final ActivityLog activity;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final ObjectMapper json;

  /**
   * Creates the holder.
   *
   * @param numbers document numbers
   * @param lovs lists of values
   * @param audit audit trail
   * @param activity activity log
   * @param currentUser current user
   * @param clock clock
   * @param json JSON writer
   */
  public CsfSupport(
      DocumentNumberService numbers,
      LovService lovs,
      AuditTrailService audit,
      ActivityLog activity,
      CurrentUser currentUser,
      Clock clock,
      ObjectMapper json) {
    this.numbers = numbers;
    this.lovs = lovs;
    this.audit = audit;
    this.activity = activity;
    this.currentUser = currentUser;
    this.clock = clock;
    this.json = json;
  }

  /**
   * A summary cut to the audit and hand-off column length.
   *
   * @param text summary
   * @return summary of at most {@value #SUMMARY_MAX} characters
   */
  static String cut(String text) {
    return text.length() <= SUMMARY_MAX ? text : text.substring(0, SUMMARY_MAX);
  }

  DocumentNumberService numbers() {
    return numbers;
  }

  LovService lovs() {
    return lovs;
  }

  AuditTrailService audit() {
    return audit;
  }

  ActivityLog activity() {
    return activity;
  }

  CurrentUser currentUser() {
    return currentUser;
  }

  Clock clock() {
    return clock;
  }

  ObjectMapper json() {
    return json;
  }
}

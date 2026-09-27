package com.iortatechnxt.brokerverse.eb.member.service;

import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.eb.domain.EbMember;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChange;
import java.time.LocalDate;
import java.util.List;

/**
 * A member change as the AO captures it (FR-EB-055).
 *
 * @param lineNo programme line
 * @param policyYear roster year; the latest accepted roster's year when null
 * @param source AO, or CLIENT for the client's request entered by the AO
 * @param financial whether it has a premium effect
 * @param description description, may be null
 * @param lines lines, at least one
 * @param files the client's request, may be empty
 */
public record MemberChangeInput(
    int lineNo,
    Integer policyYear,
    String source,
    boolean financial,
    String description,
    List<Line> lines,
    List<UploadedFile> files) {

  /** Null lists become empty. */
  public MemberChangeInput {
    lines = lines == null ? List.of() : List.copyOf(lines);
    files = files == null ? List.of() : List.copyOf(files);
  }

  /**
   * The same input with its files.
   *
   * @param uploaded files
   * @return input
   */
  public MemberChangeInput withFiles(List<UploadedFile> uploaded) {
    return new MemberChangeInput(
        lineNo, policyYear, source, financial, description, lines, uploaded);
  }

  /**
   * A line.
   *
   * @param action ADD, DELETE, CHANGE_PLAN or CHANGE_DATA
   * @param employeeNo employee number
   * @param member member data (ADD, CHANGE_DATA; the plan for CHANGE_PLAN), may be null
   * @param effectiveDate effective date, within the policy period
   */
  public record Line(
      EbMemberChange.Action action,
      String employeeNo,
      EbMember.Data member,
      LocalDate effectiveDate) {}
}

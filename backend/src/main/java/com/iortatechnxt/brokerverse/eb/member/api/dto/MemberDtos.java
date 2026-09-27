package com.iortatechnxt.brokerverse.eb.member.api.dto;

import com.iortatechnxt.brokerverse.eb.domain.EbMember;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChange;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbRosterVersion;
import com.iortatechnxt.brokerverse.eb.member.service.MemberChangeInput;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of the roster and member change API (FR-EB-054 to 056). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class MemberDtos {

  private MemberDtos() {}

  /**
   * A roster version.
   *
   * @param id id
   * @param policyYear policy year
   * @param versionNo version
   * @param sourceRef upload number
   * @param status status
   * @param headcount members
   * @param loadedBy loaded by
   * @param loadedAt loaded at
   * @param decidedBy accepted or rejected by
   * @param decidedAt time
   * @param rejectReason reason
   */
  public record RosterResponse(
      Long id,
      int policyYear,
      int versionNo,
      String sourceRef,
      String status,
      int headcount,
      String loadedBy,
      Instant loadedAt,
      String decidedBy,
      Instant decidedAt,
      String rejectReason) {

    /**
     * Maps a version.
     *
     * @param v version
     * @return response
     */
    public static RosterResponse from(EbRosterVersion v) {
      return new RosterResponse(
          v.getId(),
          v.getPolicyYear(),
          v.getVersionNo(),
          v.getSourceRef(),
          v.getStatus().name(),
          v.getHeadcount(),
          v.getCreatedBy(),
          v.getCreatedAt(),
          v.getDecidedBy(),
          v.getDecidedAt(),
          v.getRejectReason());
    }
  }

  /**
   * A member.
   *
   * @param id id
   * @param employeeNo employee number
   * @param lastName last name
   * @param firstName first name
   * @param birthDate birth date
   * @param gender gender
   * @param civilStatus civil status
   * @param planCode plan
   * @param dependants dependants
   * @param effectiveFrom effective from
   * @param effectiveTo effective to
   * @param status status
   */
  public record MemberResponse(
      Long id,
      String employeeNo,
      String lastName,
      String firstName,
      LocalDate birthDate,
      String gender,
      String civilStatus,
      String planCode,
      int dependants,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      String status) {

    /**
     * Maps a member.
     *
     * @param m member
     * @return response
     */
    public static MemberResponse from(EbMember m) {
      return new MemberResponse(
          m.getId(),
          m.getEmployeeNo(),
          m.getLastName(),
          m.getFirstName(),
          m.getBirthDate(),
          m.getGender(),
          m.getCivilStatus(),
          m.getPlanCode(),
          m.getDependants(),
          m.getEffectiveFrom(),
          m.getEffectiveTo(),
          m.getStatus().name());
    }
  }

  /**
   * A reason.
   *
   * @param reason why
   */
  public record ReasonRequest(String reason) {}

  /**
   * A member change as captured (JSON field of the multipart request).
   *
   * @param lineNo programme line
   * @param policyYear roster year
   * @param source AO or CLIENT
   * @param financial premium effect
   * @param description description
   * @param lines lines
   */
  public record MemberChangeRequest(
      int lineNo,
      Integer policyYear,
      String source,
      boolean financial,
      String description,
      List<MemberChangeInput.Line> lines) {}

  /**
   * A member change.
   *
   * @param id id
   * @param changeNo number
   * @param programmeId programme
   * @param programmeNo programme number
   * @param clientName client
   * @param lineNo programme line
   * @param benefitLine benefit line
   * @param policyYear roster year
   * @param source source
   * @param financial premium effect
   * @param directBilled direct billing
   * @param status status
   * @param description description
   * @param relayedAt relayed at
   * @param billedOn billed on
   * @param billingRef insurer billing reference
   * @param billedAmount amount billed
   * @param validatedBy validated by
   * @param endorsementRequestNo endorsement request raised
   * @param createdBy captured by
   * @param createdAt captured at
   * @param lines lines
   */
  public record MemberChangeResponse(
      Long id,
      String changeNo,
      Long programmeId,
      String programmeNo,
      String clientName,
      int lineNo,
      String benefitLine,
      int policyYear,
      String source,
      boolean financial,
      boolean directBilled,
      String status,
      String description,
      Instant relayedAt,
      LocalDate billedOn,
      String billingRef,
      BigDecimal billedAmount,
      String validatedBy,
      String endorsementRequestNo,
      String createdBy,
      Instant createdAt,
      List<ChangeLine> lines) {

    /**
     * Maps a change.
     *
     * @param c change
     * @param programme its programme, may be null
     * @return response
     */
    public static MemberChangeResponse from(EbMemberChange c, EbProgramme programme) {
      return new MemberChangeResponse(
          c.getId(),
          c.getChangeNo(),
          c.getProgrammeId(),
          programme == null ? null : programme.getProgrammeNo(),
          programme == null ? null : programme.getClientName(),
          c.getLineNo(),
          c.getBenefitLine(),
          c.getPolicyYear(),
          c.getSource(),
          c.isFinancial(),
          c.isDirectBilled(),
          c.getStatus().name(),
          c.getDescription(),
          c.getRelayedAt(),
          c.getBilledOn(),
          c.getBillingRef(),
          c.getBilledAmount(),
          c.getValidatedBy(),
          c.getEndorsementRequestNo(),
          c.getCreatedBy(),
          c.getCreatedAt(),
          c.getLines().stream().map(ChangeLine::from).toList());
    }
  }

  /**
   * A line of a member change.
   *
   * @param sortOrder order
   * @param action action
   * @param employeeNo employee number
   * @param lastName last name
   * @param firstName first name
   * @param birthDate birth date
   * @param planCode plan
   * @param effectiveDate effective date
   * @param memberId roster member
   */
  public record ChangeLine(
      int sortOrder,
      String action,
      String employeeNo,
      String lastName,
      String firstName,
      LocalDate birthDate,
      String planCode,
      LocalDate effectiveDate,
      Long memberId) {

    static ChangeLine from(EbMemberChange.Line l) {
      return new ChangeLine(
          l.getSortOrder(),
          l.getAction().name(),
          l.getEmployeeNo(),
          l.getLastName(),
          l.getFirstName(),
          l.getBirthDate(),
          l.getPlanCode(),
          l.getEffectiveDate(),
          l.getMemberId());
    }
  }
}

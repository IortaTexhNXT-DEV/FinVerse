package com.iortatechnxt.brokerverse.submitted.setup.api.dto;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovalMatrix;
import com.iortatechnxt.brokerverse.submitted.domain.SbmInsurerRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLetterRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitRule;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmSource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmStatusMap;
import com.iortatechnxt.brokerverse.submitted.domain.SbmUserScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/** Records of the Submitted Policies setup API (FR-SP-080 to 084). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class SetupDtos {

  private SetupDtos() {}

  /**
   * The maker-checker state of a setup record.
   *
   * @param recordStatus status
   * @param maker maker
   * @param authorizedBy checker
   * @param authorizedAt approval
   */
  public record Control(
      String recordStatus, String maker, String authorizedBy, Instant authorizedAt) {

    /**
     * Reads the state of a record.
     *
     * @param e record
     * @return state
     */
    public static Control of(AuthorizableEntity e) {
      return new Control(
          e.getRecordStatus().name(), e.getMaker(), e.getAuthorizedBy(), e.getAuthorizedAt());
    }
  }

  /**
   * A limit rule.
   *
   * @param id id
   * @param limits limits
   * @param control maker-checker state
   */
  public record LimitView(Long id, SbmLimitRule.Limits limits, Control control) {

    /**
     * Maps a rule.
     *
     * @param r rule
     * @return view
     */
    public static LimitView from(SbmLimitRule r) {
      return new LimitView(
          r.getId(),
          new SbmLimitRule.Limits(
              r.getInsurerCode(),
              r.getSegment(),
              r.getLine(),
              r.getMaxSumInsured(),
              r.getMaxVehicleAge(),
              r.getAttribute(),
              r.getAttributeLimit(),
              r.getDescription()),
          Control.of(r));
    }
  }

  /**
   * An insurer rule.
   *
   * @param id id
   * @param row rule
   * @param control maker-checker state
   */
  public record InsurerView(Long id, SbmInsurerRule.Row row, Control control) {

    /**
     * Maps a rule.
     *
     * @param r rule
     * @return view
     */
    public static InsurerView from(SbmInsurerRule r) {
      return new InsurerView(
          r.getId(),
          new SbmInsurerRule.Row(
              r.getSegment(),
              r.getVehicleType(),
              r.getOccupancy(),
              r.getInsurerCode(),
              r.getPriority(),
              r.isExcludeExpiring(),
              r.getDescription()),
          Control.of(r));
    }
  }

  /**
   * A letter rule.
   *
   * @param id id
   * @param row rule
   * @param control maker-checker state
   */
  public record LetterView(Long id, SbmLetterRule.Row row, Control control) {

    /**
     * Maps a rule.
     *
     * @param r rule
     * @return view
     */
    public static LetterView from(SbmLetterRule r) {
      return new LetterView(
          r.getId(),
          new SbmLetterRule.Row(
              r.getLetterType(),
              r.getSegment(),
              r.getBucket(),
              r.getStatus(),
              r.getDaysFromExpiry(),
              r.getChannel(),
              r.getTemplateCode(),
              r.getDescription()),
          Control.of(r));
    }
  }

  /**
   * A level of the approval matrix.
   *
   * @param id id
   * @param row level
   * @param control maker-checker state
   */
  public record MatrixView(Long id, SbmApprovalMatrix.Row row, Control control) {

    /**
     * Maps a level.
     *
     * @param m level
     * @return view
     */
    public static MatrixView from(SbmApprovalMatrix m) {
      return new MatrixView(
          m.getId(),
          new SbmApprovalMatrix.Row(
              m.getDocument(),
              m.getSegment(),
              m.getTsiFrom(),
              m.getTsiTo(),
              m.getLevel(),
              m.getPermission(),
              m.getApproverUsername(),
              m.getSignatoryTitle()),
          Control.of(m));
    }
  }

  /**
   * A source of the register.
   *
   * @param id id
   * @param code code
   * @param name name
   * @param segment segment
   * @param businessType business type
   * @param bulkHandler upload
   * @param format format
   * @param mandatoryFields mandatory fields
   * @param active active
   */
  public record SourceView(
      Long id,
      String code,
      String name,
      String segment,
      String businessType,
      String bulkHandler,
      String format,
      String mandatoryFields,
      boolean active) {

    /**
     * Maps a source.
     *
     * @param s source
     * @return view
     */
    public static SourceView from(SbmSource s) {
      return new SourceView(
          s.getId(),
          s.getCode(),
          s.getName(),
          s.getSegment(),
          s.getBusinessType(),
          s.getBulkHandler(),
          s.getFormat(),
          s.getMandatoryFields(),
          s.isActive());
    }
  }

  /**
   * A change of a source.
   *
   * @param name name
   * @param mandatoryFields mandatory fields
   * @param active active
   */
  public record SourceRequest(@NotBlank String name, String mandatoryFields, boolean active) {}

  /**
   * A mapping of a legacy status.
   *
   * @param legacyStatus legacy status
   * @param status status
   * @param bucket bucket
   */
  public record StatusMapView(String legacyStatus, SbmPolicyStatus status, String bucket) {

    /**
     * Maps a mapping.
     *
     * @param m mapping
     * @return view
     */
    public static StatusMapView from(SbmStatusMap m) {
      return new StatusMapView(m.getLegacyStatus(), m.getStatus(), m.getBucket());
    }
  }

  /**
   * A new or changed mapping.
   *
   * @param legacyStatus legacy status
   * @param status status
   * @param bucket bucket
   */
  public record StatusMapRequest(
      @NotBlank String legacyStatus, @NotNull SbmPolicyStatus status, String bucket) {}

  /**
   * The scope of a user.
   *
   * @param username user
   * @param segments segments, empty for all
   * @param ownRecordsOnly own records only
   */
  public record ScopeView(String username, List<String> segments, boolean ownRecordsOnly) {

    /**
     * Maps a scope.
     *
     * @param s scope
     * @return view
     */
    public static ScopeView from(SbmUserScope s) {
      return new ScopeView(s.getUsername(), s.segmentList(), s.isOwnRecordsOnly());
    }
  }

  /**
   * A new or changed scope.
   *
   * @param username user
   * @param segments segments, empty for all
   * @param ownRecordsOnly own records only
   */
  public record ScopeRequest(
      @NotBlank String username, List<String> segments, boolean ownRecordsOnly) {}

  /**
   * The state of a record after an approval or deactivation.
   *
   * @param id record
   * @param control state
   */
  public record ControlView(Long id, Control control) {}
}

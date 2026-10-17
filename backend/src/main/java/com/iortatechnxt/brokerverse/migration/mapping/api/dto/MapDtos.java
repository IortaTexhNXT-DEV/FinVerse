package com.iortatechnxt.brokerverse.migration.mapping.api.dto;

import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapEntry;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapSet;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapVersion;
import com.iortatechnxt.brokerverse.migration.mapping.domain.EntryAction;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import com.iortatechnxt.brokerverse.migration.mapping.domain.LayoutColumn;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MaskingRule;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MigRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/** Request and response bodies of the code maps, layouts and rules (FR-DM-011, FR-DM-012). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class MapDtos {

  private MapDtos() {}

  /**
   * A code map set with its approved and open versions.
   *
   * @param code code
   * @param name name
   * @param targetDomain target domain
   * @param targetKind kind of target
   * @param ownerTitle owner
   * @param stewardTitle steward
   * @param usedBy objects using the set
   * @param approvedVersion approved version number, null when none
   * @param openVersionId draft or submitted version, null when none
   * @param openStatus its status
   */
  public record SetResponse(
      String code,
      String name,
      String targetDomain,
      String targetKind,
      String ownerTitle,
      String stewardTitle,
      String usedBy,
      Integer approvedVersion,
      Long openVersionId,
      String openStatus) {

    /**
     * Maps a set.
     *
     * @param s set
     * @param approved approved version
     * @param open draft or submitted version
     * @return response
     */
    public static SetResponse from(CodeMapSet s, CodeMapVersion approved, CodeMapVersion open) {
      return new SetResponse(
          s.getCode(),
          s.getName(),
          s.getTargetDomain(),
          s.getTargetKind().name(),
          s.getOwnerTitle(),
          s.getStewardTitle(),
          s.getUsedBy(),
          approved == null ? null : approved.getVersionNo(),
          open == null ? null : open.getId(),
          open == null ? null : open.getStatus().name());
    }
  }

  /**
   * A version.
   *
   * @param id id
   * @param setCode set
   * @param versionNo number
   * @param status status
   * @param comment comment
   * @param returnReason return reason
   * @param submittedBy submitter
   * @param submittedAt time
   * @param approvedBy approver
   * @param approvedAt time
   * @param createdBy creator
   */
  public record VersionResponse(
      Long id,
      String setCode,
      int versionNo,
      String status,
      String comment,
      String returnReason,
      String submittedBy,
      Instant submittedAt,
      String approvedBy,
      Instant approvedAt,
      String createdBy) {

    /**
     * Maps a version.
     *
     * @param v version
     * @return response
     */
    public static VersionResponse from(CodeMapVersion v) {
      return new VersionResponse(
          v.getId(),
          v.getSetCode(),
          v.getVersionNo(),
          v.getStatus().name(),
          v.getComment(),
          v.getReturnReason(),
          v.getSubmittedBy(),
          v.getSubmittedAt(),
          v.getApprovedBy(),
          v.getApprovedAt(),
          v.getCreatedBy());
    }
  }

  /**
   * An entry.
   *
   * @param id id
   * @param sourceSystem source system
   * @param legacyCode legacy code
   * @param legacyDescription legacy description
   * @param qualifier qualifier
   * @param qualifierValue qualifier value
   * @param action action
   * @param targetCode target
   * @param remarks remarks
   */
  public record EntryResponse(
      Long id,
      String sourceSystem,
      String legacyCode,
      String legacyDescription,
      String qualifier,
      String qualifierValue,
      String action,
      String targetCode,
      String remarks) {

    /**
     * Maps an entry.
     *
     * @param e entry
     * @return response
     */
    public static EntryResponse from(CodeMapEntry e) {
      return new EntryResponse(
          e.getId(),
          e.getSourceSystem(),
          e.getLegacyCode(),
          e.getLegacyDescription(),
          e.getQualifier(),
          e.getQualifierValue(),
          e.getAction().name(),
          e.getTargetCode(),
          e.getRemarks());
    }
  }

  /**
   * An entry to add or change.
   *
   * @param sourceSystem source system
   * @param legacyCode legacy code
   * @param legacyDescription description
   * @param qualifier qualifier
   * @param qualifierValue qualifier value
   * @param action action
   * @param targetCode target
   * @param remarks remarks
   */
  public record EntryRequest(
      @NotBlank String sourceSystem,
      @NotBlank String legacyCode,
      String legacyDescription,
      String qualifier,
      String qualifierValue,
      @NotNull EntryAction action,
      String targetCode,
      String remarks) {

    /**
     * The domain data.
     *
     * @return data
     */
    public CodeMapEntry.EntryData toData() {
      return new CodeMapEntry.EntryData(
          sourceSystem,
          legacyCode,
          legacyDescription,
          qualifier,
          qualifierValue,
          action,
          targetCode,
          remarks);
    }
  }

  /**
   * A new draft.
   *
   * @param copyApproved copy the approved entries
   * @param comment comment
   */
  public record DraftRequest(boolean copyApproved, String comment) {}

  /**
   * The approval result.
   *
   * @param version version
   * @param toCreate reference values to create through the reference-data load
   */
  public record ApprovalResponse(VersionResponse version, List<String> toCreate) {}

  /**
   * Differences with the approved version.
   *
   * @param added added entries
   * @param changed changed entries
   * @param removed removed entries
   */
  public record DiffResponse(
      List<EntryResponse> added, List<EntryResponse> changed, List<EntryResponse> removed) {}

  /**
   * An unmapped legacy code.
   *
   * @param setCode set
   * @param sourceSystem source system
   * @param legacyCode legacy code
   * @param rows rows using it
   * @param sampleKeys sample legacy keys
   * @param error found in a mandatory column
   */
  public record UnmappedResponse(
      String setCode,
      String sourceSystem,
      String legacyCode,
      long rows,
      String sampleKeys,
      boolean error) {}

  /**
   * A layout version.
   *
   * @param id id
   * @param code code
   * @param versionNo version
   * @param objectCode object
   * @param title title
   * @param keyColumns key
   * @param hashRule hash rule
   * @param hashColumns hash columns
   * @param amountColumns amount columns
   * @param status status
   * @param frozenBy frozen by
   * @param frozenAt time
   */
  public record LayoutResponse(
      Long id,
      String code,
      int versionNo,
      String objectCode,
      String title,
      String keyColumns,
      String hashRule,
      String hashColumns,
      String amountColumns,
      String status,
      String frozenBy,
      Instant frozenAt) {

    /**
     * Maps a layout.
     *
     * @param l layout
     * @return response
     */
    public static LayoutResponse from(Layout l) {
      return new LayoutResponse(
          l.getId(),
          l.getCode(),
          l.getVersionNo(),
          l.getObjectCode(),
          l.getTitle(),
          l.getKeyColumns(),
          l.getHashRule().name(),
          l.getHashColumns(),
          l.getAmountColumns(),
          l.getStatus().name(),
          l.getFrozenBy(),
          l.getFrozenAt());
    }
  }

  /**
   * A layout column.
   *
   * @param seq sequence
   * @param name name
   * @param description description
   * @param dataType type
   * @param length length
   * @param mandatory Y, N or C
   * @param allowedValues allowed values
   * @param mapSet code map set
   * @param format format
   * @param example example
   * @param target BIBS target
   * @param validation validation rule
   */
  public record ColumnResponse(
      int seq,
      String name,
      String description,
      String dataType,
      String length,
      String mandatory,
      String allowedValues,
      String mapSet,
      String format,
      String example,
      String target,
      String validation) {

    /**
     * Maps a column.
     *
     * @param c column
     * @return response
     */
    public static ColumnResponse from(LayoutColumn c) {
      return new ColumnResponse(
          c.getSeq(),
          c.getName(),
          c.getDescription(),
          c.getDataType().name(),
          c.getLength(),
          c.getMandatory(),
          c.getAllowedValues(),
          c.getMapSet(),
          c.getFormat(),
          c.getExample(),
          c.getTarget(),
          c.getValidation());
    }
  }

  /**
   * A rule.
   *
   * @param code code
   * @param layoutScope layouts
   * @param columns columns
   * @param kind kind
   * @param description description
   * @param severity severity
   * @param message message
   * @param fixedBy fixed by
   * @param active active
   */
  public record RuleResponse(
      String code,
      String layoutScope,
      String columns,
      String kind,
      String description,
      String severity,
      String message,
      String fixedBy,
      boolean active) {

    /**
     * Maps a rule.
     *
     * @param r rule
     * @return response
     */
    public static RuleResponse from(MigRule r) {
      return new RuleResponse(
          r.getCode(),
          r.getLayoutScope(),
          r.getColumns(),
          r.getKind(),
          r.getDescription(),
          r.getSeverity(),
          r.getMessage(),
          r.getFixedBy(),
          r.isActive());
    }
  }

  /**
   * A rule setting.
   *
   * @param severity ERROR or WARNING
   * @param active active
   */
  public record RuleRequest(@NotBlank String severity, boolean active) {}

  /**
   * A masking rule.
   *
   * @param id id
   * @param layoutCode layout
   * @param columnName column
   * @param rule kind
   * @param active active
   */
  public record MaskingResponse(
      Long id, String layoutCode, String columnName, String rule, boolean active) {

    /**
     * Maps a rule.
     *
     * @param m rule
     * @return response
     */
    public static MaskingResponse from(MaskingRule m) {
      return new MaskingResponse(
          m.getId(), m.getLayoutCode(), m.getColumnName(), m.getRule().name(), m.isActive());
    }
  }

  /**
   * A new masking rule.
   *
   * @param layoutCode layout
   * @param columnName column
   * @param rule kind
   */
  public record MaskingRequest(
      @NotBlank String layoutCode, @NotBlank String columnName, @NotNull MaskingRule.Kind rule) {}

  /**
   * Activation of a masking rule.
   *
   * @param active active
   */
  public record ToggleRequest(boolean active) {}
}

package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.report.core.MisDataElement.MisEntity;
import com.iortatechnxt.brokerverse.report.domain.MisField;
import com.iortatechnxt.brokerverse.report.domain.MisFieldRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The MIS fields catalogue (FR-CR-062): the System Administrator adds, changes, deactivates and
 * reactivates a field, another administrator authorizes it; the active fields give their label to
 * the report columns that carry their data element, in the Layout panel and in the grouping of the
 * reports (one label per field across all reports).
 */
@Service
@Transactional
public class MisFieldService {

  /** Audit entity of the catalogue. */
  public static final String ENTITY = "MisField";

  private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,39}");

  private final MisFieldRepository fields;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param fields catalogue
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public MisFieldService(
      MisFieldRepository fields, AuditTrailService audit, CurrentUser currentUser, Clock clock) {
    this.fields = fields;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The catalogue, by entity and code.
   *
   * @return fields
   */
  @Transactional(readOnly = true)
  public List<MisField> list() {
    return fields.findAllByOrderByEntityAscCodeAsc();
  }

  /**
   * The label a report column is shown with: the label of the active MIS field of its data element,
   * else its own label.
   *
   * @return labeller from column key to label, given the column's own label
   */
  @Transactional(readOnly = true)
  public ColumnLabels labels() {
    List<MisField> active = fields.findAll().stream().filter(MisField::isActive).toList();
    return (key, own) ->
        MisDataElement.ofColumn(key)
            .flatMap(e -> active.stream().filter(f -> f.getSource() == e).findFirst())
            .map(MisField::getLabel)
            .orElse(own);
  }

  /**
   * Adds a field, pending authorization.
   *
   * @param code unique code
   * @param label business label
   * @param entity core entity
   * @param source data element of the entity
   * @param lovType list of values, may be null
   * @return the field
   */
  public MisField create(
      String code, String label, MisEntity entity, MisDataElement source, String lovType) {
    String c = requireNewCode(code);
    requireSourceOf(entity, source);
    requireOnlyField(source, null);
    MisField f = fields.save(new MisField(c, requireLabel(label), entity, source, blank(lovType)));
    audit.record(ENTITY, c, AuditAction.CREATE, "MIS field " + f.getLabel() + " added");
    return f;
  }

  /**
   * Changes the label and list of values of a field; it waits for its authorization again.
   *
   * @param id field
   * @param label business label
   * @param lovType list of values, may be null
   * @return the field
   */
  public MisField change(Long id, String label, String lovType) {
    MisField f = get(id);
    f.change(requireLabel(label), blank(lovType));
    audit.record(ENTITY, f.getCode(), AuditAction.UPDATE, "MIS field changed");
    return f;
  }

  /**
   * Authorizes a pending field (not by its maker); it becomes active.
   *
   * @param id field
   * @return the field
   */
  public MisField authorize(Long id) {
    MisField f = get(id);
    requireOnlyField(f.getSource(), f.getId());
    f.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, f.getCode(), AuditAction.AUTHORIZE, "MIS field authorized");
    return f;
  }

  /**
   * Deactivates a field: the reports no longer offer it; saved layouts that grouped by it keep the
   * column's own label and the report runner warns of it.
   *
   * @param id field
   * @return the field
   */
  public MisField deactivate(Long id) {
    MisField f = get(id);
    if (f.getRecordStatus() == RecordStatus.INACTIVE) {
      throw new BusinessRuleException("MIS_FIELD_INACTIVE", "The MIS field is already inactive");
    }
    f.deactivate();
    audit.record(ENTITY, f.getCode(), AuditAction.DEACTIVATE, "MIS field deactivated");
    return f;
  }

  /**
   * Reactivates an inactive field; it waits for its authorization.
   *
   * @param id field
   * @return the field
   */
  public MisField reactivate(Long id) {
    MisField f = get(id);
    if (f.getRecordStatus() != RecordStatus.INACTIVE) {
      throw new BusinessRuleException(
          "MIS_FIELD_ACTIVE", "Only an inactive MIS field is reactivated");
    }
    f.markModified();
    audit.record(ENTITY, f.getCode(), AuditAction.UPDATE, "MIS field reactivation requested");
    return f;
  }

  private String requireNewCode(String code) {
    String c = code == null ? "" : code.strip();
    if (!CODE.matcher(c).matches()) {
      throw new BusinessRuleException(
          "MIS_FIELD_CODE", "Enter a code of capital letters, digits and underscores");
    }
    if (fields.findByCode(c).isPresent()) {
      throw new BusinessRuleException(
          "MIS_FIELD_EXISTS", "An MIS field with this code already exists");
    }
    return c;
  }

  private static void requireSourceOf(MisEntity entity, MisDataElement source) {
    if (source == null || entity == null || source.entity() != entity) {
      throw new BusinessRuleException(
          "MIS_FIELD_SOURCE", "Choose a data element of the entity of the field");
    }
  }

  private MisField get(Long id) {
    return fields.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /** One field per data element, so that a column has one label across all reports. */
  private void requireOnlyField(MisDataElement source, Long except) {
    Optional<MisField> other =
        fields.findAll().stream()
            .filter(f -> f.getSource() == source && !f.getId().equals(except))
            .filter(f -> f.getRecordStatus() != RecordStatus.INACTIVE)
            .findFirst();
    if (other.isPresent()) {
      throw new BusinessRuleException(
          "MIS_FIELD_SOURCE_USED",
          "The data element is already named by the MIS field " + other.get().getLabel());
    }
  }

  private static String requireLabel(String label) {
    if (label == null || label.isBlank()) {
      throw new BusinessRuleException("MIS_FIELD_LABEL", "Enter the label of the field");
    }
    return label.strip();
  }

  private static String blank(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }

  /** The label of a report column. */
  @FunctionalInterface
  public interface ColumnLabels {

    /**
     * The label of a column.
     *
     * @param key column key
     * @param own the column's own label
     * @return label to show
     */
    String label(String key, String own);

    /**
     * A labeller of a result's column labels, by own label (for the layout of one result).
     *
     * @param result result
     * @return own label to shown label
     */
    default UnaryOperator<String> of(ReportResult result) {
      return own ->
          result.columns().stream()
              .filter(c -> c.label().equals(own))
              .findFirst()
              .map(c -> label(c.key(), own))
              .orElse(own);
    }
  }
}

package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import com.iortatechnxt.brokerverse.migration.mapping.domain.LayoutColumn;
import com.iortatechnxt.brokerverse.migration.mapping.domain.LayoutColumnRepository;
import com.iortatechnxt.brokerverse.migration.mapping.domain.LayoutRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extract layouts (DATA_MIGRATION_DESIGN section 5.1): the FROZEN version of each layout code is
 * the one in force; a file whose header differs from it is rejected. The Data Steward freezes a
 * DRAFT version, which retires the previous one (part of gate G2).
 */
@Service
@Transactional
public class LayoutService {

  private final LayoutRepository layouts;
  private final LayoutColumnRepository columns;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param layouts layouts
   * @param columns layout columns
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public LayoutService(
      LayoutRepository layouts,
      LayoutColumnRepository columns,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.layouts = layouts;
    this.columns = columns;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Every layout version.
   *
   * @return versions by object and code
   */
  @Transactional(readOnly = true)
  public List<Layout> all() {
    return layouts.findAllByOrderByObjectCodeAscCodeAscVersionNoDesc();
  }

  /**
   * The layouts in force of an object.
   *
   * @param objectCode object
   * @return frozen versions
   */
  @Transactional(readOnly = true)
  public List<Layout> inForce(String objectCode) {
    return layouts.findByObjectCodeOrderByCodeAscVersionNoDesc(objectCode).stream()
        .filter(l -> l.getStatus() == Layout.Status.FROZEN)
        .toList();
  }

  /**
   * The version in force of a layout code.
   *
   * @param code layout code
   * @return frozen version
   */
  @Transactional(readOnly = true)
  public Optional<Layout> current(String code) {
    return layouts.findByCodeAndStatusOrderByVersionNoDesc(code, Layout.Status.FROZEN).stream()
        .findFirst();
  }

  /**
   * The version in force of a layout code, refused when none.
   *
   * @param code layout code
   * @return frozen version
   */
  @Transactional(readOnly = true)
  public Layout requireCurrent(String code) {
    return current(code)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "MIG_LAYOUT_NOT_FROZEN", "Layout " + code + " has no version in force"));
  }

  /**
   * A layout version.
   *
   * @param id id
   * @return layout
   */
  @Transactional(readOnly = true)
  public Layout get(Long id) {
    return layouts
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(MigrationCodes.ENTITY_LAYOUT, id));
  }

  /**
   * The columns of a layout version in file order.
   *
   * @param layoutId layout version
   * @return columns
   */
  @Transactional(readOnly = true)
  public List<LayoutColumn> columns(Long layoutId) {
    return columns.findByLayoutIdOrderBySeqAsc(layoutId);
  }

  /**
   * Freezes a draft layout version; the previous version in force is retired.
   *
   * @param id layout version
   * @return the frozen version
   */
  public Layout freeze(Long id) {
    Layout layout = get(id);
    if (columns(id).isEmpty()) {
      throw new BusinessRuleException("MIG_LAYOUT_EMPTY", "The layout has no columns");
    }
    current(layout.getCode())
        .ifPresent(
            previous -> {
              previous.retire();
              layouts.saveAndFlush(previous);
            });
    layout.freeze(currentUser.username(), clock.instant());
    audit.record(
        MigrationCodes.ENTITY_LAYOUT,
        layout.getCode() + " v" + layout.getVersionNo(),
        AuditAction.AUTHORIZE,
        "Frozen");
    return layout;
  }
}

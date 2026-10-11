package com.iortatechnxt.brokerverse.finreport.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Basis;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Comparative;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Grouping;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.LayoutStatus;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Measure;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.ScheduleFamily;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.SelectorKind;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Side;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * An account schedule of the BDOI report pack (FRBS 3.2.0, Appendix A II-IV; report list #9, #45,
 * #46): which accounts, how the rows are grouped, which figures are shown, optional ageing, a
 * comparative period and a commentary column. The engine ({@code finreport.service.ScheduleEngine})
 * turns a definition into the report {@code GL-SCHEDULE}, so a new schedule is configuration, not
 * code (design section 10).
 */
@Entity
@Table(name = "fin_schedule_def")
public class ScheduleDefinition extends BaseEntity {

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ScheduleFamily family;

  @Column(name = "source_ref", length = 80)
  private String sourceRef;

  @Column(length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "selector_kind", nullable = false, length = 20)
  private SelectorKind selectorKind;

  @Column(name = "account_selector", nullable = false, length = 500)
  private String accountSelector;

  @Enumerated(EnumType.STRING)
  @Column(name = "grouping_key", nullable = false, length = 20)
  private Grouping grouping;

  @Column(length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(name = "balance_side", nullable = false, length = 6)
  private Side side;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Basis basis;

  @Column(nullable = false)
  private boolean ageing;

  @Column(name = "ageing_slots", length = 60)
  private String ageingSlots;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Comparative comparative;

  @Column(nullable = false)
  private boolean commentary;

  @Column(name = "board_document", nullable = false)
  private boolean boardDocument;

  @Enumerated(EnumType.STRING)
  @Column(name = "layout_status", nullable = false, length = 20)
  private LayoutStatus layoutStatus;

  @Column(nullable = false)
  private boolean active;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "fin_schedule_column", joinColumns = @JoinColumn(name = "schedule_id"))
  @OrderBy("seq")
  private final List<ScheduleColumn> columns = new ArrayList<>();

  protected ScheduleDefinition() {}

  /**
   * A new definition.
   *
   * @param code unique code
   * @param values content
   */
  public ScheduleDefinition(String code, ScheduleValues values) {
    this.code = code;
    apply(values);
  }

  /**
   * Replaces the content.
   *
   * @param values content
   */
  public final void apply(ScheduleValues values) {
    name = values.name();
    family = values.family();
    sourceRef = values.sourceRef();
    description = values.description();
    selectorKind = values.selectorKind();
    accountSelector = values.accountSelector();
    grouping = values.grouping();
    currency = values.currency();
    side = values.side();
    basis = values.basis();
    ageing = values.ageingSlots() != null;
    ageingSlots = values.ageingSlots();
    comparative = values.comparative();
    commentary = values.commentary();
    boardDocument = values.boardDocument();
    layoutStatus = values.layoutStatus();
    active = values.active();
    columns.clear();
    columns.addAll(values.columns());
  }

  /**
   * The content as values.
   *
   * @return values
   */
  public ScheduleValues values() {
    return new ScheduleValues(
        name,
        family,
        sourceRef,
        description,
        selectorKind,
        accountSelector,
        grouping,
        currency,
        side,
        basis,
        ageingSlots,
        comparative,
        commentary,
        boardDocument,
        layoutStatus,
        active,
        columns);
  }

  /**
   * Whether the schedule shows a figure.
   *
   * @param measure figure
   * @return true when one of its columns
   */
  public boolean shows(Measure measure) {
    return columns.stream().anyMatch(c -> c.measure() == measure);
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public ScheduleFamily getFamily() {
    return family;
  }

  public Grouping getGrouping() {
    return grouping;
  }

  public String getCurrency() {
    return currency;
  }

  public Side getSide() {
    return side;
  }

  public Basis getBasis() {
    return basis;
  }

  public boolean isAgeing() {
    return ageing;
  }

  public String getAgeingSlots() {
    return ageingSlots;
  }

  public Comparative getComparative() {
    return comparative;
  }

  public boolean isCommentary() {
    return commentary;
  }

  public boolean isBoardDocument() {
    return boardDocument;
  }

  public LayoutStatus getLayoutStatus() {
    return layoutStatus;
  }

  public boolean isActive() {
    return active;
  }

  public List<ScheduleColumn> getColumns() {
    return List.copyOf(columns);
  }
}

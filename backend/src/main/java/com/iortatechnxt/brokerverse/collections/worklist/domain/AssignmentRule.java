package com.iortatechnxt.brokerverse.collections.worklist.domain;

import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A default assignment rule (BRCLXN.052): the handler of the new items matching segment, sales
 * unit, client, amount and aging criteria, tried in priority order by the daily refresh.
 */
@Entity
@Table(name = "clx_assignment_rule")
public class AssignmentRule extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false)
  private int priority;

  @Column(nullable = false, length = 120)
  private String name;

  @Embedded private RuleCriteria criteria;

  @Column(name = "handler_username", nullable = false, length = 50)
  private String handlerUsername;

  @Column(nullable = false)
  private boolean active = true;

  protected AssignmentRule() {}

  /**
   * Creates a rule.
   *
   * @param companyId company
   * @param details priority, name, criteria and handler
   */
  public AssignmentRule(Long companyId, Details details) {
    this.companyId = companyId;
    apply(details);
  }

  /**
   * Changes the rule.
   *
   * @param details new details
   */
  public void update(Details details) {
    apply(details);
  }

  private void apply(Details d) {
    this.priority = d.priority();
    this.name = d.name();
    this.criteria = d.criteria();
    this.handlerUsername = d.handlerUsername();
  }

  /**
   * Activates or deactivates the rule.
   *
   * @param value active
   */
  public void activate(boolean value) {
    this.active = value;
  }

  /**
   * Whether the rule applies to an item.
   *
   * @param item item
   * @return true when every criterion given matches
   */
  public boolean matches(CollectionItem item) {
    return active && getCriteria().matches(item);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public int getPriority() {
    return priority;
  }

  public String getName() {
    return name;
  }

  /**
   * The criteria; JPA reads an embeddable whose columns are all null as null.
   *
   * @return criteria, never null
   */
  public RuleCriteria getCriteria() {
    return criteria == null ? RuleCriteria.NONE : criteria;
  }

  public String getHandlerUsername() {
    return handlerUsername;
  }

  public boolean isActive() {
    return active;
  }

  /**
   * Maintainable attributes of a rule.
   *
   * @param priority lower first
   * @param name name
   * @param criteria criteria
   * @param handlerUsername handler
   */
  public record Details(int priority, String name, RuleCriteria criteria, String handlerUsername) {}
}

package com.iortatechnxt.brokerverse.collections.common.service;

import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange;
import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange.Origin;
import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange.Target;
import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange.Values;
import com.iortatechnxt.brokerverse.collections.common.domain.FieldChangeRepository;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * The Collections change recorder (BRCLXN.043, NFR audit logging): writes one {@code
 * clx_field_change} row per changed field with the "from" and "to" values, the user, the time and
 * the client IP of the request (none for background work). Every Collections module records its
 * changes through it, next to the summary in {@code AuditTrailService}.
 */
@Component
@Transactional
public class ChangeRecorder {

  private final FieldChangeRepository changes;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the recorder.
   *
   * @param changes change log
   * @param currentUser signed-in user
   * @param clock clock
   */
  public ChangeRecorder(FieldChangeRepository changes, CurrentUser currentUser, Clock clock) {
    this.changes = changes;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records a field change when the value differs.
   *
   * @param target changed record
   * @param field field name
   * @param before old value (any type; amounts as plain text)
   * @param after new value
   * @param bulkRef bulk reference, null for a single change
   * @return true when a change was recorded
   */
  public boolean record(Target target, String field, Object before, Object after, String bulkRef) {
    String from = text(before);
    String to = text(after);
    if (Objects.equals(from, to)) {
      return false;
    }
    changes.save(
        new FieldChange(
            target,
            field,
            new Values(from, to),
            new Origin(currentUser.username(), clock.instant(), sourceIp(), bulkRef)));
    return true;
  }

  /**
   * The field changes of an item, newest first.
   *
   * @param itemId item
   * @param pageable page
   * @return changes
   */
  @Transactional(readOnly = true)
  public Page<FieldChange> ofItem(Long itemId, Pageable pageable) {
    return changes.findByItemIdOrderByIdDesc(itemId, pageable);
  }

  static String text(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal amount) {
      return amount.toPlainString();
    }
    String s = String.valueOf(value);
    return s.isBlank() ? null : s;
  }

  private static String sourceIp() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    return attributes instanceof ServletRequestAttributes servlet
        ? servlet.getRequest().getRemoteAddr()
        : null;
  }
}

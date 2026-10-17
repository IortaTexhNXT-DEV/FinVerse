package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.service.TextLayout;
import com.iortatechnxt.brokerverse.bulk.service.TextLayout.FixedField;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentFileLayout;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentFileLayoutRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The configurable layouts of the payment files (CSHID.008; bank layouts parked, OQ03/OQ04): {@code
 * AUTO} (Excel, CSV or delimited TXT with a detected separator), {@code DELIMITED} with a given
 * separator and a header line, or {@code FIXED_WIDTH} with fields written {@code
 * Header:start:length} separated by {@code ;} (start is 1-based).
 */
@Service
@Transactional
public class PaymentFileLayouts {

  private static final String ENTITY = "PaymentFileLayout";
  private static final int FIELD_PARTS = 3;

  private final PaymentFileLayoutRepository layouts;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param layouts layout table
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PaymentFileLayouts(
      PaymentFileLayoutRepository layouts,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.layouts = layouts;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Every layout.
   *
   * @return layouts
   */
  @Transactional(readOnly = true)
  public List<PaymentFileLayout> list() {
    return layouts.findAll();
  }

  /**
   * The TXT layout of a handler.
   *
   * @param handlerCode handler
   * @return layout, AUTO when none is configured
   */
  @Transactional(readOnly = true)
  public TextLayout layout(String handlerCode) {
    return layouts.findById(handlerCode).map(PaymentFileLayouts::toText).orElse(TextLayout.AUTO);
  }

  /**
   * Changes a layout.
   *
   * @param handlerCode handler
   * @param kind AUTO, DELIMITED or FIXED_WIDTH
   * @param delimiter separator of a delimited file
   * @param fields fixed-width fields
   * @return the layout
   */
  public PaymentFileLayout change(
      String handlerCode, String kind, String delimiter, String fields) {
    PaymentFileLayout layout =
        layouts
            .findById(handlerCode)
            .orElseThrow(() -> new ResourceNotFoundException(ENTITY, handlerCode));
    layout.change(kind, delimiter, fields, currentUser.username(), clock.instant());
    toText(layout);
    audit.record(ENTITY, handlerCode, AuditAction.UPDATE, layout.getKind() + " " + fields);
    return layout;
  }

  /**
   * The bulk text layout of a stored layout.
   *
   * @param layout layout
   * @return text layout
   */
  static TextLayout toText(PaymentFileLayout layout) {
    return switch (layout.getKind()) {
      case "DELIMITED" -> TextLayout.delimited(layout.getDelimiter().charAt(0));
      case "FIXED_WIDTH" -> TextLayout.fixedWidth(fields(layout.getFields()));
      default -> TextLayout.AUTO;
    };
  }

  private static List<FixedField> fields(String text) {
    List<FixedField> fields = new ArrayList<>();
    for (String part : text.split(";")) {
      String[] p = part.strip().split(":");
      if (p.length != FIELD_PARTS) {
        throw new BusinessRuleException(
            "PAYMENT_LAYOUT_FIELDS", "Write each field as Header:start:length, not '" + part + "'");
      }
      try {
        fields.add(
            new FixedField(
                p[0].strip(), Integer.parseInt(p[1].strip()), Integer.parseInt(p[2].strip())));
      } catch (NumberFormatException ex) {
        throw new BusinessRuleException(
            "PAYMENT_LAYOUT_FIELDS", "Start and length of '" + part + "' must be numbers", ex);
      }
    }
    return fields;
  }
}

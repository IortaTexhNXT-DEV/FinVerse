package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Shared pieces of the quotation bulk handlers (BRNB.024/028/042/063): typed columns and row
 * validation in its own read-only transaction, so a rejected row never marks the upload's
 * transaction for rollback.
 */
@Component
public class QuotationBulkSupport {

  private final TransactionTemplate readOnly;

  /**
   * Creates the support.
   *
   * @param txManager transaction manager
   */
  public QuotationBulkSupport(PlatformTransactionManager txManager) {
    this.readOnly = new TransactionTemplate(txManager);
    this.readOnly.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.readOnly.setReadOnly(true);
  }

  /**
   * Runs a row check; a business rule failure becomes the row's error.
   *
   * @param check check returning error messages
   * @return error messages
   */
  public List<String> check(Supplier<List<String>> check) {
    try {
      List<String> errors = readOnly.execute(status -> check.get());
      return errors == null ? List.of() : errors;
    } catch (BusinessRuleException | ResourceNotFoundException e) {
      return List.of(e.getMessage());
    }
  }

  /**
   * A number column.
   *
   * @param header header
   * @param description what to enter
   * @param required mandatory
   * @param example example
   * @return column
   */
  public static BulkColumn numeric(
      String header, String description, boolean required, String example) {
    return new BulkColumn(header, description, required, BulkColumn.Type.NUMBER, example);
  }

  /**
   * A date column.
   *
   * @param header header
   * @param description what to enter
   * @param required mandatory
   * @return column
   */
  public static BulkColumn day(String header, String description, boolean required) {
    return new BulkColumn(header, description, required, BulkColumn.Type.DATE, "2026-10-15");
  }

  /**
   * A Y/N column.
   *
   * @param header header
   * @param description what to enter
   * @return column
   */
  public static BulkColumn flag(String header, String description) {
    return new BulkColumn(header, description, false, BulkColumn.Type.YES_NO, "N");
  }

  /**
   * Whether a parameter value means yes.
   *
   * @param value Y, YES or TRUE in any case
   * @return true for yes
   */
  public static boolean yes(String value) {
    return value != null
        && List.of("Y", "YES", "TRUE").contains(value.strip().toUpperCase(Locale.ROOT));
  }
}

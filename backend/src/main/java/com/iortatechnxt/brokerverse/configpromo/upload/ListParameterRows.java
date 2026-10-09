package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.system.domain.ParameterValueType;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * The values of the lists of values and the system parameters loaded by the uploads of the
 * configuration screens (PM-09, UA-05, UA-06): a list value is added or updated by list and code
 * under the maker-checker of the upload; a parameter changes through the System Parameters service
 * (type and range checks, audit, cache).
 */
@Component
public class ListParameterRows {

  private static final String TABLE = "lov_value";
  private static final Pattern VALUE_CODE = Pattern.compile("[A-Z0-9_]{1,40}");

  private final UploadSupport db;
  private final SystemParameterService parameters;

  /**
   * Creates the helper.
   *
   * @param db database support
   * @param parameters system parameters
   */
  public ListParameterRows(UploadSupport db, SystemParameterService parameters) {
    this.db = db;
    this.parameters = parameters;
  }

  /**
   * Whether a list exists.
   *
   * @param type list code
   * @return true when it exists
   */
  boolean isList(String type) {
    return db.exists("select 1 from lov_type where code = ?", type);
  }

  /**
   * Checks a value of a list.
   *
   * @param code value code
   * @param maxLength longest code allowed
   * @return error, empty when valid
   */
  Optional<String> checkCode(String code, int maxLength) {
    if (code == null || !VALUE_CODE.matcher(code).matches() || code.length() > maxLength) {
      return Optional.of("use up to " + maxLength + " capital letters, digits or _");
    }
    return Optional.empty();
  }

  /**
   * The natural key of a list value.
   *
   * @param type list
   * @param code value
   * @return key
   */
  static Map<String, Object> key(String type, String code) {
    return ConfigUploadHandler.columns("type_code", type, "code", code);
  }

  /**
   * ADD or UPDATE for a list value.
   *
   * @param type list
   * @param code value
   * @return action
   */
  String valueAction(String type, String code) {
    return db.action(TABLE, key(type, code));
  }

  /**
   * Adds or updates a list value.
   *
   * @param type list
   * @param code value
   * @param label label
   * @param order sort order, null to keep (0 for a new value)
   * @param active false to deactivate the value
   * @param from effective from, null to keep (the business date for a new value)
   * @param context upload
   */
  void saveValue(
      String type,
      String code,
      String label,
      Integer order,
      boolean active,
      LocalDate from,
      BulkContext context) {
    boolean added = UploadSupport.ADD.equals(valueAction(type, code));
    Map<String, Object> values = ConfigUploadHandler.columns("label", label);
    if (order != null || added) {
      values.put("sort_order", order == null ? Integer.valueOf(0) : order);
    }
    if (from != null || added) {
      values.put("effective_from", from == null ? context.businessDate() : from);
    }
    values.put("record_status", active ? "ACTIVE" : "INACTIVE");
    db.upsert(TABLE, key(type, code), values, context, "List value " + type);
  }

  /**
   * The values of lists, for an export.
   *
   * @param types lists
   * @return rows with type_code, code, label, sort_order, effective_from, record_status
   */
  List<Map<String, Object>> values(List<String> types) {
    return db.rows(
        "select type_code, code, label, sort_order, effective_from, record_status from lov_value"
            + " where type_code = any (?) order by type_code, sort_order, code",
        (Object) types.toArray(String[]::new));
  }

  /**
   * Whether a parameter exists.
   *
   * @param key parameter
   * @return true when it exists
   */
  boolean isParameter(String key) {
    return db.exists("select 1 from sys_parameter where param_key = ?", key);
  }

  /**
   * The current value of a parameter.
   *
   * @param key parameter
   * @return value
   */
  Optional<String> parameterValue(String key) {
    return db.text("select param_value from sys_parameter where param_key = ?", key);
  }

  /**
   * Checks a parameter value against the type and the range of the parameter, and that no change of
   * it waits for approval on System Parameters.
   *
   * @param key parameter
   * @param value value
   * @return error, empty when valid
   */
  Optional<String> checkParameter(String key, String value) {
    List<Map<String, Object>> found =
        db.rows(
            "select value_type, min_value, max_value, pending_value from sys_parameter where param_key = ?",
            key);
    if (found.isEmpty()) {
      return Optional.of(key + " is not a parameter");
    }
    Map<String, Object> p = found.get(0);
    if (parameterValue(key).map(v -> v.equals(value == null ? "" : value.trim())).orElse(false)) {
      return Optional.empty();
    }
    if (p.get("pending_value") != null) {
      return Optional.of("a change of " + key + " already waits for approval on System Parameters");
    }
    return ParameterValueType.valueOf((String) p.get("value_type"))
        .validate(
            value == null ? "" : value.trim(),
            number(p.get("min_value")),
            number(p.get("max_value")));
  }

  /**
   * Sets a parameter (audited by the System Parameters service).
   *
   * @param key parameter
   * @param value value
   * @return true when the value changed
   */
  boolean saveParameter(String key, String value) {
    String wanted = value == null ? "" : value.trim();
    if (parameterValue(key).map(wanted::equals).orElse(false)) {
      return false;
    }
    parameters.update(key, wanted);
    return true;
  }

  private static Integer number(Object value) {
    return value == null ? null : ((Number) value).intValue();
  }
}

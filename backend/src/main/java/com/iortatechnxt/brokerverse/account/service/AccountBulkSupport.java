package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.InsuredItem;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Location;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Person;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Vehicle;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Shared pieces of the account bulk handlers (BRNB.024/025/039/052/064/066/113): column headers,
 * identifier sanitising, risk item mapping and row validation in its own read-only transaction so a
 * rejected row never marks the upload transaction for rollback.
 */
@Component
public class AccountBulkSupport {

  /** Column headers shared by the account uploads. */
  static final class Headers {
    static final String ARN = "ARN";
    static final String CLIENT_CODE = "Client Code";
    static final String CLIENT_NAME = "Client Name";
    static final String BIRTH_DATE = "Birth Date";
    static final String SEGMENT = "Market Segment";
    static final String PERIOD_FROM = "Period From";
    static final String PERIOD_TO = "Period To";
    static final String SUM_INSURED = "Sum Insured";
    static final String RATE = "Rate %";
    static final String PLATE = "Plate No";
    static final String CONDUCTION = "Conduction Sticker";
    static final String ENGINE = "Engine No";
    static final String CHASSIS = "Chassis No";
    static final String MAKE = "Make";
    static final String MODEL = "Model";
    static final String YEAR = "Year Model";
    static final String BODY = "Body Type";
    static final String ADDRESS = "Address";
    static final String CITY = "City";
    static final String PROVINCE = "Province";
    static final String OCCUPANCY = "Occupancy";
    static final String CONSTRUCTION = "Construction Class";
    static final String ITEMS_INSURED = "Items Insured";
    static final String DESCRIPTION = "Description";
    static final String PERSON = "Insured Person";
    static final String INSURER = "Insurer Code";
    static final String BRANCH = "Insurer Branch";
    static final String MORTGAGEE = "Mortgagee Bank";
    static final String LOAN = "Loan Application No";
    static final String PN = "PN Numbers";
    static final String QUOTATION = "Quotation Ref";
    static final String FFY_START = "FFY Start";
    static final String DIRECT_PAYMENT = "Direct Payment";
    static final String BUSINESS_TYPE = "Business Type";
    static final String RENEWAL_OF = "Renewal Of";
    static final String SUBMIT = "Submit";
    static final String CONTACT_EMAIL = "Contact Email";
    static final String CONTACT_MOBILE = "Contact Mobile";

    private Headers() {}
  }

  /** Parameter: risk code of the accounts. */
  static final String PRODUCT = "product";

  /** Parameter: default market segment. */
  static final String SEGMENT = "segment";

  /** Parameter: submit the accounts to Processing on commit. */
  static final String SUBMIT = "submit";

  private static final String MOTOR =
      "the product is a motor product and its minimum fields ask for it";
  private static final String FIRE =
      "the product is a fire product and its minimum fields ask for it";

  /** Who fills the account templates in. */
  static final String FILLED_BY =
      "Account officers of the branches and the New Business processing team";

  private static final Set<String> IDENTIFIERS =
      Set.of(Headers.PLATE, Headers.CONDUCTION, Headers.ENGINE, Headers.CHASSIS);
  private static final String LIST_SEPARATOR = "[;|]";
  private static final Set<String> YES = Set.of("Y", "y", "true", "TRUE", "True");

  private final TransactionTemplate checks;

  /**
   * Creates the support.
   *
   * @param txManager transaction manager
   */
  public AccountBulkSupport(PlatformTransactionManager txManager) {
    this.checks = new TransactionTemplate(txManager);
    this.checks.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.checks.setReadOnly(true);
  }

  /**
   * Runs a row validation in its own read-only transaction: a business rule failure becomes the
   * row's error message instead of marking the upload's transaction for rollback.
   *
   * @param validation validation returning error messages
   * @return error messages
   */
  List<String> safely(Supplier<List<String>> validation) {
    try {
      List<String> errors = checks.execute(s -> validation.get());
      return errors == null ? List.of() : errors;
    } catch (BusinessRuleException | ResourceNotFoundException e) {
      return List.of(e.getMessage());
    }
  }

  /**
   * Sanitises a value: vehicle identifiers are upper-cased without spaces or dashes.
   *
   * @param header column
   * @param clean value already trimmed by the framework
   * @return value
   */
  static String sanitize(String header, String clean) {
    return IDENTIFIERS.contains(header) ? BulkImportHandler.identifier(clean) : clean;
  }

  /**
   * Whether the submit parameter asks to submit on commit.
   *
   * @param value parameter value
   * @return true for Y / true
   */
  static boolean yes(String value) {
    return value != null && YES.contains(value);
  }

  /**
   * Columns describing one risk item (vehicle, location, person or generic).
   *
   * @return columns
   */
  static List<BulkColumn> itemColumns() {
    return List.of(
        number(Headers.SUM_INSURED, "Sum insured of the item", true, "850000"),
        number(Headers.RATE, "Premium rate in percent; blank for the product's rate", false, "1.3")
            .format("Number in percent, e.g. 1.3 for 1.3%"),
        BulkColumn.optional(Headers.PLATE, "Motor: plate number", "ABC 1234")
            .when(MOTOR + " (plate number or conduction sticker)"),
        BulkColumn.optional(Headers.CONDUCTION, "Motor: conduction sticker of a new vehicle", "")
            .when(MOTOR + " (new vehicle without plate)"),
        BulkColumn.optional(Headers.ENGINE, "Motor: engine (motor) number", "4A91-123456")
            .when(MOTOR),
        BulkColumn.optional(Headers.CHASSIS, "Motor: chassis (serial) number", "MHF12345678")
            .when(MOTOR),
        BulkColumn.optional(Headers.MAKE, "Motor: make of the vehicle", "Toyota").when(MOTOR),
        BulkColumn.optional(Headers.MODEL, "Motor: model of the vehicle", "Vios 1.3 E").when(MOTOR),
        number(Headers.YEAR, "Motor: year model", false, "2026")
            .when(MOTOR)
            .format("Year, 4 digits"),
        BulkColumn.optional(Headers.BODY, "Motor: body type", "SEDAN")
            .when(MOTOR)
            .lov("VEHICLE_BODY_TYPE"),
        BulkColumn.optional(Headers.ADDRESS, "Fire: full address of the risk", "12 Mabini St.")
            .when(FIRE),
        BulkColumn.optional(Headers.CITY, "Fire: city or municipality", "Makati").when(FIRE),
        BulkColumn.optional(Headers.PROVINCE, "Fire: province", "Metro Manila").when(FIRE),
        BulkColumn.optional(Headers.OCCUPANCY, "Fire: occupancy of the risk", "DWELLING")
            .when(FIRE)
            .lov("OCCUPANCY"),
        BulkColumn.optional(Headers.CONSTRUCTION, "Fire: construction class", "CLASS_1")
            .when(FIRE)
            .lov("CONSTRUCTION_CLASS"),
        BulkColumn.optional(
            Headers.ITEMS_INSURED, "Fire: item insured for the sum on the row", "Building"),
        BulkColumn.optional(Headers.DESCRIPTION, "Other lines: description of the risk", "")
            .when("the product is neither motor, fire nor personal accident"),
        BulkColumn.optional(Headers.PERSON, "Personal accident: name of the insured person", "")
            .when("the product is a personal accident product"));
  }

  /**
   * A number column.
   *
   * @param header header
   * @param description description
   * @param required mandatory
   * @param example example
   * @return column
   */
  static BulkColumn number(String header, String description, boolean required, String example) {
    return new BulkColumn(header, description, required, BulkColumn.Type.NUMBER, example);
  }

  /**
   * A date column.
   *
   * @param header header
   * @param description description
   * @param required mandatory
   * @return column
   */
  static BulkColumn date(String header, String description, boolean required) {
    return new BulkColumn(header, description, required, BulkColumn.Type.DATE, "2026-10-01");
  }

  /**
   * The risk item of a row (one item per row).
   *
   * @param row row
   * @return item
   */
  static RiskItemData item(BulkRow row) {
    BigDecimal sum = row.number(Headers.SUM_INSURED);
    BigDecimal year = row.number(Headers.YEAR);
    String described = row.text(Headers.ITEMS_INSURED);
    List<InsuredItem> insured =
        described == null
            ? List.of()
            : List.of(new InsuredItem(described, sum == null ? BigDecimal.ZERO : sum));
    return new RiskItemData(
        row.text(Headers.DESCRIPTION),
        sum,
        row.number(Headers.RATE),
        null,
        null,
        new Vehicle(
            row.text(Headers.PLATE),
            row.text(Headers.CONDUCTION),
            row.text(Headers.ENGINE),
            row.text(Headers.CHASSIS),
            row.text(Headers.MAKE),
            row.text(Headers.MODEL),
            year == null ? null : year.intValue(),
            row.text(Headers.BODY),
            null,
            null),
        new Location(
            row.text(Headers.ADDRESS),
            row.text(Headers.CITY),
            row.text(Headers.PROVINCE),
            row.text(Headers.OCCUPANCY),
            row.text(Headers.CONSTRUCTION),
            insured),
        new Person(row.text(Headers.PERSON), null, null));
  }

  /**
   * Values of a list cell separated by ; or |.
   *
   * @param value cell
   * @return values, empty when blank
   */
  static List<String> list(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split(LIST_SEPARATOR))
        .map(String::strip)
        .filter(v -> !v.isEmpty())
        .toList();
  }

  /**
   * The first vehicle identifier or address of a row (duplicates inside one file).
   *
   * @param row row
   * @return key or null
   */
  static String riskKey(BulkRow row) {
    for (String header :
        List.of(Headers.PLATE, Headers.CONDUCTION, Headers.ENGINE, Headers.CHASSIS)) {
      if (row.text(header) != null) {
        return header + ":" + row.text(header);
      }
    }
    String address = row.text(Headers.ADDRESS);
    return address == null ? null : "ADDRESS:" + address.toLowerCase(Locale.ROOT);
  }
}

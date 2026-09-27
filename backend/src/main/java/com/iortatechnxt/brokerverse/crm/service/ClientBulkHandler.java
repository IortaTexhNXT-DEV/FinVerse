package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientProfile;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.service.ClientRules.Violation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bulk client creation and update {@value #CODE} (BRNB.047/065): a row with a prospect or client
 * code updates that client (blank cells keep the current value); a row without a code updates the
 * client it matches on a hard duplicate key (TIN, ID, name with birth date), otherwise it creates a
 * prospect. Rows are validated like the client screen, with one message per problem.
 */
@Component
public class ClientBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "CLIENT_CREATE";

  static final String CLIENT_CODE = "Client Code";
  static final String TYPE = "Client Type";
  static final String LAST = "Last Name";
  static final String FIRST = "First Name";
  static final String MIDDLE = "Middle Name";
  static final String CORPORATE = "Corporate Name";
  static final String BIRTH = "Birth Date";
  static final String TIN = "TIN";
  static final String ID_TYPE = "ID Type";
  static final String ID_NUMBER = "ID Number";
  static final String EMAIL = "E-mail";
  static final String MOBILE = "Mobile";
  static final String ADDRESS = "Address";
  static final String CITY = "City";
  static final String PROVINCE = "Province";
  static final String POSTAL = "Postal Code";
  static final String SEGMENT = "Market Segment";
  static final String BANK = "Bank Client";
  static final String CIF = "Bank CIF";
  static final String NATIONALITY = "Nationality";
  static final String SOURCE = "Source of Funds";
  static final String RISK = "Risk Rating";

  private static final Set<String> CODED =
      Set.of(CLIENT_CODE, TYPE, ID_TYPE, SEGMENT, NATIONALITY, SOURCE, RISK);

  private final ClientService clients;
  private final ClientRepository repository;
  private final ClientValidator validator;
  private final DuplicateCheckService duplicates;

  /**
   * Creates the handler.
   *
   * @param clients client service
   * @param repository clients (lookups that must not fail the validation transaction)
   * @param validator client validation
   * @param duplicates duplicate detection
   */
  public ClientBulkHandler(
      ClientService clients,
      ClientRepository repository,
      ClientValidator validator,
      DuplicateCheckService duplicates) {
    this.clients = clients;
    this.repository = repository;
    this.validator = validator;
    this.duplicates = duplicates;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Bulk client creation and update";
  }

  @Override
  public String permission() {
    return "CLIENT_MAINTAIN";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.optional(
            CLIENT_CODE, "Prospect or client code to update; blank to create or match", ""),
        BulkColumn.required(TYPE, "INDIVIDUAL or CORPORATE", "INDIVIDUAL"),
        BulkColumn.optional(LAST, "Last name (individual)", "Reyes"),
        BulkColumn.optional(FIRST, "First name (individual)", "Ana"),
        BulkColumn.optional(MIDDLE, "Middle name", "Cruz"),
        BulkColumn.optional(CORPORATE, "Registered name (corporate)", ""),
        new BulkColumn(BIRTH, "Birth date (individual)", false, BulkColumn.Type.DATE, "1990-04-15"),
        BulkColumn.optional(TIN, "TIN as 000-000-000-000", "123-456-789-000"),
        BulkColumn.optional(ID_TYPE, "ID type code (list ID_TYPE)", "PASSPORT"),
        BulkColumn.optional(ID_NUMBER, "ID number", "P1234567A"),
        BulkColumn.optional(EMAIL, "E-mail", "ana.reyes@example.ph"),
        BulkColumn.optional(MOBILE, "Mobile as 09xxxxxxxxx or +639xxxxxxxxx", "09171234567"),
        BulkColumn.optional(ADDRESS, "Street address", "12 Rizal St."),
        BulkColumn.optional(CITY, "City / municipality", "Pasig"),
        BulkColumn.optional(PROVINCE, "Province", "Metro Manila"),
        BulkColumn.optional(POSTAL, "Postal code", "1600"),
        BulkColumn.optional(SEGMENT, "Market segment code (list MARKET_SEGMENT)", "CBG"),
        new BulkColumn(BANK, "BDO bank client", false, BulkColumn.Type.YES_NO, "Y"),
        BulkColumn.optional(CIF, "BDO customer information file number", ""),
        BulkColumn.optional(NATIONALITY, "Nationality code (list NATIONALITY)", "FILIPINO"),
        BulkColumn.optional(SOURCE, "Source of funds code (list SOURCE_OF_FUNDS)", "SALARY"),
        BulkColumn.optional(RISK, "Risk rating code (list KYC_RISK_RATING)", "STANDARD"));
  }

  @Override
  public String instructions() {
    return "Rows with a client code update that client; blank cells keep the current values. "
        + "Rows without a code update the client found by TIN, ID or name and birth date, "
        + "otherwise a prospect is created.";
  }

  @Override
  public String sanitize(String header, String value) {
    String clean = BulkImportHandler.super.sanitize(header, value);
    return CODED.contains(header) ? clean.toUpperCase(Locale.ROOT) : clean;
  }

  @Override
  public String duplicateKey(BulkRow row) {
    if (row.text(CLIENT_CODE) != null) {
      return row.text(CLIENT_CODE);
    }
    return row.text(TIN);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    ClientType type = typeOf(row.text(TYPE));
    if (type == null) {
      errors.add("Client Type must be INDIVIDUAL or CORPORATE");
      return errors;
    }
    Optional<Client> existing = existing(row, context, errors);
    ClientDetails details = BulkClientRows.details(row, type, existing.orElse(null));
    errors.addAll(BulkClientRows.nameErrors(details));
    for (Violation v :
        validator.violations(details, BulkClientRows.profile(row, existing.orElse(null)))) {
      errors.add(v.message());
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    ClientType type = typeOf(row.text(TYPE));
    Optional<Client> existing = existing(row, context, new ArrayList<>());
    ClientDetails details = BulkClientRows.details(row, type, existing.orElse(null));
    ClientProfile profile = BulkClientRows.profile(row, existing.orElse(null));
    Client saved =
        existing.isPresent()
            ? clients.update(existing.get().getId(), details, profile)
            : clients.create(context.companyId(), details, profile);
    return saved.getCode();
  }

  private Optional<Client> existing(BulkRow row, BulkContext context, List<String> errors) {
    String code = row.text(CLIENT_CODE);
    if (code != null) {
      Optional<Client> byCode = repository.findByCode(context.companyId(), code);
      if (byCode.isEmpty()) {
        errors.add("Client code " + code + " does not exist");
      }
      return byCode;
    }
    ClientType type = typeOf(row.text(TYPE));
    List<DuplicateMatch> hard =
        duplicates
            .candidates(
                context.companyId(),
                DuplicateProbe.of(BulkClientRows.details(row, type, null)),
                null)
            .stream()
            .filter(DuplicateMatch::hard)
            .toList();
    if (hard.size() > 1) {
      errors.add(
          "The row matches several clients: "
              + String.join(", ", hard.stream().map(DuplicateMatch::code).toList()));
      return Optional.empty();
    }
    return hard.stream().findFirst().flatMap(m -> repository.findById(m.clientId()));
  }

  private static ClientType typeOf(String value) {
    if ("INDIVIDUAL".equals(value)) {
      return ClientType.INDIVIDUAL;
    }
    return "CORPORATE".equals(value) ? ClientType.CORPORATE : null;
  }
}

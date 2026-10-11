package com.iortatechnxt.brokerverse.renewal.insurer.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponseCode;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_INSURER_DISPOSITION} (FRRN.013.01, Annex H): the insurer disposition file
 * of CBG (Annex H) or Non-CBG (the insurer disposition request file with the insurer's approval,
 * insurer, branch and remarks); each record is matched and applied immediately.
 */
@Component
public class InsurerDispositionHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_INSURER_DISPOSITION";

  private static final String RENEWAL_REF = "Renewal Reference Number";
  private static final String EXPIRING_REF = "Expiring Reference Number";
  private static final String POLICY = "Expiring Policy No.";
  private static final String APPROVAL = "Insurer's Approval";
  private static final String INSURER = "Insurer";
  private static final String BRANCH = "Insurer's Branch";
  private static final String REMARKS = "Insurer Remarks";
  private static final String TSI = "TSI";
  private static final String RATE = "Premium Rate";

  private final InsurerDispositionService service;
  private final RenewalInsurerResponseService responses;

  /**
   * Creates the handler.
   *
   * @param service insurer dispositions
   * @param responses checks of an insurer response
   */
  public InsurerDispositionHandler(
      InsurerDispositionService service, RenewalInsurerResponseService responses) {
    this.service = service;
    this.responses = responses;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - insurer disposition file";
  }

  @Override
  public String permission() {
    return Permission.RNW_INSURER.name();
  }

  @Override
  public String filledBy() {
    return "The insurer, on the insurer disposition request file";
  }

  @Override
  public String uploadPath() {
    return "Renewal > Renewal Home, button Insurer Disposition Upload";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        new BulkColumn("Item No.", "Sequence of the record", false, BulkColumn.Type.NUMBER, "1"),
        BulkColumn.optional("Assured's Name", "Name of the assured", "Juan Dela Cruz"),
        BulkColumn.optional(RENEWAL_REF, "Renewal reference number", "RNW-2027-000001"),
        BulkColumn.optional(EXPIRING_REF, "Reference of the expiring account", "INV-2026-000123"),
        BulkColumn.optional(POLICY, "Policy number of the expiring account", "MC-2026-000123"),
        new BulkColumn(
            "Inception Date", "Start of the cover", false, BulkColumn.Type.DATE, "2027-03-01"),
        new BulkColumn(
            "Expiry Date", "End of the cover", false, BulkColumn.Type.DATE, "2028-03-01"),
        BulkColumn.optional("Location Of Risk", "Address of the risk (property)", ""),
        BulkColumn.optional("Unit Description", "Vehicle", "2024 SEDAN"),
        BulkColumn.optional("Serial No.", "Serial number (motor)", ""),
        BulkColumn.optional("Motor No.", "Motor number (motor)", ""),
        BulkColumn.optional("Plate No.", "Plate number", ""),
        BulkColumn.optional("Color", "Colour of the vehicle", ""),
        new BulkColumn(TSI, "Total sum insured", false, BulkColumn.Type.NUMBER, "900000"),
        new BulkColumn(RATE, "Premium rate in percent", false, BulkColumn.Type.NUMBER, "2.376"),
        BulkColumn.optional(INSURER, "Insurer", "INS-MGIC"),
        BulkColumn.optional(BRANCH, "Insurer's branch", "Head Office"),
        BulkColumn.required(APPROVAL, "Insurer disposition", "Renew As Is / Approved")
            .values(
                service.values().stream()
                    .map(InsurerDispositionService.Value::label)
                    .toArray(String[]::new)),
        BulkColumn.optional(REMARKS, "Remarks of the insurer", ""));
  }

  @Override
  public Set<String> optionalHeaders() {
    return columns().stream()
        .filter(c -> !c.required())
        .map(BulkColumn::header)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public List<String> outcomeCategories() {
    return List.of("MATCHED", "UNMATCHED");
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    if (service.value(row.text(APPROVAL)).isEmpty()) {
      errors.add("Insurer's Approval " + row.text(APPROVAL) + " is not an insurer disposition");
    }
    if (row.text(RENEWAL_REF) == null
        && row.text(EXPIRING_REF) == null
        && row.text(POLICY) == null) {
      errors.add(
          "Record not found: give the renewal reference, expiring reference or policy number");
      return errors;
    }
    Optional<InsurerDispositionService.Match> m = service.find(context.companyId(), keys(row));
    if (m.isEmpty()) {
      errors.add("Record not found");
    } else if (errors.isEmpty()) {
      errors.addAll(
          responses.problems(
              m.get().candidate(),
              new InsurerResponse.Content(
                  InsurerResponseCode.RENEW_AS_IS, null, null, null, null, null, null, null)));
    }
    return errors;
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    InsurerDispositionService.Match m = service.find(context.companyId(), keys(row)).orElseThrow();
    return service.apply(
        context.companyId(),
        new InsurerDispositionService.Source(context.jobNo(), row.rowNo(), row.text(POLICY)),
        m,
        new InsurerDispositionService.Record(
            service.value(row.text(APPROVAL)).orElseThrow(),
            row.text(INSURER),
            row.text(BRANCH),
            row.text(REMARKS),
            row.number(TSI),
            row.number(RATE)));
  }

  private static InsurerDispositionService.Keys keys(BulkRow row) {
    return new InsurerDispositionService.Keys(
        row.text(RENEWAL_REF), row.text(EXPIRING_REF), row.text(POLICY));
  }
}

package com.iortatechnxt.brokerverse.productmaint.api;

import com.iortatechnxt.brokerverse.productmaint.api.dto.OutputDtos.ClientViewBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.OutputDtos.OutputView;
import com.iortatechnxt.brokerverse.productmaint.api.dto.OutputDtos.RequirementsBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.OutputDtos.RequirementsView;
import com.iortatechnxt.brokerverse.productmaint.api.dto.OutputDtos.SetupBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.OutputDtos.SignoffView;
import com.iortatechnxt.brokerverse.productmaint.api.dto.RequestDtos.CommentBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.RequestDtos.ReasonBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.RequestDtos.RequestResponse;
import com.iortatechnxt.brokerverse.productmaint.service.ComparativeService;
import com.iortatechnxt.brokerverse.productmaint.service.ComparativeTable.Selection;
import com.iortatechnxt.brokerverse.productmaint.service.PackageSetupHandoff;
import com.iortatechnxt.brokerverse.productmaint.service.PackageSetupHandoff.SetupInput;
import com.iortatechnxt.brokerverse.productmaint.service.RequirementsService;
import com.iortatechnxt.brokerverse.productmaint.service.TermsCodec;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Outputs and hand-offs of a package request (BRPM.014/015, PMADD03): the comparative master and
 * client views with their PDF / Excel files, the requirements pack, the package slip, the ManCom
 * sign-off, and the MBS set-up, return and retirement.
 */
@RestController
@RequestMapping("/api/v1/product-maintenance/requests/{id}")
public class PackageOutputsController {

  private static final String NEGOTIATE = "hasAuthority('PKG_NEGOTIATE')";
  private static final String MBS = "hasAuthority('PRODUCT_MAINTAIN')";

  private final ComparativeService comparatives;
  private final RequirementsService requirements;
  private final PackageSetupHandoff setup;
  private final TermsCodec codec;

  /**
   * Creates the controller.
   *
   * @param comparatives comparative outputs
   * @param requirements requirements and sign-off
   * @param setup MBS set-up
   * @param codec terms JSON
   */
  public PackageOutputsController(
      ComparativeService comparatives,
      RequirementsService requirements,
      PackageSetupHandoff setup,
      TermsCodec codec) {
    this.comparatives = comparatives;
    this.requirements = requirements;
    this.setup = setup;
    this.codec = codec;
  }

  /**
   * Stored comparative outputs, newest first.
   *
   * @param id request
   * @return outputs
   */
  @GetMapping("/comparatives")
  @PreAuthorize(PackageRequestController.VIEW)
  public List<OutputView> comparatives(@PathVariable Long id) {
    return comparatives.outputs(id).stream().map(OutputView::from).toList();
  }

  /**
   * Compiles a new audit master from the latest round.
   *
   * @param id request
   * @return the master
   */
  @PostMapping("/comparatives/master")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(NEGOTIATE)
  public OutputView compileMaster(@PathVariable Long id) {
    return OutputView.from(comparatives.compileMaster(id));
  }

  /**
   * Generates a client view of the current master.
   *
   * @param id request
   * @param body title, fields and insurers
   * @return the client output
   */
  @PostMapping("/comparatives")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(NEGOTIATE)
  public OutputView clientView(@PathVariable Long id, @Valid @RequestBody ClientViewBody body) {
    return OutputView.from(
        comparatives.clientView(id, body.title(), new Selection(body.fields(), body.insurers())));
  }

  /**
   * An output as PDF.
   *
   * @param id request
   * @param outputId output
   * @return PDF
   */
  @GetMapping("/comparatives/{outputId}.pdf")
  @PreAuthorize(PackageRequestController.VIEW)
  public ResponseEntity<byte[]> outputPdf(@PathVariable Long id, @PathVariable Long outputId) {
    return PackageRequestController.file(comparatives.file(id, outputId, false));
  }

  /**
   * An output as Excel.
   *
   * @param id request
   * @param outputId output
   * @return XLSX
   */
  @GetMapping("/comparatives/{outputId}.xlsx")
  @PreAuthorize(PackageRequestController.VIEW)
  public ResponseEntity<byte[]> outputXlsx(@PathVariable Long id, @PathVariable Long outputId) {
    return PackageRequestController.file(comparatives.file(id, outputId, true));
  }

  /**
   * What the requirements pack lacks, and the ManCom decisions.
   *
   * @param id request
   * @return requirements status
   */
  @GetMapping("/requirements")
  @PreAuthorize(PackageRequestController.VIEW)
  public RequirementsView requirements(@PathVariable Long id) {
    return new RequirementsView(
        requirements.missing(id),
        requirements.signoffs(id).stream().map(SignoffView::from).toList());
  }

  /**
   * Completes the proposed rate scheme and dates.
   *
   * @param id request
   * @param body scheme and dates
   * @return the request
   */
  @PutMapping("/requirements")
  @PreAuthorize(NEGOTIATE)
  public RequestResponse updateRequirements(
      @PathVariable Long id, @Valid @RequestBody RequirementsBody body) {
    return PackageRequestController.view(
        requirements.updateRequirements(id, body.scheme(), body.dates()), codec);
  }

  /**
   * Submits the requirements pack to ManCom.
   *
   * @param id request
   * @param body comment
   * @return the request
   */
  @PostMapping("/submit-requirements")
  @PreAuthorize(NEGOTIATE)
  public RequestResponse submitRequirements(
      @PathVariable Long id, @Valid @RequestBody CommentBody body) {
    return PackageRequestController.view(requirements.submitRequirements(id, body.text()), codec);
  }

  /**
   * The package slip to print for signature.
   *
   * @param id request
   * @return PDF
   */
  @GetMapping("/package-slip.pdf")
  @PreAuthorize(PackageRequestController.VIEW)
  public ResponseEntity<byte[]> packageSlip(@PathVariable Long id) {
    return PackageRequestController.file(requirements.packageSlip(id));
  }

  /**
   * ManCom sign-off.
   *
   * @param id request
   * @param body comment
   * @return the sign-off
   */
  @PostMapping("/signoff")
  @PreAuthorize("hasAuthority('PKG_MANCOM_SIGNOFF')")
  public SignoffView signoff(@PathVariable Long id, @Valid @RequestBody CommentBody body) {
    return SignoffView.from(requirements.signoff(id, body.text()));
  }

  /**
   * MBS set-up of the package version.
   *
   * @param id request
   * @param body product of a new package, change summary, comment
   * @return the request
   */
  @PostMapping("/setup")
  @PreAuthorize(MBS)
  public RequestResponse setUp(@PathVariable Long id, @Valid @RequestBody SetupBody body) {
    return PackageRequestController.view(
        setup.setUp(
            id,
            new SetupInput(
                body.productCode(), body.productName(), body.changeSummary(), body.comment())),
        codec);
  }

  /**
   * MBS returns incomplete requirements to TSU.
   *
   * @param id request
   * @param body reason and comment
   * @return the request
   */
  @PostMapping("/return-incomplete")
  @PreAuthorize(MBS)
  public RequestResponse returnIncomplete(
      @PathVariable Long id, @Valid @RequestBody ReasonBody body) {
    return PackageRequestController.view(
        setup.returnIncomplete(id, body.reasonCode(), body.comment()), codec);
  }

  /**
   * MBS retires the package of a RETIRE request.
   *
   * @param id request
   * @param body comment
   * @return the request
   */
  @PostMapping("/retire")
  @PreAuthorize(MBS)
  public RequestResponse retire(@PathVariable Long id, @Valid @RequestBody CommentBody body) {
    return PackageRequestController.view(setup.retire(id, body.text()), codec);
  }
}

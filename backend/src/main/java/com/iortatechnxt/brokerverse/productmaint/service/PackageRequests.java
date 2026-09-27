package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequestRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import org.springframework.stereotype.Component;

/**
 * Loads package requests for the process services and checks the stage an action needs; the
 * services of the module share it so that none of them depends on another for a simple read.
 */
@Component
public class PackageRequests {

  /** Entity type of package requests in the workflow, attachments, messages and audit trail. */
  public static final String ENTITY = "PackageRequest";

  /** Workflow of package requests (V755). */
  public static final String WORKFLOW = "PM_PACKAGE_REQUEST";

  private final PackageRequestRepository requests;

  /**
   * Creates the reader.
   *
   * @param requests package requests
   */
  public PackageRequests(PackageRequestRepository requests) {
    this.requests = requests;
  }

  /**
   * One request.
   *
   * @param id id
   * @return request
   */
  public PackageRequest get(Long id) {
    return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * One request that must be in a stage.
   *
   * @param id id
   * @param stage required stage
   * @param code error code when it is not
   * @param message error message when it is not
   * @return request
   */
  public PackageRequest inStage(Long id, RequestStage stage, String code, String message) {
    PackageRequest p = get(id);
    if (p.getStatus() != stage) {
      throw new BusinessRuleException(code, message + " (request is " + p.getStatus() + ")");
    }
    return p;
  }

  /**
   * Saves a new request.
   *
   * @param request request
   * @return saved request
   */
  public PackageRequest save(PackageRequest request) {
    return requests.save(request);
  }

  /**
   * The record link of a request (work case and e-mails).
   *
   * @param p request
   * @return frontend route
   */
  public static String link(PackageRequest p) {
    return "/product-maintenance/requests/" + p.getId();
  }

  /**
   * Work case title of a request.
   *
   * @param p request
   * @return title
   */
  public static String title(PackageRequest p) {
    String product = p.getTargetProductCode() == null ? "new package" : p.getTargetProductCode();
    return p.getRequestType() + " " + product + " - " + p.getTitle();
  }
}

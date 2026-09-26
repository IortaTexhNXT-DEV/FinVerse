package com.iortatechnxt.brokerverse.brokerclaims.setup.api;

import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.setup.api.dto.SetupDtos.ClaimsListResponse;
import com.iortatechnxt.brokerverse.brokerclaims.setup.api.dto.SetupDtos.HandlerRequest;
import com.iortatechnxt.brokerverse.brokerclaims.setup.api.dto.SetupDtos.HandlerResponse;
import com.iortatechnxt.brokerverse.brokerclaims.setup.api.dto.SetupDtos.MatrixRowRequest;
import com.iortatechnxt.brokerverse.brokerclaims.setup.api.dto.SetupDtos.MatrixRowResponse;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.AttributeSetupService;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.AttributeSetupService.SettlementAttributes;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.AttributeSetupService.StatusAttributes;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.AttributeSetupService.ValueAttributes;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.HandlerSetupService;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.MatrixSetupService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
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
 * Claims Setup (BRCLM.010/012/014/017/036; FR-CL-040/041/043): status and settlement type
 * attributes, the status access matrix and the claims handler register, maintained by the Unit Head
 * (BCL_SETUP) under maker-checker; and the Claims lists maintained through the list-of-values API
 * with the owner permission BCL_SETUP.
 */
@RestController
@RequestMapping("/api/v1/broker-claims/setup")
@PreAuthorize(ClaimsSetupController.SETUP)
public class ClaimsSetupController {

  /** Claims Setup permission. */
  static final String SETUP = "hasAuthority('BCL_SETUP')";

  /** Checkers of the setup changes: another Unit Head or a master data authorizer. */
  static final String CHECKER = "hasAnyAuthority('BCL_SETUP', 'MASTER_AUTHORIZE')";

  private final AttributeSetupService attributes;
  private final MatrixSetupService matrix;
  private final HandlerSetupService handlers;
  private final LovService lovs;

  /**
   * Creates the controller.
   *
   * @param attributes status and settlement attributes
   * @param matrix status access matrix
   * @param handlers handler register and lists
   * @param lovs lists of values (labels)
   */
  public ClaimsSetupController(
      AttributeSetupService attributes,
      MatrixSetupService matrix,
      HandlerSetupService handlers,
      LovService lovs) {
    this.attributes = attributes;
    this.matrix = matrix;
    this.handlers = handlers;
    this.lovs = lovs;
  }

  /**
   * The values of a list with their attributes.
   *
   * @param typeCode {@code BCL_CLAIM_STATUS} or {@code BCL_SETTLEMENT_TYPE}
   * @return values
   */
  @GetMapping("/attributes/{typeCode}")
  public List<ValueAttributes> attributes(@PathVariable String typeCode) {
    return attributes.list(typeCode);
  }

  /**
   * Proposes the attributes of a status.
   *
   * @param code status
   * @param request attributes
   * @return the value
   */
  @PutMapping("/statuses/{code}/attributes")
  public ValueAttributes proposeStatus(
      @PathVariable String code, @RequestBody StatusAttributes request) {
    return attributes.proposeStatus(code, request);
  }

  /**
   * Proposes the attributes of a settlement type.
   *
   * @param code settlement type
   * @param request attributes
   * @return the value
   */
  @PutMapping("/settlement-types/{code}/attributes")
  public ValueAttributes proposeSettlement(
      @PathVariable String code, @RequestBody SettlementAttributes request) {
    return attributes.proposeSettlement(code, request);
  }

  /**
   * Authorizes the pending attributes of a value.
   *
   * @param typeCode list
   * @param code value
   * @return the value
   */
  @PostMapping("/attributes/{typeCode}/{code}/authorize")
  @PreAuthorize(CHECKER)
  public ValueAttributes authorizeAttributes(
      @PathVariable String typeCode, @PathVariable String code) {
    return attributes.authorize(typeCode, code);
  }

  /**
   * Withdraws or rejects the pending attributes of a value.
   *
   * @param typeCode list
   * @param code value
   * @return the value
   */
  @PostMapping("/attributes/{typeCode}/{code}/reject")
  @PreAuthorize(CHECKER)
  public ValueAttributes rejectAttributes(
      @PathVariable String typeCode, @PathVariable String code) {
    return attributes.reject(typeCode, code);
  }

  /**
   * The status access matrix.
   *
   * @return rows
   */
  @GetMapping("/matrix")
  public List<MatrixRowResponse> matrix() {
    return matrix.rows().stream().map(r -> MatrixRowResponse.from(r, this::statusLabel)).toList();
  }

  /**
   * The roles of the matrix.
   *
   * @return code and name of each claims role
   */
  @GetMapping("/matrix/roles")
  public List<Map<String, Object>> roles() {
    return matrix.roles();
  }

  /**
   * Adds a matrix row, waiting for authorization.
   *
   * @param request status, role and unit
   * @return the row
   */
  @PostMapping("/matrix")
  @ResponseStatus(HttpStatus.CREATED)
  public MatrixRowResponse addRow(@RequestBody MatrixRowRequest request) {
    return MatrixRowResponse.from(
        matrix.add(request.statusCode(), request.roleCode(), request.unitCode()),
        this::statusLabel);
  }

  /**
   * Authorizes a matrix row.
   *
   * @param id row
   * @return the row
   */
  @PostMapping("/matrix/{id}/authorize")
  @PreAuthorize(CHECKER)
  public MatrixRowResponse authorizeRow(@PathVariable Long id) {
    return MatrixRowResponse.from(matrix.authorize(id), this::statusLabel);
  }

  /**
   * Deactivates a matrix row.
   *
   * @param id row
   * @return the row
   */
  @PostMapping("/matrix/{id}/deactivate")
  public MatrixRowResponse deactivateRow(@PathVariable Long id) {
    return MatrixRowResponse.from(matrix.deactivate(id), this::statusLabel);
  }

  /**
   * The claims handler register.
   *
   * @return handlers
   */
  @GetMapping("/handlers")
  public List<HandlerResponse> handlers() {
    return handlers.handlers().stream().map(HandlerResponse::from).toList();
  }

  /**
   * Registers or changes a handler.
   *
   * @param request user, unit, team and active flag
   * @return the handler
   */
  @PutMapping("/handlers")
  public HandlerResponse saveHandler(@RequestBody HandlerRequest request) {
    return HandlerResponse.from(
        handlers.save(request.username(), request.unitCode(), request.team(), request.active()));
  }

  /**
   * The claims users who may be registered.
   *
   * @return usernames
   */
  @GetMapping("/users")
  public List<String> users() {
    return handlers.claimsUsers();
  }

  /**
   * The Claims lists maintained with BCL_SETUP.
   *
   * @return list types
   */
  @GetMapping("/lists")
  public List<ClaimsListResponse> lists() {
    return handlers.claimsLists().stream().map(ClaimsListResponse::from).toList();
  }

  private String statusLabel(String code) {
    return lovs.label(ClaimCodes.LOV_STATUS, code);
  }
}

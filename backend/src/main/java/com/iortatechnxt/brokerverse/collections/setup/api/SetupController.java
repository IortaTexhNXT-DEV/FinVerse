package com.iortatechnxt.brokerverse.collections.setup.api;

import com.iortatechnxt.brokerverse.collections.common.domain.LovAttribute;
import com.iortatechnxt.brokerverse.collections.common.service.LovAttributes;
import com.iortatechnxt.brokerverse.collections.setup.api.dto.SetupDtos.AttributeRequest;
import com.iortatechnxt.brokerverse.collections.setup.api.dto.SetupDtos.DispositionRow;
import com.iortatechnxt.brokerverse.collections.setup.api.dto.SetupDtos.ParameterRequest;
import com.iortatechnxt.brokerverse.collections.setup.api.dto.SetupDtos.ParameterRow;
import com.iortatechnxt.brokerverse.collections.setup.api.dto.SetupDtos.SetupResponse;
import com.iortatechnxt.brokerverse.collections.setup.api.dto.SetupDtos.UnitHeadRequest;
import com.iortatechnxt.brokerverse.collections.setup.api.dto.SetupDtos.UnitRow;
import com.iortatechnxt.brokerverse.collections.setup.service.CollectionsSetupService;
import com.iortatechnxt.brokerverse.collections.worklist.api.ClxAccess;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Collections Setup (BRCLXN.005-007, 011, 016/017, 037; COLLECTIONS_DESIGN 11) under {@code
 * CLX_SETUP}: parameters, disposition attributes and Unit Heads. The LOV values themselves are
 * maintained on the LOV maintenance screen (maker-checker).
 */
@RestController
@RequestMapping("/api/v1/collections/setup")
public class SetupController {

  private final CollectionsSetupService setup;
  private final LovService lovs;

  /**
   * Creates the controller.
   *
   * @param setup Collections Setup
   * @param lovs lists of values
   */
  public SetupController(CollectionsSetupService setup, LovService lovs) {
    this.setup = setup;
    this.lovs = lovs;
  }

  /**
   * Parameters, disposition values with their attributes and Unit Heads.
   *
   * @param companyId company
   * @return setup
   */
  @GetMapping
  @PreAuthorize(ClxAccess.SETUP)
  public SetupResponse setup(@RequestParam Long companyId) {
    List<LovAttribute> attributes = setup.lovAttributes();
    List<DispositionRow> rows = new ArrayList<>();
    for (String type : List.of(LovAttributes.PR_DISPOSITION, LovAttributes.UPP_DISPOSITION)) {
      lovs.values(type).forEach(v -> rows.add(DispositionRow.from(v, attributes)));
    }
    return new SetupResponse(
        setup.parameters().stream().map(ParameterRow::from).toList(),
        rows,
        setup.units(companyId).stream().map(UnitRow::from).toList());
  }

  /**
   * Changes a Collections parameter (BRCLXN.005-007).
   *
   * @param companyId company
   * @param key parameter
   * @param request value
   * @return parameter
   */
  @PutMapping("/parameters/{key}")
  @PreAuthorize(ClxAccess.SETUP)
  public ParameterRow parameter(
      @RequestParam Long companyId,
      @PathVariable String key,
      @Valid @RequestBody ParameterRequest request) {
    return ParameterRow.from(setup.updateParameter(companyId, key, request.value()));
  }

  /**
   * Sets or removes an attribute of a disposition value (BRCLXN.016/017, 037).
   *
   * @param companyId company
   * @param request attribute
   */
  @PutMapping("/lov-attributes")
  @PreAuthorize(ClxAccess.SETUP)
  public void attribute(
      @RequestParam Long companyId, @Valid @RequestBody AttributeRequest request) {
    setup.setLovAttribute(
        companyId, request.typeCode(), request.code(), request.attribute(), request.value());
  }

  /**
   * Sets or clears the Unit Head of a sales unit (BRCLXN.011/012).
   *
   * @param companyId company
   * @param unitCode sales unit
   * @param request head
   * @return unit
   */
  @PutMapping("/unit-heads/{unitCode}")
  @PreAuthorize(ClxAccess.SETUP)
  public UnitRow unitHead(
      @RequestParam Long companyId,
      @PathVariable String unitCode,
      @Valid @RequestBody UnitHeadRequest request) {
    return UnitRow.from(setup.assignUnitHead(companyId, unitCode, request.username()));
  }
}

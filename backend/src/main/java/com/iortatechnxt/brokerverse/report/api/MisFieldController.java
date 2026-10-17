package com.iortatechnxt.brokerverse.report.api;

import com.iortatechnxt.brokerverse.common.security.GlobalRecord;
import com.iortatechnxt.brokerverse.report.api.dto.MisFieldDtos.MisFieldRequest;
import com.iortatechnxt.brokerverse.report.api.dto.MisFieldDtos.MisFieldView;
import com.iortatechnxt.brokerverse.report.core.MisFieldService;
import jakarta.validation.Valid;
import java.util.List;
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
 * MIS Fields (Broking Setup, FR-CR-062): the catalogue of the business labels of the data elements
 * of the core entities, read by every report user for the Layout panel and maintained by the System
 * Administrator under maker-checker.
 */
@RestController
@RequestMapping("/api/v1/mis-fields")
@GlobalRecord("the MIS fields catalogue is programme-wide")
public class MisFieldController {

  private static final String MAINTAIN = "hasAuthority('MIS_FIELD_MAINTAIN')";
  private static final String AUTHORIZE = "hasAuthority('MIS_FIELD_AUTHORIZE')";

  private final MisFieldService fields;

  /**
   * Creates the controller.
   *
   * @param fields catalogue
   */
  public MisFieldController(MisFieldService fields) {
    this.fields = fields;
  }

  /**
   * The catalogue with the data elements each field may read.
   *
   * @return fields, by entity and code
   */
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public List<MisFieldView> list() {
    return fields.list().stream().map(MisFieldView::from).toList();
  }

  /**
   * Adds a field, pending authorization.
   *
   * @param request field
   * @return the field
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public MisFieldView create(@Valid @RequestBody MisFieldRequest request) {
    return MisFieldView.from(
        fields.create(
            request.code(),
            request.label(),
            request.entity(),
            request.source(),
            request.lovType()));
  }

  /**
   * Changes the label and list of values of a field, pending authorization.
   *
   * @param id field
   * @param request field (code, entity and source are kept)
   * @return the field
   */
  @PutMapping("/{id}")
  @PreAuthorize(MAINTAIN)
  public MisFieldView change(@PathVariable Long id, @Valid @RequestBody MisFieldRequest request) {
    return MisFieldView.from(fields.change(id, request.label(), request.lovType()));
  }

  /**
   * Authorizes a pending field (not by its maker).
   *
   * @param id field
   * @return the field
   */
  @PostMapping("/{id}/authorize")
  @PreAuthorize(AUTHORIZE)
  public MisFieldView authorize(@PathVariable Long id) {
    return MisFieldView.from(fields.authorize(id));
  }

  /**
   * Deactivates a field.
   *
   * @param id field
   * @return the field
   */
  @PostMapping("/{id}/deactivate")
  @PreAuthorize(MAINTAIN)
  public MisFieldView deactivate(@PathVariable Long id) {
    return MisFieldView.from(fields.deactivate(id));
  }

  /**
   * Reactivates an inactive field, pending authorization.
   *
   * @param id field
   * @return the field
   */
  @PostMapping("/{id}/reactivate")
  @PreAuthorize(MAINTAIN)
  public MisFieldView reactivate(@PathVariable Long id) {
    return MisFieldView.from(fields.reactivate(id));
  }
}

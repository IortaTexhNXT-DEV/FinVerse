package com.iortatechnxt.brokerverse.migration.mapping.api;

import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.ColumnResponse;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.LayoutResponse;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.MaskingRequest;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.MaskingResponse;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.RuleRequest;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.RuleResponse;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.ToggleRequest;
import com.iortatechnxt.brokerverse.migration.mapping.service.LayoutService;
import com.iortatechnxt.brokerverse.migration.mapping.service.RuleCatalogueService;
import com.iortatechnxt.brokerverse.migration.mapping.service.TemplateExport;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Layouts, load templates, data-quality rules and masking rules (screen Layouts and Rules): the
 * templates BDOI fills are generated from the layout versions in force.
 */
@RestController
@RequestMapping("/api/v1/migration")
public class LayoutController {

  private static final String VIEW = "hasAuthority('MIG_VIEW')";
  private static final String EDIT = "hasAuthority('MIG_MAPPING_EDIT')";
  private static final String CSV = "text/csv";
  private static final String XLSX_FORMAT = "xlsx";
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final LayoutService layouts;
  private final TemplateExport templates;
  private final RuleCatalogueService rules;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param layouts layouts
   * @param templates load templates
   * @param rules rule catalogue
   * @param downloads file answers
   */
  public LayoutController(
      LayoutService layouts,
      TemplateExport templates,
      RuleCatalogueService rules,
      FileDownloads downloads) {
    this.layouts = layouts;
    this.templates = templates;
    this.rules = rules;
    this.downloads = downloads;
  }

  /**
   * Every layout version.
   *
   * @return layouts
   */
  @GetMapping("/layouts")
  @PreAuthorize(VIEW)
  public List<LayoutResponse> layouts() {
    return layouts.all().stream().map(LayoutResponse::from).toList();
  }

  /**
   * The columns of a layout version.
   *
   * @param id layout version
   * @return columns
   */
  @GetMapping("/layouts/{id}/columns")
  @PreAuthorize(VIEW)
  public List<ColumnResponse> columns(@PathVariable Long id) {
    return layouts.columns(id).stream().map(ColumnResponse::from).toList();
  }

  /**
   * Freezes a draft layout version.
   *
   * @param id layout version
   * @return layout
   */
  @PostMapping("/layouts/{id}/freeze")
  @PreAuthorize(EDIT)
  public LayoutResponse freeze(@PathVariable Long id) {
    return LayoutResponse.from(layouts.freeze(id));
  }

  /**
   * The load template of a layout: the guided Excel template ({@code format=xlsx}, the one the
   * console offers) or the CSV layout (header row only, for large extracts; the default of the
   * API).
   *
   * @param code layout
   * @param format csv or xlsx
   * @param request HTTP request
   * @return CSV or XLSX
   */
  @GetMapping("/templates/layouts/{code}")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> layoutTemplate(
      @PathVariable String code,
      @RequestParam(defaultValue = "csv") String format,
      HttpServletRequest request) {
    if (XLSX_FORMAT.equals(format)) {
      return downloads.respond(
          FileDownload.inline(code + "_template.xlsx", XLSX, templates.layoutWorkbook(code)),
          request);
    }
    return downloads.respond(
        FileDownload.inline(code + "_template.csv", CSV, templates.layoutCsv(code)), request);
  }

  /**
   * The template of the control file: the guided Excel template ({@code format=xlsx}) or the CSV
   * layout (the default of the API).
   *
   * @param format csv or xlsx
   * @param request HTTP request
   * @return CSV or XLSX
   */
  @GetMapping("/templates/control")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> controlTemplate(
      @RequestParam(defaultValue = "csv") String format, HttpServletRequest request) {
    if (XLSX_FORMAT.equals(format)) {
      return downloads.respond(
          FileDownload.inline("CONTROL_template.ctl.xlsx", XLSX, templates.controlWorkbook()),
          request);
    }
    return downloads.respond(
        FileDownload.inline("CONTROL_template.ctl.csv", CSV, templates.controlCsv()), request);
  }

  /**
   * The workbook of every load template with the control file and the filling instructions.
   *
   * @param request HTTP request
   * @return XLSX
   */
  @GetMapping("/templates/workbook")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> workbook(HttpServletRequest request) {
    return downloads.respond(
        FileDownload.inline("Migration_load_templates.xlsx", XLSX, templates.fullWorkbook()),
        request);
  }

  /**
   * The rule catalogue.
   *
   * @return rules
   */
  @GetMapping("/rules")
  @PreAuthorize(VIEW)
  public List<RuleResponse> rules() {
    return rules.rules().stream().map(RuleResponse::from).toList();
  }

  /**
   * Sets the severity and activation of a rule.
   *
   * @param code rule
   * @param request setting
   * @return rule
   */
  @PutMapping("/rules/{code}")
  @PreAuthorize(EDIT)
  public RuleResponse configure(
      @PathVariable String code, @Valid @RequestBody RuleRequest request) {
    return RuleResponse.from(rules.configure(code, request.severity(), request.active()));
  }

  /**
   * The masking rules.
   *
   * @return rules
   */
  @GetMapping("/masking-rules")
  @PreAuthorize(VIEW)
  public List<MaskingResponse> masking() {
    return rules.masking().stream().map(MaskingResponse::from).toList();
  }

  /**
   * Adds a masking rule.
   *
   * @param request rule
   * @return rule
   */
  @PostMapping("/masking-rules")
  @PreAuthorize(EDIT)
  public MaskingResponse addMasking(@Valid @RequestBody MaskingRequest request) {
    return MaskingResponse.from(
        rules.addMasking(request.layoutCode(), request.columnName(), request.rule()));
  }

  /**
   * Switches a masking rule on or off.
   *
   * @param id rule
   * @param request activation
   * @return rule
   */
  @PutMapping("/masking-rules/{id}")
  @PreAuthorize(EDIT)
  public MaskingResponse toggle(@PathVariable Long id, @RequestBody ToggleRequest request) {
    return MaskingResponse.from(rules.toggleMasking(id, request.active()));
  }
}

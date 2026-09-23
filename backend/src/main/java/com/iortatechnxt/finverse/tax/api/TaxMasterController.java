package com.iortatechnxt.finverse.tax.api;

import com.iortatechnxt.finverse.tax.api.dto.PartyTaxProfileRequest;
import com.iortatechnxt.finverse.tax.api.dto.PartyTaxProfileResponse;
import com.iortatechnxt.finverse.tax.api.dto.TaxCodeRequest;
import com.iortatechnxt.finverse.tax.api.dto.TaxCodeResponse;
import com.iortatechnxt.finverse.tax.api.dto.TaxFormRequest;
import com.iortatechnxt.finverse.tax.api.dto.TaxFormResponse;
import com.iortatechnxt.finverse.tax.service.PartyTaxProfileService;
import com.iortatechnxt.finverse.tax.service.TaxCodeService;
import com.iortatechnxt.finverse.tax.service.TaxFormService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tax masters: tax codes / ATCs, filing calendar forms and party tax profiles (maker-checker). */
@RestController
@RequestMapping("/api/v1/tax")
public class TaxMasterController {

  private final TaxCodeService codes;
  private final TaxFormService forms;
  private final PartyTaxProfileService profiles;

  /**
   * Creates the controller.
   *
   * @param codes tax codes
   * @param forms tax forms
   * @param profiles party tax profiles
   */
  public TaxMasterController(
      TaxCodeService codes, TaxFormService forms, PartyTaxProfileService profiles) {
    this.codes = codes;
    this.forms = forms;
    this.profiles = profiles;
  }

  /**
   * Lists tax codes.
   *
   * @param companyId company
   * @return codes
   */
  @GetMapping("/codes")
  @PreAuthorize(TaxAccess.VIEW)
  public List<TaxCodeResponse> codes(@RequestParam Long companyId) {
    return codes.list(companyId).stream().map(TaxCodeResponse::from).toList();
  }

  /**
   * Creates a tax code.
   *
   * @param request values
   * @return code
   */
  @PostMapping("/codes")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(TaxAccess.MANAGE)
  public TaxCodeResponse createCode(@Valid @RequestBody TaxCodeRequest request) {
    return TaxCodeResponse.from(codes.create(request.toCommand()));
  }

  /**
   * Updates a tax code.
   *
   * @param id id
   * @param request values
   * @return code
   */
  @PutMapping("/codes/{id}")
  @PreAuthorize(TaxAccess.MANAGE)
  public TaxCodeResponse updateCode(
      @PathVariable Long id, @Valid @RequestBody TaxCodeRequest request) {
    return TaxCodeResponse.from(codes.update(id, request.toCommand()));
  }

  /**
   * Authorizes a tax code.
   *
   * @param id id
   * @return code
   */
  @PostMapping("/codes/{id}/authorize")
  @PreAuthorize(TaxAccess.AUTHORIZE)
  public TaxCodeResponse authorizeCode(@PathVariable Long id) {
    return TaxCodeResponse.from(codes.authorize(id));
  }

  /**
   * Lists tax forms.
   *
   * @param companyId company
   * @return forms
   */
  @GetMapping("/forms")
  @PreAuthorize(TaxAccess.VIEW)
  public List<TaxFormResponse> forms(@RequestParam Long companyId) {
    return forms.list(companyId).stream().map(TaxFormResponse::from).toList();
  }

  /**
   * Creates a tax form.
   *
   * @param request values
   * @return form
   */
  @PostMapping("/forms")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(TaxAccess.MANAGE)
  public TaxFormResponse createForm(@Valid @RequestBody TaxFormRequest request) {
    return TaxFormResponse.from(forms.create(request.toCommand()));
  }

  /**
   * Updates a tax form.
   *
   * @param id id
   * @param request values
   * @return form
   */
  @PutMapping("/forms/{id}")
  @PreAuthorize(TaxAccess.MANAGE)
  public TaxFormResponse updateForm(
      @PathVariable Long id, @Valid @RequestBody TaxFormRequest request) {
    return TaxFormResponse.from(forms.update(id, request.toCommand()));
  }

  /**
   * Authorizes a tax form.
   *
   * @param id id
   * @return form
   */
  @PostMapping("/forms/{id}/authorize")
  @PreAuthorize(TaxAccess.AUTHORIZE)
  public TaxFormResponse authorizeForm(@PathVariable Long id) {
    return TaxFormResponse.from(forms.authorize(id));
  }

  /**
   * Lists party tax profiles.
   *
   * @param companyId company
   * @return profiles
   */
  @GetMapping("/profiles")
  @PreAuthorize(TaxAccess.VIEW)
  public List<PartyTaxProfileResponse> profiles(@RequestParam Long companyId) {
    return profiles.list(companyId).stream().map(PartyTaxProfileResponse::from).toList();
  }

  /**
   * Creates a party tax profile.
   *
   * @param request values
   * @return profile
   */
  @PostMapping("/profiles")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(TaxAccess.MANAGE)
  public PartyTaxProfileResponse createProfile(@Valid @RequestBody PartyTaxProfileRequest request) {
    return PartyTaxProfileResponse.from(profiles.create(request.toCommand()));
  }

  /**
   * Updates a party tax profile.
   *
   * @param id id
   * @param request values
   * @return profile
   */
  @PutMapping("/profiles/{id}")
  @PreAuthorize(TaxAccess.MANAGE)
  public PartyTaxProfileResponse updateProfile(
      @PathVariable Long id, @Valid @RequestBody PartyTaxProfileRequest request) {
    return PartyTaxProfileResponse.from(profiles.update(id, request.toCommand()));
  }

  /**
   * Authorizes a party tax profile.
   *
   * @param id id
   * @return profile
   */
  @PostMapping("/profiles/{id}/authorize")
  @PreAuthorize(TaxAccess.AUTHORIZE)
  public PartyTaxProfileResponse authorizeProfile(@PathVariable Long id) {
    return PartyTaxProfileResponse.from(profiles.authorize(id));
  }
}

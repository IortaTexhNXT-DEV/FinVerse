package com.iortatechnxt.finverse.fixedasset.api;

import com.iortatechnxt.finverse.fixedasset.api.dto.AssetMovementResponse;
import com.iortatechnxt.finverse.fixedasset.api.dto.DisposalRequest;
import com.iortatechnxt.finverse.fixedasset.api.dto.FixedAssetRequest;
import com.iortatechnxt.finverse.fixedasset.api.dto.FixedAssetResponse;
import com.iortatechnxt.finverse.fixedasset.api.dto.TransferRequest;
import com.iortatechnxt.finverse.fixedasset.domain.AssetStatus;
import com.iortatechnxt.finverse.fixedasset.service.AssetLifecycleService;
import com.iortatechnxt.finverse.fixedasset.service.DisposalPreview;
import com.iortatechnxt.finverse.fixedasset.service.FixedAssetService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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

/** REST API for the fixed asset register. */
@RestController
@RequestMapping("/api/v1/assets/register")
public class FixedAssetController {

  private final FixedAssetService service;
  private final AssetLifecycleService lifecycle;

  /**
   * Creates the controller.
   *
   * @param service asset register service
   * @param lifecycle capitalization, disposal and transfer service
   */
  public FixedAssetController(FixedAssetService service, AssetLifecycleService lifecycle) {
    this.service = service;
    this.lifecycle = lifecycle;
  }

  /**
   * Searches the register.
   *
   * @param companyId company
   * @param status status filter
   * @param branchId branch filter
   * @param categoryId category filter
   * @param q tag number / description fragment
   * @return assets
   */
  @GetMapping
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<FixedAssetResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) AssetStatus status,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) String q) {
    return service.search(companyId, status, branchId, categoryId, q).stream()
        .map(FixedAssetResponse::from)
        .toList();
  }

  /**
   * Gets an asset.
   *
   * @param id id
   * @return asset
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public FixedAssetResponse get(@PathVariable Long id) {
    return FixedAssetResponse.from(service.get(id));
  }

  /**
   * Movement history of an asset.
   *
   * @param id id
   * @return movements
   */
  @GetMapping("/{id}/movements")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<AssetMovementResponse> movements(@PathVariable Long id) {
    return service.movements(id).stream().map(AssetMovementResponse::from).toList();
  }

  /**
   * Registers an asset (pending capitalization).
   *
   * @param request request
   * @return asset
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public FixedAssetResponse create(@Valid @RequestBody FixedAssetRequest request) {
    return FixedAssetResponse.from(service.create(request));
  }

  /**
   * Updates an asset awaiting capitalization.
   *
   * @param id id
   * @param request request
   * @return asset
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('MASTER_MAINTAIN')")
  public FixedAssetResponse update(
      @PathVariable Long id, @Valid @RequestBody FixedAssetRequest request) {
    return FixedAssetResponse.from(service.update(id, request));
  }

  /**
   * Capitalizes an asset (checker).
   *
   * @param id id
   * @return asset
   */
  @PostMapping("/{id}/capitalize")
  @PreAuthorize("hasAuthority('MASTER_AUTHORIZE')")
  public FixedAssetResponse capitalize(@PathVariable Long id) {
    return FixedAssetResponse.from(lifecycle.capitalize(id));
  }

  /**
   * Disposes of an asset.
   *
   * @param id id
   * @param request disposal
   * @return movement
   */
  @PostMapping("/{id}/dispose")
  @PreAuthorize("hasAuthority('ASSET_MANAGE')")
  public AssetMovementResponse dispose(
      @PathVariable Long id, @Valid @RequestBody DisposalRequest request) {
    return AssetMovementResponse.from(lifecycle.dispose(id, request));
  }

  /**
   * Previews a disposal: the depreciation it would reverse, the net book value it is measured
   * against and the gain or loss, without posting anything.
   *
   * @param id id
   * @param disposalDate disposal date
   * @param proceeds sale proceeds
   * @return preview
   */
  @GetMapping("/{id}/disposal-preview")
  @PreAuthorize("hasAuthority('ASSET_MANAGE')")
  public DisposalPreview disposalPreview(
      @PathVariable Long id,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate disposalDate,
      @RequestParam BigDecimal proceeds) {
    return lifecycle.previewDisposal(id, disposalDate, proceeds);
  }

  /**
   * Transfers an asset to another branch.
   *
   * @param id id
   * @param request transfer
   * @return movement
   */
  @PostMapping("/{id}/transfer")
  @PreAuthorize("hasAuthority('ASSET_MANAGE')")
  public AssetMovementResponse transfer(
      @PathVariable Long id, @Valid @RequestBody TransferRequest request) {
    return AssetMovementResponse.from(lifecycle.transfer(id, request));
  }
}

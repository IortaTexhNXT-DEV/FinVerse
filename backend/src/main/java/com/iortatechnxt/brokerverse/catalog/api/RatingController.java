package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.RatingRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.RatingResponse;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import com.iortatechnxt.brokerverse.catalog.service.RatingService.Rating;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueries;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Premium calculator (Appendix A) for the calculator screen and the account wizard. */
@RestController
@RequestMapping("/api/v1/catalog/rating")
public class RatingController {

  private final RatingService rating;
  private final ProductVersionQueries versions;

  /**
   * Creates the controller.
   *
   * @param rating rating service
   * @param versions package versions (the version shown with the result)
   */
  public RatingController(RatingService rating, ProductVersionQueries versions) {
    this.rating = rating;
    this.versions = versions;
  }

  /**
   * Computes the premium breakdown; nothing is stored. A package is priced on the version in force,
   * or on an earlier released version for information (BRPM.007: "Priced on version n"); item rates
   * other than the scheme rate are priced and flagged.
   *
   * @param request product, insurer branch, items, period and version
   * @return breakdown, rates and the version used
   */
  @PostMapping("/quote")
  @PreAuthorize(CatalogAccess.READ)
  public RatingResponse quote(@Valid @RequestBody RatingRequest request) {
    Rating result = rating.rateDraft(request.query());
    Integer current =
        versions.current(request.productCode()).map(ProductVersionView::versionNo).orElse(null);
    LocalDate effectiveFrom =
        result.schemeVersion() == null
            ? null
            : versions.require(request.productCode(), result.schemeVersion()).getEffectiveFrom();
    return RatingResponse.from(result, effectiveFrom, current);
  }
}

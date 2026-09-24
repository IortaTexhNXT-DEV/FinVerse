package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.RatingRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.RatingResponse;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import jakarta.validation.Valid;
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

  /**
   * Creates the controller.
   *
   * @param rating rating service
   */
  public RatingController(RatingService rating) {
    this.rating = rating;
  }

  /**
   * Computes the premium breakdown; nothing is stored.
   *
   * @param request product, insurer branch, items and period
   * @return breakdown and rates used
   */
  @PostMapping("/quote")
  @PreAuthorize(CatalogAccess.READ)
  public RatingResponse quote(@Valid @RequestBody RatingRequest request) {
    return RatingResponse.from(rating.rate(request.query()));
  }
}

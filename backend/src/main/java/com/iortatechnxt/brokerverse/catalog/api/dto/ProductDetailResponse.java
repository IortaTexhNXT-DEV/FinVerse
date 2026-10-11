package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.RatingMethod;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import java.util.List;

/**
 * A product with its line's item kind and formula, the minimum fields and the mandatory documents
 * that apply to it (product detail screen, account wizard).
 *
 * @param product product
 * @param lineName product line name
 * @param riskItemKind kind of risk items of the line
 * @param ratingMethod Appendix A formula of the line
 * @param fieldRules effective minimum-field rules (narrowest scope wins)
 * @param requiredDocuments mandatory document types
 */
public record ProductDetailResponse(
    ProductResponse product,
    String lineName,
    RiskItemKind riskItemKind,
    RatingMethod ratingMethod,
    List<FieldRuleResponse> fieldRules,
    List<String> requiredDocuments) {}

package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.RuleScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * New or changed document rule.
 *
 * @param scope scope (ignored on update)
 * @param scopeCode '*', line or product (ignored on update)
 * @param documentType document type (ignored on update)
 * @param required mandatory
 */
public record DocumentRuleRequest(
    @NotNull RuleScope scope,
    @NotBlank @Size(max = 30) String scopeCode,
    @NotBlank @Size(max = 40) String documentType,
    boolean required) {}

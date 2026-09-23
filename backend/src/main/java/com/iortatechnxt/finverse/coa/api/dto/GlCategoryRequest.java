package com.iortatechnxt.finverse.coa.api.dto;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * GL category maintenance request.
 *
 * @param code code
 * @param name name
 * @param accountClass class
 * @param bankCategory bank flag
 */
public record GlCategoryRequest(
    @NotBlank @Size(max = 10) String code,
    @NotBlank @Size(max = 120) String name,
    @NotNull AccountClass accountClass,
    boolean bankCategory) {}

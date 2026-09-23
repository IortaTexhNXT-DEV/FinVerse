package com.iortatechnxt.finverse.common.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request carrying a mandatory reason (freeze, reject, reopen...).
 *
 * @param reason reason text
 */
public record ReasonRequest(@NotBlank @Size(max = 200) String reason) {}

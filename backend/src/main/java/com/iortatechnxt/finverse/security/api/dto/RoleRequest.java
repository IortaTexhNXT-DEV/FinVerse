package com.iortatechnxt.finverse.security.api.dto;

import com.iortatechnxt.finverse.security.domain.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * Role maintenance request. {@code code} is ignored on update.
 *
 * @param code code
 * @param name name
 * @param permissions permissions
 */
public record RoleRequest(
    @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Z0-9_]+") String code,
    @NotBlank @Size(max = 120) String name,
    @NotNull Set<Permission> permissions) {}

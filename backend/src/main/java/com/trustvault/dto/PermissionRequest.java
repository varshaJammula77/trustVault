package com.trustvault.dto;

import com.trustvault.model.PermissionLevel;
import jakarta.validation.constraints.*;

public record PermissionRequest(
    @NotBlank String userId,
    @NotNull PermissionLevel permission,
    @NotBlank @Size(min=5,max=200) String reason
) {}

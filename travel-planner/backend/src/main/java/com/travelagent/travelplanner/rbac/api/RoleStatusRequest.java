package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 更新角色启停状态。
 *
 * @param status ACTIVE 或 DISABLED
 */
public record RoleStatusRequest(
        @NotBlank
        @Pattern(regexp = "ACTIVE|DISABLED")
        @Schema(example = "DISABLED") String status) {
}

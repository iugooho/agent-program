package com.travelagent.travelplanner.rbac.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建自定义权限点请求。
 *
 * @param code 权限编码
 * @param name 权限名称
 * @param module 权限分组
 * @param type 权限类型
 * @param parentId 父权限主键
 * @param routePath 对应前端路由
 * @param riskLevel 风险等级
 * @param description 权限说明
 */
public record PermissionCreateRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(
                regexp = "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$",
                message = "code 必须为点分隔的小写权限码")
        @Schema(example = "report.export") String code,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String module,
        @NotBlank
        @Pattern(regexp = "MENU|PAGE|BUTTON|API", message = "type 必须为 MENU、PAGE、BUTTON 或 API")
        @Schema(example = "BUTTON") String type,
        Long parentId,
        @Size(max = 200) String routePath,
        @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "riskLevel 必须为 LOW、MEDIUM 或 HIGH")
        @Schema(example = "LOW") String riskLevel,
        @Size(max = 500) String description) {
}

package com.travelagent.travelplanner.auth.api;

import com.travelagent.travelplanner.auth.domain.AvatarAsset;
import com.travelagent.travelplanner.auth.domain.AvatarStorage;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 公开头像读取接口。 */
@RestController
@RequestMapping("/api/v1/users/avatars")
@Tag(name = "用户", description = "当前登录用户资料")
public class AvatarController {

    private final AvatarStorage avatarStorage;

    /**
     * 创建头像读取控制器。
     *
     * @param avatarStorage 头像存储
     */
    public AvatarController(AvatarStorage avatarStorage) {
        this.avatarStorage = avatarStorage;
    }

    /**
     * 按不可预测文件名读取头像。
     *
     * @param fileName 文件名
     * @return 头像内容
     */
    @GetMapping("/{fileName:.+}")
    @Operation(summary = "读取用户头像")
    public ResponseEntity<byte[]> avatar(@PathVariable String fileName) {
        AvatarAsset asset = avatarStorage.find(fileName)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "头像不存在"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(asset.content());
    }
}

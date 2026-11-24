package com.travelagent.travelplanner.auth.domain;

import java.util.Optional;

/** 头像文件存储接口。 */
public interface AvatarStorage {

    /**
     * 校验、裁切并保存头像。
     *
     * @param userId 用户主键
     * @param content 原始文件内容
     * @return 保存后的文件名
     */
    String store(long userId, byte[] content);

    /**
     * 按文件名读取头像。
     *
     * @param fileName 文件名
     * @return 头像文件
     */
    Optional<AvatarAsset> find(String fileName);

    /**
     * 删除头像文件，不存在时忽略。
     *
     * @param fileName 文件名
     */
    void delete(String fileName);
}

package com.travelagent.travelplanner.auth.domain;

/**
 * 可读取的头像文件。
 *
 * @param fileName 文件名
 * @param contentType MIME 类型
 * @param content 文件内容
 */
public record AvatarAsset(String fileName, String contentType, byte[] content) {
}

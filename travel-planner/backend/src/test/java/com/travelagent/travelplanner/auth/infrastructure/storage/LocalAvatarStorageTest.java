package com.travelagent.travelplanner.auth.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.travelagent.travelplanner.auth.domain.AvatarAsset;
import com.travelagent.travelplanner.common.error.BusinessException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 本地头像存储的图片校验与处理测试。 */
class LocalAvatarStorageTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    @DisplayName("头像中心裁切并统一保存为 512×512 PNG")
    void storesCroppedPng() throws IOException {
        LocalAvatarStorage storage = new LocalAvatarStorage(temporaryDirectory.toString());

        String fileName = storage.store(9L, image(900, 600, "png"));
        AvatarAsset asset = storage.find(fileName).orElseThrow();
        BufferedImage stored = ImageIO.read(new java.io.ByteArrayInputStream(asset.content()));

        assertThat(fileName).matches("9-[0-9a-f-]{36}\\.png");
        assertThat(asset.contentType()).isEqualTo("image/png");
        assertThat(stored.getWidth()).isEqualTo(512);
        assertThat(stored.getHeight()).isEqualTo(512);
    }

    @Test
    @DisplayName("非图片内容会被拒绝")
    void rejectsInvalidImage() {
        LocalAvatarStorage storage = new LocalAvatarStorage(temporaryDirectory.toString());

        assertThatThrownBy(() -> storage.store(9L, "not-an-image".getBytes()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JPEG 或 PNG");
    }

    private byte[] image(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(35, 89, 198));
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}

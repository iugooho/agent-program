package com.travelagent.travelplanner.auth.infrastructure.storage;

import com.travelagent.travelplanner.auth.domain.AvatarAsset;
import com.travelagent.travelplanner.auth.domain.AvatarStorage;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 本地磁盘头像存储：校验图片、中心裁切并统一输出 512×512 PNG。 */
@Component
public class LocalAvatarStorage implements AvatarStorage {

    private static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    private static final int MAX_SOURCE_EDGE = 4096;
    private static final int OUTPUT_EDGE = 512;
    private static final String CONTENT_TYPE = "image/png";
    private static final Pattern FILE_NAME_PATTERN =
            Pattern.compile("^\\d+-[0-9a-fA-F-]{36}\\.png$");

    private final Path root;

    /**
     * 创建本地头像存储。
     *
     * @param directory 头像目录
     */
    public LocalAvatarStorage(@Value("${app.profile.avatar-dir:uploads/avatars}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public String store(long userId, byte[] content) {
        if (content == null || content.length == 0) {
            throw invalidAvatar("请选择头像图片");
        }
        if (content.length > MAX_FILE_BYTES) {
            throw invalidAvatar("头像图片不能超过 2MB");
        }

        BufferedImage source = readImage(content);
        BufferedImage output = cropAndResize(source);
        String fileName = userId + "-" + UUID.randomUUID() + ".png";
        Path target = root.resolve(fileName);
        Path temporary = null;
        try {
            Files.createDirectories(root);
            temporary = Files.createTempFile(root, ".avatar-", ".tmp");
            if (!ImageIO.write(output, "png", temporary.toFile())) {
                throw new IllegalStateException("PNG 编码器不可用");
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            return fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("头像文件保存失败", exception);
        } finally {
            deleteQuietly(temporary);
        }
    }

    @Override
    public Optional<AvatarAsset> find(String fileName) {
        if (!validFileName(fileName)) {
            return Optional.empty();
        }
        Path path = root.resolve(fileName).normalize();
        if (!path.startsWith(root)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new AvatarAsset(fileName, CONTENT_TYPE, Files.readAllBytes(path)));
        } catch (NoSuchFileException exception) {
            return Optional.empty();
        } catch (IOException exception) {
            throw new IllegalStateException("头像文件读取失败", exception);
        }
    }

    @Override
    public void delete(String fileName) {
        if (!validFileName(fileName)) {
            return;
        }
        Path path = root.resolve(fileName).normalize();
        if (path.startsWith(root)) {
            deleteQuietly(path);
        }
    }

    private BufferedImage readImage(byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw invalidAvatar("无法读取头像图片");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidAvatar("头像仅支持 JPEG 或 PNG");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!"jpeg".equals(format) && !"jpg".equals(format) && !"png".equals(format)) {
                    throw invalidAvatar("头像仅支持 JPEG 或 PNG");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 32 || height < 32) {
                    throw invalidAvatar("头像图片尺寸不能小于 32×32");
                }
                if (width > MAX_SOURCE_EDGE || height > MAX_SOURCE_EDGE) {
                    throw invalidAvatar("头像图片尺寸不能超过 4096×4096");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("头像图片解析失败", exception);
        }
    }

    private BufferedImage cropAndResize(BufferedImage source) {
        int edge = Math.min(source.getWidth(), source.getHeight());
        int sourceX = (source.getWidth() - edge) / 2;
        int sourceY = (source.getHeight() - edge) / 2;
        BufferedImage output = new BufferedImage(OUTPUT_EDGE, OUTPUT_EDGE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(
                    source,
                    0,
                    0,
                    OUTPUT_EDGE,
                    OUTPUT_EDGE,
                    sourceX,
                    sourceY,
                    sourceX + edge,
                    sourceY + edge,
                    null);
        } finally {
            graphics.dispose();
        }
        return output;
    }

    private static boolean validFileName(String fileName) {
        return fileName != null && FILE_NAME_PATTERN.matcher(fileName).matches();
    }

    private static BusinessException invalidAvatar(String message) {
        return new BusinessException(ErrorCode.PARAM_INVALID, message);
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 清理失败不覆盖主流程结果，后续可依赖运维清理孤儿文件。
        }
    }
}

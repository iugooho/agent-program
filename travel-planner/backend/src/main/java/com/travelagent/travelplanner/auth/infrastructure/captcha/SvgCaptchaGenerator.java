package com.travelagent.travelplanner.auth.infrastructure.captcha;

import com.travelagent.travelplanner.auth.domain.CaptchaGenerator;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/** 生成带干扰线和字符旋转的自托管 SVG 图形验证码。 */
@Component
public class SvgCaptchaGenerator implements CaptchaGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 5;
    private static final int WIDTH = 160;
    private static final int HEIGHT = 56;

    private final SecureRandom random = new SecureRandom();

    @Override
    public GeneratedCaptcha generate() {
        StringBuilder answer = new StringBuilder(CODE_LENGTH);
        StringBuilder characters = new StringBuilder();
        for (int index = 0; index < CODE_LENGTH; index++) {
            char value = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
            answer.append(value);
            int x = 16 + index * 28;
            int y = 38 + random.nextInt(7) - 3;
            int angle = random.nextInt(25) - 12;
            characters.append("<text x=\"")
                    .append(x)
                    .append("\" y=\"")
                    .append(y)
                    .append("\" transform=\"rotate(")
                    .append(angle)
                    .append(" ")
                    .append(x)
                    .append(" ")
                    .append(y)
                    .append(")\" font-family=\"Consolas,monospace\" font-size=\"30\" font-weight=\"700\" fill=\"#0f172a\">")
                    .append(value)
                    .append("</text>");
        }

        StringBuilder noise = new StringBuilder();
        for (int index = 0; index < 7; index++) {
            noise.append("<line x1=\"")
                    .append(random.nextInt(WIDTH))
                    .append("\" y1=\"")
                    .append(random.nextInt(HEIGHT))
                    .append("\" x2=\"")
                    .append(random.nextInt(WIDTH))
                    .append("\" y2=\"")
                    .append(random.nextInt(HEIGHT))
                    .append("\" stroke=\"#64748b\" stroke-width=\"1\" opacity=\"0.35\"/>");
        }

        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">
                  <rect width="100%%" height="100%%" fill="#f8fafc"/>
                  <g>%s</g>
                  <g>%s</g>
                </svg>
                """.formatted(WIDTH, HEIGHT, WIDTH, HEIGHT, noise, characters);
        String dataUrl = "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return new GeneratedCaptcha(answer.toString(), dataUrl);
    }
}

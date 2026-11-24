package com.travelagent.travelplanner.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import com.travelagent.travelplanner.auth.application.EmailCodeHasher;
import com.travelagent.travelplanner.auth.domain.EmailCodeStore;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;

/** 个人资料和长期偏好接口集成测试。 */
@SpringBootTest(properties = "app.profile.avatar-dir=target/test-avatars")
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class UserProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailCodeStore emailCodeStore;

    @Test
    @DisplayName("更新基础资料并读写长期偏好")
    void updatesProfileAndPreferences() throws Exception {
        String bearer = bearerByLogin("demo", "Demo123456");

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname":"新的昵称",
                                  "phone":"13800000000",
                                  "bio":"喜欢慢节奏旅行"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("新的昵称"))
                .andExpect(jsonPath("$.data.phone").value("13800000000"))
                .andExpect(jsonPath("$.data.bio").value("喜欢慢节奏旅行"))
                .andExpect(jsonPath("$.data.lastLoginTime").isNotEmpty());

        mockMvc.perform(put("/api/v1/users/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "travelStyleDefault": "RELAXED",
                                  "foodHabit": "清淡",
                                  "forbiddenList": ["不购物", "不吃辣"],
                                  "accommodationPreference": "近地铁",
                                  "budgetLevel": "COMFORT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.travelStyleDefault").value("RELAXED"))
                .andExpect(jsonPath("$.data.forbiddenList[0]").value("不购物"));

        mockMvc.perform(get("/api/v1/users/me/preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.foodHabit").value("清淡"))
                .andExpect(jsonPath("$.data.budgetLevel").value("COMFORT"));
    }

    @Test
    @DisplayName("个人中心概览只返回当前用户汇总数据")
    void returnsOwnedTripOverview() throws Exception {
        String bearer = bearerByLogin("demo", "Demo123456");

        mockMvc.perform(get("/api/v1/users/me/overview")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTrips").isNumber())
                .andExpect(jsonPath("$.data.upcomingTrips").isNumber())
                .andExpect(jsonPath("$.data.cities").isNumber())
                .andExpect(jsonPath("$.data.totalDays").isNumber())
                .andExpect(jsonPath("$.data.recentTrips").isArray());
    }

    @Test
    @DisplayName("上传头像后可公开读取生成的头像文件")
    void uploadsAndReadsAvatar() throws Exception {
        String bearer = bearerByLogin("demo", "Demo123456");
        MockMultipartFile avatar = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                pngImage());

        MvcResult result = mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(avatar)
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").isNotEmpty())
                .andReturn();
        String avatarUrl = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("avatarUrl")
                .asText();

        mockMvc.perform(get(avatarUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    @DisplayName("头像上传缺少文件时返回统一参数错误")
    void avatarUploadRequiresFile() throws Exception {
        String bearer = bearerByLogin("demo", "Demo123456");

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()))
                .andExpect(jsonPath("$.message").value("请选择需要上传的文件"));
    }

    @Test
    @DisplayName("敏感资料修改必须提供当前邮箱验证码")
    void sensitiveProfileUpdateRequiresCurrentEmailCode() throws Exception {
        String bearer = bearerByLogin("demo", "Demo123456");
        String newUsername = "profile_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newUsername", newUsername))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("修改敏感资料需要邮箱验证码"));

        emailCodeStore.saveCode(
                "demo@example.com",
                EmailPurpose.UPDATE_PROFILE,
                EmailCodeHasher.hash("demo@example.com", EmailPurpose.UPDATE_PROFILE, "123456"),
                Duration.ofMinutes(5));

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newUsername", newUsername,
                                "verificationCode", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(newUsername));
    }

    @Test
    @DisplayName("修改邮箱同时验证当前邮箱和新邮箱")
    void emailUpdateRequiresBothEmailCodes() throws Exception {
        String bearer = bearerByLogin("demo", "Demo123456");
        String newEmail = "profile_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                + "@example.com";
        emailCodeStore.saveCode(
                "demo@example.com",
                EmailPurpose.UPDATE_PROFILE,
                EmailCodeHasher.hash("demo@example.com", EmailPurpose.UPDATE_PROFILE, "123456"),
                Duration.ofMinutes(5));

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newEmail", newEmail,
                                "verificationCode", "123456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("修改邮箱需要验证新邮箱"));

        emailCodeStore.saveCode(
                newEmail,
                EmailPurpose.UPDATE_PROFILE,
                EmailCodeHasher.hash(newEmail, EmailPurpose.UPDATE_PROFILE, "654321"),
                Duration.ofMinutes(5));

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newEmail", newEmail,
                                "verificationCode", "123456",
                                "newEmailCode", "654321"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(newEmail));
    }

    @Test
    @DisplayName("未登录不能读取个人偏好")
    void preferencesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/preferences"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.code()));
    }

    private String bearerByLogin(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginType", "PASSWORD",
                                "username", username,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
        return "Bearer " + token;
    }

    private byte[] pngImage() throws Exception {
        BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(36, 89, 198));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}

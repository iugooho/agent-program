package com.travelagent.travelplanner.auth.config;

import com.travelagent.travelplanner.auth.domain.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 为本地 H2 数据库创建可直接登录的演示账号。 */
@Component
@Profile("local")
public class LocalUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String email;
    private final String nickname;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminEmail;
    private final String adminNickname;

    /**
     * 创建初始化器。
     *
     * @param userRepository 用户仓储
     * @param passwordEncoder 密码编码器
     * @param username 开发账号
     * @param password 开发密码
     * @param email 开发邮箱
     * @param nickname 开发昵称
     * @param adminUsername 管理员账号
     * @param adminPassword 管理员密码
     * @param adminEmail 管理员邮箱
     * @param adminNickname 管理员昵称
     */
    public LocalUserInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.dev-user.username:demo}") String username,
            @Value("${app.dev-user.password:}") String password,
            @Value("${app.dev-user.email:demo@example.com}") String email,
            @Value("${app.dev-user.nickname:本地演示用户}") String nickname,
            @Value("${app.dev-admin.username:admin}") String adminUsername,
            @Value("${app.dev-admin.password:}") String adminPassword,
            @Value("${app.dev-admin.email:admin@example.com}") String adminEmail,
            @Value("${app.dev-admin.nickname:本地管理员}") String adminNickname) {
        if (isUnconfigured(password) || isUnconfigured(adminPassword)) {
            throw new IllegalStateException(
                    "local profile 必须通过 APP_DEV_USER_PASSWORD 和 "
                            + "APP_DEV_ADMIN_PASSWORD 配置开发账号密码");
        }
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.email = email;
        this.nickname = nickname;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminEmail = adminEmail;
        this.adminNickname = adminNickname;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByUsername(username).isEmpty()) {
            userRepository.createLocalUser(username, email, passwordEncoder.encode(password), nickname);
        }
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            userRepository.createLocalAdmin(
                    adminUsername,
                    adminEmail,
                    passwordEncoder.encode(adminPassword),
                    adminNickname);
        }
    }

    private static boolean isUnconfigured(String password) {
        return password == null || password.isBlank() || password.startsWith("CHANGE_ME_");
    }
}

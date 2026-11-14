package com.travelagent.travelplanner.auth.config;

import com.travelagent.travelplanner.auth.domain.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 需要显式启用的管理员初始化器，用于 Docker 或全新开发数据库。 */
@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String email;
    private final String nickname;

    /**
     * 创建管理员初始化器。
     *
     * @param userRepository 用户仓储
     * @param passwordEncoder 密码编码器
     * @param username 管理员账号
     * @param password 管理员密码
     * @param email 管理员邮箱
     * @param nickname 管理员昵称
     */
    public BootstrapAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.username:admin}") String username,
            @Value("${app.bootstrap-admin.password:Admin123456}") String password,
            @Value("${app.bootstrap-admin.email:admin@example.com}") String email,
            @Value("${app.bootstrap-admin.nickname:Docker 管理员}") String nickname) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.email = email;
        this.nickname = nickname;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByUsername(username).isEmpty()
                && userRepository.findByEmail(email).isEmpty()) {
            userRepository.createLocalAdmin(
                    username,
                    email,
                    passwordEncoder.encode(password),
                    nickname);
        }
    }
}

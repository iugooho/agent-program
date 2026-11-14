package com.travelagent.travelplanner.auth.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelagent.travelplanner.auth.domain.UserAccount;
import com.travelagent.travelplanner.auth.domain.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Docker 管理员初始化器测试。 */
class BootstrapAdminInitializerTest {

    @Test
    @DisplayName("账号不存在时创建超级管理员")
    void createsAdminWhenMissing() {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(repository.findByUsername("admin")).thenReturn(Optional.empty());
        when(repository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(encoder.encode("Admin123456")).thenReturn("encoded");

        BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(
                repository,
                encoder,
                "admin",
                "Admin123456",
                "admin@example.com",
                "Docker 管理员");
        initializer.run(null);

        verify(repository).createLocalAdmin("admin", "admin@example.com", "encoded", "Docker 管理员");
    }

    @Test
    @DisplayName("账号已存在时不重复创建")
    void doesNotCreateDuplicateAdmin() {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin()));

        BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(
                repository,
                encoder,
                "admin",
                "Admin123456",
                "admin@example.com",
                "Docker 管理员");
        initializer.run(null);

        verify(repository, never()).createLocalAdmin(
                "admin", "admin@example.com", "encoded", "Docker 管理员");
    }

    private UserAccount admin() {
        return new UserAccount(
                1L,
                "admin",
                "admin@example.com",
                "encoded",
                "ADMIN",
                "ACTIVE",
                "Docker 管理员",
                null,
                LocalDateTime.now(),
                null);
    }
}

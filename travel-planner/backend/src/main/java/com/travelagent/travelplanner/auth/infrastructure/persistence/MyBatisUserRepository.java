package com.travelagent.travelplanner.auth.infrastructure.persistence;

import com.travelagent.travelplanner.auth.domain.UserAccount;
import com.travelagent.travelplanner.auth.domain.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis 的用户仓储实现。 */
@Repository
public class MyBatisUserRepository implements UserRepository {

    private final UserAccountMapper mapper;

    /**
     * 创建仓储。
     *
     * @param mapper 用户 Mapper
     */
    public MyBatisUserRepository(UserAccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(mapper.findByUsername(username)).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return Optional.ofNullable(mapper.findByEmail(email)).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(long id) {
        return Optional.ofNullable(mapper.findById(id)).map(this::toDomain);
    }

    @Override
    public void updateProfile(
            long userId,
            String username,
            String email,
            String passwordHash,
            String nickname,
            String phone) {
        mapper.updateProfile(userId, username, email, passwordHash, nickname, phone);
    }

    @Override
    public void createUser(String username, String email, String passwordHash, String nickname) {
        mapper.insertUser(username, email, passwordHash, nickname);
        mapper.assignRoleByUsername(username, "USER");
    }

    @Override
    public void updateLastLoginTime(long id) {
        mapper.updateLastLoginTime(id);
    }

    @Override
    public void createLocalUser(String username, String email, String passwordHash, String nickname) {
        mapper.insertLocalUser(username, email, passwordHash, nickname);
        mapper.assignRoleByUsername(username, "USER");
    }

    @Override
    public void createLocalAdmin(String username, String email, String passwordHash, String nickname) {
        mapper.insertLocalAdmin(username, email, passwordHash, nickname);
        mapper.assignRoleByUsername(username, "SUPER_ADMIN");
    }

    private UserAccount toDomain(UserAccountRow row) {
        return new UserAccount(
                row.id(),
                row.username(),
                row.email(),
                row.passwordHash(),
                row.role(),
                row.status(),
                row.nickname(),
                row.phone(),
                row.createTime(),
                row.lastLoginTime());
    }
}

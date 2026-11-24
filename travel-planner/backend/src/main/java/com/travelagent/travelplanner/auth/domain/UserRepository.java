package com.travelagent.travelplanner.auth.domain;

import java.util.Optional;

/** 登录用户仓储接口。 */
public interface UserRepository {

    /**
     * 按用户名查询。
     *
     * @param username 登录账号
     * @return 用户
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * 按邮箱查询。
     *
     * @param email 登录邮箱
     * @return 用户
     */
    Optional<UserAccount> findByEmail(String email);

    /**
     * 按主键查询。
     *
     * @param id 用户主键
     * @return 用户
     */
    Optional<UserAccount> findById(long id);

    /**
     * 更新用户账号资料。
     *
     * @param userId 用户主键
     * @param username 登录账号
     * @param email 登录邮箱
     * @param passwordHash 密码哈希
     * @param nickname 昵称
     * @param phone 联系电话
     * @param bio 个人简介
     */
    void updateProfile(
            long userId,
            String username,
            String email,
            String passwordHash,
            String nickname,
            String phone,
            String bio);

    /**
     * 更新头像地址。
     *
     * @param userId 用户主键
     * @param avatarUrl 头像访问地址
     */
    void updateAvatarUrl(long userId, String avatarUrl);

    /**
     * 脱敏并软删除用户。
     *
     * @param userId 用户主键
     * @param anonymousUsername 脱敏账号
     * @param anonymousEmail 脱敏邮箱
     * @param passwordHash 随机失效密码哈希
     */
    void softDelete(long userId, String anonymousUsername, String anonymousEmail, String passwordHash);

    /**
     * 创建新用户。
     *
     * @param username 登录账号
     * @param email 登录邮箱
     * @param passwordHash 密码哈希
     * @param nickname 昵称
     */
    void createUser(String username, String email, String passwordHash, String nickname);

    /**
     * 更新最近登录时间。
     *
     * @param id 用户主键
     */
    void updateLastLoginTime(long id);

    /**
     * 创建本地开发账号。
     *
     * @param username 登录账号
     * @param email 登录邮箱
     * @param passwordHash 密码哈希
     * @param nickname 昵称
     */
    void createLocalUser(String username, String email, String passwordHash, String nickname);

    /**
     * 创建本地管理员账号。
     *
     * @param username 登录账号
     * @param email 登录邮箱
     * @param passwordHash 密码哈希
     * @param nickname 昵称
     */
    void createLocalAdmin(String username, String email, String passwordHash, String nickname);
}

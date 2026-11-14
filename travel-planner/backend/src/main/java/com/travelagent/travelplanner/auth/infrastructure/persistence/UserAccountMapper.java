package com.travelagent.travelplanner.auth.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** users 表查询 Mapper。 */
@Mapper
public interface UserAccountMapper {

    /**
     * 按用户名查询。
     *
     * @param username 登录账号
     * @return 用户行，不存在时返回 null
     */
    @Select("""
            SELECT id, username, email, password_hash AS passwordHash, role, status,
                   nickname, phone, create_time AS createTime, last_login_time AS lastLoginTime
            FROM users
            WHERE username = #{username}
            LIMIT 1
            """)
    UserAccountRow findByUsername(@Param("username") String username);

    /**
     * 按邮箱查询。
     *
     * @param email 登录邮箱
     * @return 用户行，不存在时返回 null
     */
    @Select("""
            SELECT id, username, email, password_hash AS passwordHash, role, status,
                   nickname, phone, create_time AS createTime, last_login_time AS lastLoginTime
            FROM users
            WHERE email = #{email}
            LIMIT 1
            """)
    UserAccountRow findByEmail(@Param("email") String email);

    /**
     * 按主键查询。
     *
     * @param id 用户主键
     * @return 用户行，不存在时返回 null
     */
    @Select("""
            SELECT id, username, email, password_hash AS passwordHash, role, status,
                   nickname, phone, create_time AS createTime, last_login_time AS lastLoginTime
            FROM users
            WHERE id = #{id}
            LIMIT 1
            """)
    UserAccountRow findById(@Param("id") long id);

    /**
     * 更新用户资料。
     *
     * @param id 用户主键
     * @param username 登录账号
     * @param email 登录邮箱
     * @param passwordHash 密码哈希
     * @param nickname 昵称
     * @param phone 联系电话
     * @return 更新行数
     */
    @Update("""
            UPDATE users
            SET username = #{username},
                email = #{email},
                password_hash = #{passwordHash},
                nickname = #{nickname},
                phone = #{phone},
                update_time = CURRENT_TIMESTAMP(6)
            WHERE id = #{id}
            """)
    int updateProfile(
            @Param("id") long id,
            @Param("username") String username,
            @Param("email") String email,
            @Param("passwordHash") String passwordHash,
            @Param("nickname") String nickname,
            @Param("phone") String phone);

    /**
     * 更新最近登录时间。
     *
     * @param id 用户主键
     * @return 更新行数
     */
    @Update("UPDATE users SET last_login_time = CURRENT_TIMESTAMP(6) WHERE id = #{id}")
    int updateLastLoginTime(@Param("id") long id);

    /**
     * 创建正式注册用户。
     *
     * @param username 登录账号
     * @param email 登录邮箱
     * @param passwordHash 密码哈希
     * @param nickname 昵称
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO users (username, email, password_hash, role, status, nickname)
            VALUES (#{username}, #{email}, #{passwordHash}, 'USER', 'ACTIVE', #{nickname})
            """)
    int insertUser(
            @Param("username") String username,
            @Param("email") String email,
            @Param("passwordHash") String passwordHash,
            @Param("nickname") String nickname);

    /**
     * 为新用户分配内置角色。
     *
     * @param username 登录账号
     * @param roleCode 角色编码
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO sys_user_role (user_id, role_id)
            SELECT u.id, r.id
            FROM users u
            JOIN sys_role r ON r.code = #{roleCode}
            WHERE u.username = #{username}
            """)
    int assignRoleByUsername(
            @Param("username") String username,
            @Param("roleCode") String roleCode);

    /**
     * 创建本地开发账号。
     *
     * @param username 登录账号
     * @param email 登录邮箱
     * @param passwordHash 密码哈希
     * @param nickname 昵称
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO users (username, email, password_hash, role, status, nickname)
            VALUES (#{username}, #{email}, #{passwordHash}, 'USER', 'ACTIVE', #{nickname})
            """)
    int insertLocalUser(
            @Param("username") String username,
            @Param("email") String email,
            @Param("passwordHash") String passwordHash,
            @Param("nickname") String nickname);

    /**
     * 创建本地管理员账号。
     *
     * @param username 登录账号
     * @param email 登录邮箱
     * @param passwordHash 密码哈希
     * @param nickname 昵称
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO users (username, email, password_hash, role, status, nickname)
            VALUES (#{username}, #{email}, #{passwordHash}, 'ADMIN', 'ACTIVE', #{nickname})
            """)
    int insertLocalAdmin(
            @Param("username") String username,
            @Param("email") String email,
            @Param("passwordHash") String passwordHash,
            @Param("nickname") String nickname);
}

package com.travelagent.travelplanner.rbac.infrastructure.persistence;

import com.travelagent.travelplanner.rbac.infrastructure.persistence.RbacRows.AssignmentRow;
import com.travelagent.travelplanner.rbac.infrastructure.persistence.RbacRows.PermissionRow;
import com.travelagent.travelplanner.rbac.infrastructure.persistence.RbacRows.RoleRow;
import com.travelagent.travelplanner.rbac.infrastructure.persistence.RbacRows.UserRow;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** RBAC 表查询与关系更新 Mapper。 */
@Mapper
public interface RbacMapper {

    /**
     * 查询全部角色。
     *
     * @return 角色行
     */
    @Select("""
            SELECT id, code, name, description, built_in AS builtIn
            FROM sys_role
            ORDER BY id
            """)
    List<RoleRow> findRoles();

    /**
     * 查询全部权限。
     *
     * @return 权限行
     */
    @Select("""
            SELECT id, code, name, module, description
            FROM sys_permission
            ORDER BY sort_order, id
            """)
    List<PermissionRow> findPermissions();

    /**
     * 查询权限配置页用户。
     *
     * @return 用户行
     */
    @Select("""
            SELECT id, username, nickname, email, status
            FROM users
            WHERE status <> 'DELETED'
            ORDER BY id
            """)
    List<UserRow> findUsers();

    /**
     * 查询全部角色权限关系。
     *
     * @return 关系行
     */
    @Select("""
            SELECT role_id AS ownerId, permission_id AS targetId
            FROM sys_role_permission
            ORDER BY role_id, permission_id
            """)
    List<AssignmentRow> findRolePermissions();

    /**
     * 查询全部用户角色关系。
     *
     * @return 关系行
     */
    @Select("""
            SELECT user_id AS ownerId, role_id AS targetId
            FROM sys_user_role
            ORDER BY user_id, role_id
            """)
    List<AssignmentRow> findUserRoles();

    /**
     * 查询用户角色编码。
     *
     * @param userId 用户主键
     * @return 角色编码
     */
    @Select("""
            SELECT r.code
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            ORDER BY r.id
            """)
    List<String> findRoleCodesByUser(@Param("userId") long userId);

    /**
     * 查询用户权限编码。
     *
     * @param userId 用户主键
     * @return 权限编码
     */
    @Select("""
            SELECT DISTINCT p.code
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId}
            ORDER BY p.sort_order, p.id
            """)
    List<String> findPermissionCodesByUser(@Param("userId") long userId);

    /**
     * 删除角色已有权限。
     *
     * @param roleId 角色主键
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteRolePermissions(@Param("roleId") long roleId);

    /**
     * 新增角色权限。
     *
     * @param roleId 角色主键
     * @param permissionIds 权限主键
     * @return 新增行数
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_permission (role_id, permission_id)
            VALUES
            <foreach collection="permissionIds" item="permissionId" separator=",">
                (#{roleId}, #{permissionId})
            </foreach>
            </script>
            """)
    int insertRolePermissions(
            @Param("roleId") long roleId,
            @Param("permissionIds") List<Long> permissionIds);

    /**
     * 删除用户已有角色。
     *
     * @param userId 用户主键
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoles(@Param("userId") long userId);

    /**
     * 新增用户角色。
     *
     * @param userId 用户主键
     * @param roleIds 角色主键
     * @return 新增行数
     */
    @Insert("""
            <script>
            INSERT INTO sys_user_role (user_id, role_id)
            VALUES
            <foreach collection="roleIds" item="roleId" separator=",">
                (#{userId}, #{roleId})
            </foreach>
            </script>
            """)
    int insertUserRoles(@Param("userId") long userId, @Param("roleIds") List<Long> roleIds);
}

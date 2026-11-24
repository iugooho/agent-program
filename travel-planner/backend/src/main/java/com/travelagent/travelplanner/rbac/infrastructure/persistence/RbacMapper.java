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
import org.apache.ibatis.annotations.Update;

/** RBAC 表查询与关系更新 Mapper。 */
@Mapper
public interface RbacMapper {

    /**
     * 查询全部角色。
     *
     * @return 角色行
     */
    @Select("""
            SELECT id, code, name, description, built_in AS builtIn, status,
                   data_scope AS dataScope
            FROM sys_role
            WHERE status <> 'DELETED'
            ORDER BY id
            """)
    List<RoleRow> findRoles();

    /**
     * 按主键查询角色。
     *
     * @param roleId 角色主键
     * @return 角色行
     */
    @Select("""
            SELECT id, code, name, description, built_in AS builtIn, status,
                   data_scope AS dataScope
            FROM sys_role
            WHERE id = #{roleId}
            LIMIT 1
            """)
    RoleRow findRoleById(@Param("roleId") long roleId);

    /**
     * 按编码查询角色，包括已删除角色，用于唯一性判断。
     *
     * @param code 角色编码
     * @return 角色行
     */
    @Select("""
            SELECT id, code, name, description, built_in AS builtIn, status,
                   data_scope AS dataScope
            FROM sys_role
            WHERE code = #{code}
            LIMIT 1
            """)
    RoleRow findRoleByCode(@Param("code") String code);

    /**
     * 创建自定义角色。
     *
     * @param code 角色编码
     * @param name 角色名称
     * @param description 角色说明
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO sys_role (code, name, description, built_in, status)
            VALUES (#{code}, #{name}, #{description}, FALSE, 'ACTIVE')
            """)
    int insertRole(
            @Param("code") String code,
            @Param("name") String name,
            @Param("description") String description);

    /**
     * 更新角色名称和说明。
     *
     * @param roleId 角色主键
     * @param name 角色名称
     * @param description 角色说明
     * @return 更新行数
     */
    @Update("""
            UPDATE sys_role
            SET name = #{name},
                description = #{description},
                update_time = CURRENT_TIMESTAMP(6)
            WHERE id = #{roleId}
            """)
    int updateRole(
            @Param("roleId") long roleId,
            @Param("name") String name,
            @Param("description") String description);

    /**
     * 更新角色状态。
     *
     * @param roleId 角色主键
     * @param status 角色状态
     * @return 更新行数
     */
    @Update("""
            UPDATE sys_role
            SET status = #{status},
                update_time = CURRENT_TIMESTAMP(6)
            WHERE id = #{roleId}
            """)
    int updateRoleStatus(@Param("roleId") long roleId, @Param("status") String status);

    /**
     * 删除角色的全部用户关联。
     *
     * @param roleId 角色主键
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_user_role WHERE role_id = #{roleId}")
    int deleteRoleUsers(@Param("roleId") long roleId);

    /**
     * 将角色标记为已删除。
     *
     * @param roleId 角色主键
     * @return 更新行数
     */
    @Update("""
            UPDATE sys_role
            SET status = 'DELETED',
                update_time = CURRENT_TIMESTAMP(6)
            WHERE id = #{roleId}
            """)
    int markRoleDeleted(@Param("roleId") long roleId);

    /**
     * 查询全部权限。
     *
     * @return 权限行
     */
    @Select("""
            SELECT id, parent_id AS parentId, code, name, module, type,
                   route_path AS routePath, description, status,
                   risk_level AS riskLevel, built_in AS builtIn
            FROM sys_permission
            WHERE status <> 'DELETED'
            ORDER BY sort_order, id
            """)
    List<PermissionRow> findPermissions();

    /**
     * 按主键查询权限。
     *
     * @param permissionId 权限主键
     * @return 权限行
     */
    @Select("""
            SELECT id, parent_id AS parentId, code, name, module, type,
                   route_path AS routePath, description, status,
                   risk_level AS riskLevel, built_in AS builtIn
            FROM sys_permission
            WHERE id = #{permissionId}
            LIMIT 1
            """)
    PermissionRow findPermissionById(@Param("permissionId") long permissionId);

    /**
     * 按编码查询权限。
     *
     * @param code 权限编码
     * @return 权限行
     */
    @Select("""
            SELECT id, parent_id AS parentId, code, name, module, type,
                   route_path AS routePath, description, status,
                   risk_level AS riskLevel, built_in AS builtIn
            FROM sys_permission
            WHERE code = #{code}
            LIMIT 1
            """)
    PermissionRow findPermissionByCode(@Param("code") String code);

    /**
     * 查询当前最大权限排序值。
     *
     * @return 最大排序值
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM sys_permission")
    int findMaxPermissionSortOrder();

    /**
     * 创建自定义权限点。
     *
     * @param code 权限编码
     * @param name 权限名称
     * @param module 权限分组
     * @param type 权限类型
     * @param parentId 父权限主键
     * @param routePath 前端路由
     * @param riskLevel 风险等级
     * @param description 权限说明
     * @param sortOrder 排序值
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO sys_permission (
                code, name, module, type, parent_id, route_path,
                description, status, risk_level, sort_order, built_in
            )
            VALUES (
                #{code}, #{name}, #{module}, #{type}, #{parentId}, #{routePath},
                #{description}, 'ACTIVE', #{riskLevel}, #{sortOrder}, FALSE
            )
            """)
    int insertPermission(
            @Param("code") String code,
            @Param("name") String name,
            @Param("module") String module,
            @Param("type") String type,
            @Param("parentId") Long parentId,
            @Param("routePath") String routePath,
            @Param("riskLevel") String riskLevel,
            @Param("description") String description,
            @Param("sortOrder") int sortOrder);

    /**
     * 标记权限点为已删除。
     *
     * @param permissionId 权限主键
     * @return 更新行数
     */
    @Update("""
            UPDATE sys_permission
            SET status = 'DELETED'
            WHERE id = #{permissionId}
            """)
    int markPermissionDeleted(@Param("permissionId") long permissionId);

    /**
     * 查询权限下仍有效的直接子权限数量。
     *
     * @param permissionId 权限主键
     * @return 子权限数量
     */
    @Select("""
            SELECT COUNT(*)
            FROM sys_permission
            WHERE parent_id = #{permissionId}
              AND status = 'ACTIVE'
            """)
    int countActivePermissionChildren(@Param("permissionId") long permissionId);

    /**
     * 查询全部权限包。
     *
     * @return 权限包行
     */
    @Select("""
            SELECT id, code, name, description, status
            FROM sys_permission_package
            WHERE status <> 'DELETED'
            ORDER BY sort_order, id
            """)
    List<RbacRows.PermissionPackageRow> findPermissionPackages();

    /**
     * 查询权限包与权限关系。
     *
     * @return 关系行
     */
    @Select("""
            SELECT package_id AS ownerId, permission_id AS targetId
            FROM sys_permission_package_item
            ORDER BY package_id, permission_id
            """)
    List<AssignmentRow> findPackagePermissions();

    /**
     * 查询全部权限依赖。
     *
     * @return 依赖行
     */
    @Select("""
            SELECT permission_id AS permissionId,
                   required_permission_id AS requiredPermissionId
            FROM sys_permission_dependency
            ORDER BY permission_id, required_permission_id
            """)
    List<RbacRows.PermissionDependencyRow> findPermissionDependencies();

    /**
     * 给角色新增单个权限点。
     *
     * @param roleId 角色主键
     * @param permissionId 权限主键
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO sys_role_permission (role_id, permission_id)
            VALUES (#{roleId}, #{permissionId})
            """)
    int insertRolePermission(
            @Param("roleId") long roleId,
            @Param("permissionId") long permissionId);

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
            FROM sys_role_permission rp
            JOIN sys_permission p ON p.id = rp.permission_id
            WHERE p.status <> 'DELETED'
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
              AND r.status = 'ACTIVE'
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
            JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND r.status = 'ACTIVE'
              AND p.status = 'ACTIVE'
              AND p.type <> 'CATALOG'
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

    /**
     * 查询角色成员。
     *
     * @param roleId 角色主键
     * @return 用户主键
     */
    @Select("""
            SELECT user_id
            FROM sys_user_role
            WHERE role_id = #{roleId}
            ORDER BY user_id
            """)
    List<Long> findUserIdsByRole(@Param("roleId") long roleId);

    /**
     * 批量添加角色成员。
     *
     * @param roleId 角色主键
     * @param userIds 用户主键
     * @return 插入行数
     */
    @Insert("""
            <script>
            INSERT INTO sys_user_role (user_id, role_id)
            VALUES
            <foreach collection="userIds" item="userId" separator=",">
                (#{userId}, #{roleId})
            </foreach>
            </script>
            """)
    int insertUsersForRole(
            @Param("roleId") long roleId,
            @Param("userIds") List<Long> userIds);

    /**
     * 查询全部有效部门。
     *
     * @return 部门行
     */
    @Select("""
            SELECT id, parent_id AS parentId, code, name,
                   leader_user_id AS leaderUserId, status, sort_order AS sortOrder
            FROM sys_department
            WHERE status <> 'DELETED'
            ORDER BY sort_order, id
            """)
    List<RbacRows.DepartmentRow> findDepartments();

    /**
     * 按主键查询部门。
     *
     * @param departmentId 部门主键
     * @return 部门行
     */
    @Select("""
            SELECT id, parent_id AS parentId, code, name,
                   leader_user_id AS leaderUserId, status, sort_order AS sortOrder
            FROM sys_department
            WHERE id = #{departmentId}
            LIMIT 1
            """)
    RbacRows.DepartmentRow findDepartmentById(@Param("departmentId") long departmentId);

    /**
     * 按编码查询部门。
     *
     * @param code 部门编码
     * @return 部门行
     */
    @Select("""
            SELECT id, parent_id AS parentId, code, name,
                   leader_user_id AS leaderUserId, status, sort_order AS sortOrder
            FROM sys_department
            WHERE code = #{code}
            LIMIT 1
            """)
    RbacRows.DepartmentRow findDepartmentByCode(@Param("code") String code);

    /**
     * 创建部门。
     *
     * @param parentId 父部门主键
     * @param code 部门编码
     * @param name 部门名称
     * @param leaderUserId 负责人主键
     * @param sortOrder 排序值
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO sys_department (
                parent_id, code, name, leader_user_id, status, sort_order
            )
            VALUES (
                #{parentId}, #{code}, #{name}, #{leaderUserId}, 'ACTIVE', #{sortOrder}
            )
            """)
    int insertDepartment(
            @Param("parentId") Long parentId,
            @Param("code") String code,
            @Param("name") String name,
            @Param("leaderUserId") Long leaderUserId,
            @Param("sortOrder") int sortOrder);

    /**
     * 更新部门。
     *
     * @param departmentId 部门主键
     * @param parentId 父部门主键
     * @param name 部门名称
     * @param leaderUserId 负责人主键
     * @param status 状态
     * @param sortOrder 排序值
     * @return 更新行数
     */
    @Update("""
            UPDATE sys_department
            SET parent_id = #{parentId},
                name = #{name},
                leader_user_id = #{leaderUserId},
                status = #{status},
                sort_order = #{sortOrder},
                update_time = CURRENT_TIMESTAMP(6)
            WHERE id = #{departmentId}
            """)
    int updateDepartment(
            @Param("departmentId") long departmentId,
            @Param("parentId") Long parentId,
            @Param("name") String name,
            @Param("leaderUserId") Long leaderUserId,
            @Param("status") String status,
            @Param("sortOrder") int sortOrder);

    /**
     * 标记部门为已删除。
     *
     * @param departmentId 部门主键
     * @return 更新行数
     */
    @Update("""
            UPDATE sys_department
            SET status = 'DELETED',
                update_time = CURRENT_TIMESTAMP(6)
            WHERE id = #{departmentId}
            """)
    int markDepartmentDeleted(@Param("departmentId") long departmentId);

    /**
     * 查询有效子部门数量。
     *
     * @param departmentId 部门主键
     * @return 子部门数量
     */
    @Select("""
            SELECT COUNT(*)
            FROM sys_department
            WHERE parent_id = #{departmentId}
              AND status = 'ACTIVE'
            """)
    int countActiveDepartmentChildren(@Param("departmentId") long departmentId);

    /**
     * 查询用户部门关系。
     *
     * @return 关系行
     */
    @Select("""
            SELECT ud.user_id AS ownerId, ud.department_id AS targetId
            FROM sys_user_department ud
            JOIN sys_department d ON d.id = ud.department_id
            WHERE d.status = 'ACTIVE'
            ORDER BY ud.user_id, ud.department_id
            """)
    List<AssignmentRow> findUserDepartments();

    /**
     * 查询角色数据范围部门关系。
     *
     * @return 关系行
     */
    @Select("""
            SELECT rsd.role_id AS ownerId, rsd.department_id AS targetId
            FROM sys_role_data_scope_dept rsd
            JOIN sys_department d ON d.id = rsd.department_id
            WHERE d.status = 'ACTIVE'
            ORDER BY rsd.role_id, rsd.department_id
            """)
    List<AssignmentRow> findRoleDataScopeDepartments();

    /**
     * 替换用户所属部门。
     *
     * @param userId 用户主键
     * @param departmentIds 部门主键
     * @return 插入行数
     */
    @Insert("""
            <script>
            INSERT INTO sys_user_department (user_id, department_id)
            VALUES
            <foreach collection="departmentIds" item="departmentId" separator=",">
                (#{userId}, #{departmentId})
            </foreach>
            </script>
            """)
    int insertUserDepartments(
            @Param("userId") long userId,
            @Param("departmentIds") List<Long> departmentIds);

    /**
     * 删除用户已有部门关系。
     *
     * @param userId 用户主键
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_user_department WHERE user_id = #{userId}")
    int deleteUserDepartments(@Param("userId") long userId);

    /**
     * 更新角色数据范围。
     *
     * @param roleId 角色主键
     * @param dataScope 数据范围
     * @return 更新行数
     */
    @Update("""
            UPDATE sys_role
            SET data_scope = #{dataScope},
                update_time = CURRENT_TIMESTAMP(6)
            WHERE id = #{roleId}
            """)
    int updateRoleDataScope(
            @Param("roleId") long roleId,
            @Param("dataScope") String dataScope);

    /**
     * 删除角色自定义数据范围部门。
     *
     * @param roleId 角色主键
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_role_data_scope_dept WHERE role_id = #{roleId}")
    int deleteRoleDataScopeDepartments(@Param("roleId") long roleId);

    /**
     * 新增角色自定义数据范围部门。
     *
     * @param roleId 角色主键
     * @param departmentIds 部门主键
     * @return 插入行数
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_data_scope_dept (role_id, department_id)
            VALUES
            <foreach collection="departmentIds" item="departmentId" separator=",">
                (#{roleId}, #{departmentId})
            </foreach>
            </script>
            """)
    int insertRoleDataScopeDepartments(
            @Param("roleId") long roleId,
            @Param("departmentIds") List<Long> departmentIds);

    /**
     * 写入授权审计记录。
     *
     * @param log 审计记录
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO sys_auth_audit_log (
                operator_id, operator_name, target_type, target_id,
                action, before_value, after_value, client_ip
            )
            VALUES (
                #{operatorId}, #{operatorName}, #{targetType}, #{targetId},
                #{action}, #{beforeValue}, #{afterValue}, #{clientIp}
            )
            """)
    int insertAuditLog(RbacRows.AuditLogRow log);

    /**
     * 查询最近的授权审计记录。
     *
     * @param limit 数量上限
     * @return 审计记录
     */
    @Select("""
            SELECT id, operator_id AS operatorId, operator_name AS operatorName,
                   target_type AS targetType, target_id AS targetId, action,
                   before_value AS beforeValue, after_value AS afterValue,
                   client_ip AS clientIp, create_time AS createTime
            FROM sys_auth_audit_log
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<RbacRows.AuditLogRow> findAuditLogs(@Param("limit") int limit);
}

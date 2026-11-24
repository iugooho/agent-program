import axios from 'axios'

import { http } from '@/api/http'
import type {
  ApiResponseListRbacAuditLogResponse,
  ApiResponseRbacDepartmentResponse,
  ApiResponseRbacPermissionResponse,
  ApiResponseRbacOverviewResponse,
  ApiResponseRbacRoleResponse,
  ApiResponseVoid,
  DepartmentCreateRequest,
  DepartmentMembersRequest,
  DepartmentUpdateRequest,
  PermissionCreateRequest,
  RbacAuditLogResponse,
  RbacDepartmentResponse,
  RbacOverviewResponse,
  RbacPermissionResponse,
  RbacRoleResponse,
  RoleCopyRequest,
  RoleCreateRequest,
  RoleDataScopeRequest,
  RolePermissionsRequest,
  RoleStatusRequest,
  RoleUpdateRequest,
  UserDepartmentsRequest,
  UserRolesRequest
} from '@/api/schema'

/** RBAC 接口失败。 */
export class RbacApiError extends Error {
  constructor(
    readonly status: number,
    message: string
  ) {
    super(message)
    this.name = 'RbacApiError'
  }
}

/** 读取角色、权限和用户分配数据。 */
export async function fetchRbacOverview(): Promise<RbacOverviewResponse> {
  try {
    const response = await http.get<ApiResponseRbacOverviewResponse>('/v1/admin/rbac/overview')
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '权限数据加载失败')
  }
}

/** 读取最近的授权变更记录。 */
export async function fetchRbacAuditLogs(limit = 50): Promise<RbacAuditLogResponse[]> {
  try {
    const response = await http.get<ApiResponseListRbacAuditLogResponse>('/v1/admin/rbac/audit', {
      params: { limit }
    })
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '授权变更记录加载失败')
  }
}

/** 创建自定义角色。 */
export async function createRole(request: RoleCreateRequest): Promise<RbacRoleResponse> {
  try {
    const response = await http.post<ApiResponseRbacRoleResponse>('/v1/admin/rbac/roles', request)
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '角色创建失败')
  }
}

/** 复制角色。 */
export async function copyRole(
  roleId: number,
  request: RoleCopyRequest
): Promise<RbacRoleResponse> {
  try {
    const response = await http.post<ApiResponseRbacRoleResponse>(
      `/v1/admin/rbac/roles/${roleId}/copy`,
      request
    )
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '角色复制失败')
  }
}

/** 创建自定义权限点。 */
export async function createPermission(
  request: PermissionCreateRequest
): Promise<RbacPermissionResponse> {
  try {
    const response = await http.post<ApiResponseRbacPermissionResponse>(
      '/v1/admin/rbac/permissions',
      request
    )
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '权限创建失败')
  }
}

/** 删除自定义权限点。 */
export async function deletePermission(permissionId: number): Promise<void> {
  try {
    const response = await http.delete<ApiResponseVoid>(
      `/v1/admin/rbac/permissions/${permissionId}`
    )
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
  } catch (error: unknown) {
    throw readableError(error, '权限删除失败')
  }
}

/** 更新角色名称和说明。 */
export async function updateRole(
  roleId: number,
  request: RoleUpdateRequest
): Promise<RbacRoleResponse> {
  try {
    const response = await http.put<ApiResponseRbacRoleResponse>(
      `/v1/admin/rbac/roles/${roleId}`,
      request
    )
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '角色更新失败')
  }
}

/** 启用或停用角色。 */
export async function updateRoleStatus(
  roleId: number,
  status: 'ACTIVE' | 'DISABLED'
): Promise<RbacRoleResponse> {
  const request: RoleStatusRequest = { status }
  try {
    const response = await http.put<ApiResponseRbacRoleResponse>(
      `/v1/admin/rbac/roles/${roleId}/status`,
      request
    )
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '角色状态更新失败')
  }
}

/** 删除自定义角色。 */
export async function deleteRole(roleId: number): Promise<void> {
  try {
    const response = await http.delete<ApiResponseVoid>(`/v1/admin/rbac/roles/${roleId}`)
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
  } catch (error: unknown) {
    throw readableError(error, '角色删除失败')
  }
}

/** 更新角色权限。 */
export async function updateRolePermissions(
  roleId: number,
  permissionIds: number[],
  confirmHighRisk = false
): Promise<void> {
  const request: RolePermissionsRequest = { permissionIds, confirmHighRisk }
  try {
    const response = await http.put<ApiResponseVoid>(
      `/v1/admin/rbac/roles/${roleId}/permissions`,
      request
    )
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
  } catch (error: unknown) {
    throw readableError(error, '角色权限保存失败')
  }
}

/** 更新用户角色。 */
export async function updateUserRoles(userId: number, roleIds: number[]): Promise<void> {
  const request: UserRolesRequest = { roleIds }
  try {
    const response = await http.put<ApiResponseVoid>(
      `/v1/admin/rbac/users/${userId}/roles`,
      request
    )
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
  } catch (error: unknown) {
    throw readableError(error, '用户角色保存失败')
  }
}

/** 创建部门。 */
export async function createDepartment(
  request: DepartmentCreateRequest
): Promise<RbacDepartmentResponse> {
  try {
    const response = await http.post<ApiResponseRbacDepartmentResponse>(
      '/v1/admin/rbac/departments',
      request
    )
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '部门创建失败')
  }
}

/** 更新部门资料和启停状态。 */
export async function updateDepartment(
  departmentId: number,
  request: DepartmentUpdateRequest
): Promise<RbacDepartmentResponse> {
  try {
    const response = await http.put<ApiResponseRbacDepartmentResponse>(
      `/v1/admin/rbac/departments/${departmentId}`,
      request
    )
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '部门更新失败')
  }
}

/** 删除部门。 */
export async function deleteDepartment(departmentId: number): Promise<void> {
  try {
    const response = await http.delete<ApiResponseVoid>(
      `/v1/admin/rbac/departments/${departmentId}`
    )
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
  } catch (error: unknown) {
    throw readableError(error, '部门删除失败')
  }
}

/** 替换部门成员。 */
export async function updateDepartmentMembers(
  departmentId: number,
  userIds: number[]
): Promise<void> {
  const request: DepartmentMembersRequest = { userIds }
  try {
    const response = await http.put<ApiResponseVoid>(
      `/v1/admin/rbac/departments/${departmentId}/members`,
      request
    )
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
  } catch (error: unknown) {
    throw readableError(error, '部门成员保存失败')
  }
}

/** 更新角色数据范围。 */
export async function updateRoleDataScope(
  roleId: number,
  dataScope: string,
  departmentIds: number[]
): Promise<void> {
  const request: RoleDataScopeRequest = { dataScope, departmentIds }
  try {
    const response = await http.put<ApiResponseVoid>(
      `/v1/admin/rbac/roles/${roleId}/data-scope`,
      request
    )
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
  } catch (error: unknown) {
    throw readableError(error, '角色数据范围保存失败')
  }
}

/** 更新用户所属部门。 */
export async function updateUserDepartments(
  userId: number,
  departmentIds: number[]
): Promise<void> {
  const request: UserDepartmentsRequest = { departmentIds }
  try {
    const response = await http.put<ApiResponseVoid>(
      `/v1/admin/rbac/users/${userId}/departments`,
      request
    )
    if (response.data.code !== 0) {
      throw new RbacApiError(response.status, response.data.message)
    }
  } catch (error: unknown) {
    throw readableError(error, '用户所属部门保存失败')
  }
}

function readableError(error: unknown, fallback: string): Error {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return new RbacApiError(error.response?.status ?? 0, error.response?.data?.message ?? fallback)
  }
  return error instanceof Error ? error : new Error(fallback)
}

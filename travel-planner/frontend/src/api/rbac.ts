import axios from 'axios'

import { http } from '@/api/http'
import type {
  ApiResponseRbacOverviewResponse,
  ApiResponseRbacRoleResponse,
  ApiResponseVoid,
  RbacOverviewResponse,
  RbacRoleResponse,
  RoleCreateRequest,
  RolePermissionsRequest,
  RoleStatusRequest,
  RoleUpdateRequest,
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
  permissionIds: number[]
): Promise<void> {
  const request: RolePermissionsRequest = { permissionIds }
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

function readableError(error: unknown, fallback: string): Error {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return new RbacApiError(error.response?.status ?? 0, error.response?.data?.message ?? fallback)
  }
  return error instanceof Error ? error : new Error(fallback)
}

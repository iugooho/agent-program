import axios from 'axios'

import { http } from '@/api/http'
import type { AdminAccessResponse, ApiResponseAdminAccessResponse } from '@/api/schema'

/** 管理员接口拒绝访问。 */
export class AdminAccessError extends Error {
  constructor(
    readonly status: number,
    message: string
  ) {
    super(message)
    this.name = 'AdminAccessError'
  }
}

/** 调用管理员接口，验证当前 JWT 的 RBAC 权限。 */
export async function checkAdminAccess(): Promise<AdminAccessResponse> {
  try {
    const response = await http.get<ApiResponseAdminAccessResponse>('/v1/admin/access-check')
    if (response.data.code !== 0) {
      throw new AdminAccessError(response.status, response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    if (axios.isAxiosError<{ message?: string }>(error)) {
      throw new AdminAccessError(
        error.response?.status ?? 0,
        error.response?.data?.message ?? '管理员权限校验失败'
      )
    }
    throw error instanceof Error ? error : new Error('管理员权限校验失败')
  }
}

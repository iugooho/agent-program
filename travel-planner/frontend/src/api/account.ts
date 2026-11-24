import axios from 'axios'

import { http } from '@/api/http'
import type {
  ApiResponseListSecurityEventResponse,
  ApiResponseListUserSessionResponse,
  ApiResponseNotificationPreferencesResponse,
  ApiResponseUserDataExportResponse,
  ApiResponseVoid,
  NotificationPreferencesRequest,
  NotificationPreferencesResponse,
  SecurityEventResponse,
  UserDataExportResponse,
  UserSessionResponse
} from '@/api/schema'

/** 查询当前用户有效登录会话。 */
export async function fetchSessions(): Promise<UserSessionResponse[]> {
  try {
    const response = await http.get<ApiResponseListUserSessionResponse>('/v1/users/me/sessions')
    return unwrap(response.data, '登录会话加载失败')
  } catch (error: unknown) {
    throw readableError(error, '登录会话加载失败')
  }
}

/** 撤销指定登录会话。 */
export async function revokeSession(sessionId: string): Promise<void> {
  try {
    const response = await http.delete<ApiResponseVoid>(
      `/v1/users/me/sessions/${encodeURIComponent(sessionId)}`
    )
    if (response.data.code !== 0) {
      throw new Error(response.data.message)
    }
  } catch (error: unknown) {
    throw readableError(error, '会话撤销失败')
  }
}

/** 查询最近账号安全事件。 */
export async function fetchSecurityEvents(): Promise<SecurityEventResponse[]> {
  try {
    const response = await http.get<ApiResponseListSecurityEventResponse>(
      '/v1/users/me/security-log'
    )
    return unwrap(response.data, '安全日志加载失败')
  } catch (error: unknown) {
    throw readableError(error, '安全日志加载失败')
  }
}

/** 查询通知偏好。 */
export async function fetchNotificationPreferences(): Promise<NotificationPreferencesResponse> {
  try {
    const response = await http.get<ApiResponseNotificationPreferencesResponse>(
      '/v1/users/me/notification-preferences'
    )
    return unwrap(response.data, '通知偏好加载失败')
  } catch (error: unknown) {
    throw readableError(error, '通知偏好加载失败')
  }
}

/** 更新通知偏好。 */
export async function updateNotificationPreferences(
  request: NotificationPreferencesRequest
): Promise<NotificationPreferencesResponse> {
  try {
    const response = await http.put<ApiResponseNotificationPreferencesResponse>(
      '/v1/users/me/notification-preferences',
      request
    )
    return unwrap(response.data, '通知偏好保存失败')
  } catch (error: unknown) {
    throw readableError(error, '通知偏好保存失败')
  }
}

/** 导出当前用户数据。 */
export async function exportUserData(): Promise<UserDataExportResponse> {
  try {
    const response = await http.get<ApiResponseUserDataExportResponse>('/v1/users/me/export')
    return unwrap(response.data, '数据导出失败')
  } catch (error: unknown) {
    throw readableError(error, '数据导出失败')
  }
}

/** 验证当前邮箱后注销账号。 */
export async function deleteAccount(verificationCode: string): Promise<void> {
  try {
    const response = await http.post<ApiResponseVoid>('/v1/users/me/deletion', {
      verificationCode
    })
    if (response.data.code !== 0) {
      throw new Error(response.data.message)
    }
  } catch (error: unknown) {
    throw readableError(error, '账号注销失败')
  }
}

function unwrap<T>(body: { code: number; message: string; data: T }, fallback: string): T {
  if (body.code !== 0) {
    throw new Error(body.message || fallback)
  }
  return body.data
}

function readableError(error: unknown, fallback: string): Error {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return new Error(error.response?.data?.message ?? fallback)
  }
  return error instanceof Error ? error : new Error(fallback)
}

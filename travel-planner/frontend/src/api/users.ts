import axios from 'axios'

import { http } from '@/api/http'
import type {
  ApiResponseUserPreferencesResponse,
  ApiResponseUserResponse,
  ProfileUpdateRequest,
  UserPreferencesRequest,
  UserPreferencesResponse,
  UserResponse
} from '@/api/schema'

/** 更新当前用户资料。 */
export async function updateCurrentUser(request: ProfileUpdateRequest): Promise<UserResponse> {
  try {
    const response = await http.put<ApiResponseUserResponse>('/v1/users/me', request)
    if (response.data.code !== 0) {
      throw new Error(response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '用户资料更新失败')
  }
}

/** 获取当前用户长期偏好。 */
export async function fetchPreferences(): Promise<UserPreferencesResponse> {
  try {
    const response = await http.get<ApiResponseUserPreferencesResponse>('/v1/users/me/preferences')
    if (response.data.code !== 0) {
      throw new Error(response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '偏好加载失败')
  }
}

/** 更新当前用户长期偏好。 */
export async function updatePreferences(
  request: UserPreferencesRequest
): Promise<UserPreferencesResponse> {
  try {
    const response = await http.put<ApiResponseUserPreferencesResponse>(
      '/v1/users/me/preferences',
      request
    )
    if (response.data.code !== 0) {
      throw new Error(response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '偏好保存失败')
  }
}

function readableError(error: unknown, fallback: string): Error {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return new Error(error.response?.data?.message ?? fallback)
  }
  return error instanceof Error ? error : new Error(fallback)
}

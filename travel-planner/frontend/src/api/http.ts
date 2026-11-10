import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'

import type { ApiResponseAuthTokenResponse } from '@/api/schema'
import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from '@/auth/tokenStorage'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api'

/** 统一的 axios 实例：组件不要直接调用 axios，一律通过 src/api 下的模块调用。 */
export const http: AxiosInstance = axios.create({
  baseURL,
  timeout: 15000
})

interface RetryableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean
}

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!axios.isAxiosError(error) || error.response?.status !== 401) {
      return Promise.reject(error)
    }

    const config = error.config as RetryableConfig | undefined
    const url = config?.url ?? ''
    if (
      !config ||
      config._retried ||
      url.includes('/v1/auth/login') ||
      url.includes('/v1/auth/refresh')
    ) {
      return Promise.reject(error)
    }

    const refreshToken = getRefreshToken()
    if (!refreshToken) {
      expireSession()
      return Promise.reject(error)
    }

    config._retried = true
    try {
      const refreshed = await axios.post<ApiResponseAuthTokenResponse>(
        `${baseURL}/v1/auth/refresh`,
        { refreshToken }
      )
      if (refreshed.data.code !== 0) {
        throw new Error(refreshed.data.message)
      }
      saveTokens(refreshed.data.data.accessToken, refreshed.data.data.refreshToken)
      config.headers.set('Authorization', `Bearer ${refreshed.data.data.accessToken}`)
      return http(config)
    } catch (refreshError: unknown) {
      expireSession()
      return Promise.reject(refreshError)
    }
  }
)

function expireSession(): void {
  clearTokens()
  window.dispatchEvent(new Event('travel-planner:auth-expired'))
}

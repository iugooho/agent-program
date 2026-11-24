import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'

import type { ApiResponseAuthTokenResponse } from '@/api/schema'
import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from '@/auth/tokenStorage'

/** 后端 API 前缀；独立部署时通过 VITE_API_BASE_URL 覆盖，不在页面里写死地址。 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

/** 统一的 axios 实例：组件不要直接调用 axios，一律通过 src/api 下的模块调用。 */
export const http: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000
})

interface RetryableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean
}

let refreshInFlight: Promise<string> | null = null

/**
 * 刷新访问令牌。多个并发 401 共用同一个刷新请求，避免刷新令牌被重复消费。
 */
export function refreshAccessToken(): Promise<string> {
  if (!refreshInFlight) {
    refreshInFlight = requestRefreshToken().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

/**
 * 带 Bearer 认证的 fetch。SSE 需要读取 ReadableStream，axios 的浏览器适配器做不到，
 * 因此这里复用同一套令牌与刷新逻辑，避免页面各自处理 401。
 */
export async function authorizedFetch(
  input: RequestInfo | URL,
  init: RequestInit = {}
): Promise<Response> {
  const headers = new Headers(init.headers)
  const token = getAccessToken()
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const response = await fetch(input, { ...init, headers })
  if (response.status !== 401) {
    return response
  }
  try {
    const refreshed = await refreshAccessToken()
    headers.set('Authorization', `Bearer ${refreshed}`)
    return await fetch(input, { ...init, headers })
  } catch {
    expireSession()
    return response
  }
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

    if (!getRefreshToken()) {
      expireSession()
      return Promise.reject(error)
    }

    config._retried = true
    try {
      const accessToken = await refreshAccessToken()
      config.headers.set('Authorization', `Bearer ${accessToken}`)
      return http(config)
    } catch (refreshError: unknown) {
      expireSession()
      return Promise.reject(refreshError)
    }
  }
)

async function requestRefreshToken(): Promise<string> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('登录状态已失效')
  }
  const refreshed = await axios.post<ApiResponseAuthTokenResponse>(
    `${API_BASE_URL}/v1/auth/refresh`,
    { refreshToken }
  )
  if (refreshed.data.code !== 0) {
    throw new Error(refreshed.data.message)
  }
  saveTokens(refreshed.data.data.accessToken, refreshed.data.data.refreshToken)
  return refreshed.data.data.accessToken
}

function expireSession(): void {
  clearTokens()
  window.dispatchEvent(new Event('travel-planner:auth-expired'))
}

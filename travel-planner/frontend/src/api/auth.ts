import axios from 'axios'

import type { CaptchaAnswer } from '@/api/captcha'
import { http } from '@/api/http'
import type {
  ApiResponseAuthTokenResponse,
  ApiResponseEmailCodeResponse,
  ApiResponseUserResponse,
  AuthTokenResponse,
  EmailCodeResponse,
  LoginRequest,
  RegisterRequest,
  UserResponse
} from '@/api/schema'

const EMAIL_CODE_TIMEOUT_MS = 45_000

interface ApiErrorBody {
  code?: number
  message?: string
}

/** 认证接口业务错误，保留 code 供页面判断 CAPTCHA 等分支。 */
export class AuthApiError extends Error {
  constructor(
    message: string,
    readonly code?: number,
    readonly status?: number
  ) {
    super(message)
    this.name = 'AuthApiError'
  }
}

function readableError(error: unknown, fallback: string): Error {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    return new AuthApiError(
      error.response?.data?.message ?? fallback,
      error.response?.data?.code,
      error.response?.status
    )
  }
  return error instanceof Error ? error : new Error(fallback)
}

/** 使用邮箱验证码注册并返回 JWT。 */
export async function register(request: RegisterRequest): Promise<AuthTokenResponse> {
  try {
    const response = await http.post<ApiResponseAuthTokenResponse>('/v1/auth/register', request)
    if (response.data.code !== 0) {
      throw new AuthApiError(response.data.message, response.data.code, response.status)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '注册失败')
  }
}

/** 登录并返回 JWT。 */
export async function login(request: LoginRequest): Promise<AuthTokenResponse> {
  try {
    const response = await http.post<ApiResponseAuthTokenResponse>('/v1/auth/login', request)
    if (response.data.code !== 0) {
      throw new AuthApiError(response.data.message, response.data.code, response.status)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '登录失败')
  }
}

/** 发送邮箱验证码。 */
export async function sendEmailCode(
  email: string,
  purpose: string,
  captcha: CaptchaAnswer
): Promise<EmailCodeResponse> {
  try {
    const response = await http.post<ApiResponseEmailCodeResponse>(
      '/v1/auth/email-code',
      {
        email,
        purpose,
        captchaChallengeId: captcha.challengeId,
        captchaAnswer: captcha.answer
      },
      { timeout: EMAIL_CODE_TIMEOUT_MS }
    )
    if (response.data.code !== 0) {
      throw new Error(response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    if (
      axios.isAxiosError(error) &&
      (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT')
    ) {
      throw new Error('发送结果尚未确认，请先检查邮箱，不要连续点击')
    }
    throw readableError(error, '验证码发送失败')
  }
}

/** 获取当前用户。 */
export async function fetchCurrentUser(): Promise<UserResponse> {
  try {
    const response = await http.get<ApiResponseUserResponse>('/v1/users/me')
    if (response.data.code !== 0) {
      throw new Error(response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw readableError(error, '无法获取当前用户')
  }
}

/** 退出当前会话。 */
export async function logout(): Promise<void> {
  try {
    await http.post('/v1/auth/logout')
  } catch (error: unknown) {
    throw readableError(error, '退出登录失败')
  }
}

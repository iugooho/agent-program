import axios from 'axios'

import { http } from '@/api/http'
import type { ApiResponseCaptchaChallengeResponse, CaptchaChallengeResponse } from '@/api/schema'

/** 随业务操作一起提交的 CAPTCHA 答案。 */
export interface CaptchaAnswer {
  challengeId: string
  answer: string
}

/** 获取图形人机验证挑战。 */
export async function fetchCaptchaChallenge(
  purpose: 'LOGIN' | 'EMAIL_CODE'
): Promise<CaptchaChallengeResponse> {
  try {
    const response = await http.post<ApiResponseCaptchaChallengeResponse>(
      '/v1/auth/captcha/challenge',
      { purpose }
    )
    if (response.data.code !== 0) {
      throw new Error(response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    if (axios.isAxiosError<{ message?: string }>(error)) {
      throw new Error(error.response?.data?.message ?? '无法加载人机验证')
    }
    throw error instanceof Error ? error : new Error('无法加载人机验证')
  }
}

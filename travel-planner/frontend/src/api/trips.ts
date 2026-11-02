import axios from 'axios'

import { http } from '@/api/http'
import type { ApiResponseTripPlanResponse, TripPlanResponse } from '@/api/schema'

/** 调用后端智能体生成旅行计划，并把失败响应转换成页面可展示的错误。 */
export async function planTrip(message: string): Promise<TripPlanResponse> {
  try {
    const response = await http.post<ApiResponseTripPlanResponse>('/v1/trips/plan', { message })
    if (response.data.code !== 0) {
      throw new Error(response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    if (axios.isAxiosError<ApiResponseTripPlanResponse>(error)) {
      throw new Error(error.response?.data?.message ?? '旅行规划服务暂时不可用')
    }
    throw error
  }
}

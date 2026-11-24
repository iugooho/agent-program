import axios from 'axios'

import { http } from '@/api/http'
import type {
  ApiResponseListTravelPlanSummaryResponse,
  ApiResponseSavedTravelPlanResponse,
  ApiResponseTripPlanResponse,
  ApiResponseVoid,
  SavedTravelPlanResponse,
  TravelPlanSummaryResponse,
  TripPlanResponse
} from '@/api/schema'

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

/** 查询当前用户数据范围内可见的旅行计划。 */
export async function fetchTripPlans(): Promise<TravelPlanSummaryResponse[]> {
  try {
    const response = await http.get<ApiResponseListTravelPlanSummaryResponse>('/v1/trips')
    if (response.data.code !== 0) {
      throw new Error(response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw tripRequestError(error, '旅行计划列表加载失败')
  }
}

/** 查询单条旅行计划详情。 */
export async function fetchTripPlan(planId: number): Promise<SavedTravelPlanResponse> {
  try {
    const response = await http.get<ApiResponseSavedTravelPlanResponse>(`/v1/trips/${planId}`)
    if (response.data.code !== 0) {
      throw new Error(response.data.message)
    }
    return response.data.data
  } catch (error: unknown) {
    throw tripRequestError(error, '旅行计划详情加载失败')
  }
}

/** 删除旅行计划。 */
export async function deleteTripPlan(planId: number): Promise<void> {
  try {
    const response = await http.delete<ApiResponseVoid>(`/v1/trips/${planId}`)
    if (response.data.code !== 0) {
      throw new Error(response.data.message)
    }
  } catch (error: unknown) {
    throw tripRequestError(error, '旅行计划删除失败')
  }
}

function tripRequestError(error: unknown, fallback: string): Error {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return new Error(error.response?.data?.message ?? fallback)
  }
  return error instanceof Error ? error : new Error(fallback)
}

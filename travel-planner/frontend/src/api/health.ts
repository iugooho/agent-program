import { http } from '@/api/http'
import type { ApiResponse } from '@/api/types'

export interface HealthInfo {
  service: string
  status: string
  time: string
}

/** 调用后端健康检查接口，用来确认前后端联调是否打通。 */
export async function fetchHealth(): Promise<ApiResponse<HealthInfo>> {
  const response = await http.get<ApiResponse<HealthInfo>>('/v1/health')
  return response.data
}

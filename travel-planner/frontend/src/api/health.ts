import { http } from '@/api/http'
import type { ApiResponseHealthResponse } from '@/api/schema'

/** 调用后端健康检查接口，用来确认前后端联调是否打通。 */
export async function fetchHealth(): Promise<ApiResponseHealthResponse> {
  const response = await http.get<ApiResponseHealthResponse>('/v1/health')
  return response.data
}

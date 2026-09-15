import { http } from '@/api/http'

/** 后端统一响应结构，与 Java 侧的 ApiResponse 一一对应。 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

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

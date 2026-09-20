import { http } from '@/api/http'
import type { ApiResponse } from '@/api/types'

export async function fetchHello(): Promise<string> {
  const response = await http.get<ApiResponse<string>>('/v1/demo/hello')
  return response.data.data
}

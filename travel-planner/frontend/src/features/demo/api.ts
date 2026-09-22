import { http } from '@/api/http'
import type { ApiResponseString } from '@/api/schema'

export async function fetchHello(): Promise<string> {
  const response = await http.get<ApiResponseString>('/v1/demo/hello')
  return response.data.data
}

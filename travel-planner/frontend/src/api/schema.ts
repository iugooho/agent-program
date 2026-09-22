/**
 * 本文件由 scripts/gen-api-types.mjs 从 docs/openapi.json 自动生成，请勿手动修改。
 * 重新生成：npm run gen:api
 */
export interface ApiResponseHealthResponse {
  code: number
  message: string
  data: HealthResponse
}

export interface ApiResponseString {
  code: number
  message: string
  data: string
}

export interface HealthResponse {
  service: string
  status: string
  time: string
}

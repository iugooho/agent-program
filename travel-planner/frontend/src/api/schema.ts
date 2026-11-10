/**
 * 本文件由 scripts/gen-api-types.mjs 从 docs/openapi.json 自动生成，请勿手动修改。
 * 重新生成：npm run gen:api
 */
export interface AdminAccessResponse {
  username: string
  role: string
  accessGranted: boolean
  message: string
}

export interface ApiResponseAdminAccessResponse {
  code: number
  message: string
  data: AdminAccessResponse
}

export interface ApiResponseAuthTokenResponse {
  code: number
  message: string
  data: AuthTokenResponse
}

export interface ApiResponseCaptchaChallengeResponse {
  code: number
  message: string
  data: CaptchaChallengeResponse
}

export interface ApiResponseEmailCodeResponse {
  code: number
  message: string
  data: EmailCodeResponse
}

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

export interface ApiResponseTripPlanResponse {
  code: number
  message: string
  data: TripPlanResponse
}

export interface ApiResponseUserResponse {
  code: number
  message: string
  data: UserResponse
}

export interface ApiResponseVoid {
  code: number
  message: string
  data: unknown
}

export interface AuthTokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface BudgetSummaryResponse {
  attractions: number
  food: number
  accommodationAndTransport: number
  total: number
}

export interface CaptchaChallengeRequest {
  purpose: string
}

export interface CaptchaChallengeResponse {
  challengeId: string
  imageDataUrl: string
  expiresIn: number
}

export interface DayPlanResponse {
  dayNumber: number
  date: string
  city: string
  items: ItineraryItemResponse[]
  notes: string[]
}

export interface EmailCodeRequest {
  email: string
  purpose: string
  captchaChallengeId?: string
  captchaAnswer?: string
}

export interface EmailCodeResponse {
  expiresIn: number
  retryAfterSeconds: number
}

export interface EvidenceResponse {
  source: string
  description: string
  fetchedAt: string
}

export interface HealthResponse {
  service: string
  status: string
  time: string
}

export interface ItineraryItemResponse {
  title: string
  type: string
  startTime: string
  endTime: string
  location: string
  estimatedCost: number
  notes: string
}

export interface ItineraryResponse {
  title: string
  totalBudget: number
  budgetSummary: BudgetSummaryResponse
  days: DayPlanResponse[]
  warnings: string[]
  evidences: EvidenceResponse[]
}

export interface LoginRequest {
  loginType: string
  username?: string
  password?: string
  email?: string
  emailCode?: string
  captchaChallengeId?: string
  captchaAnswer?: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
  emailCode: string
}

export interface TripPlanRequest {
  message: string
}

export interface TripPlanResponse {
  request: TripRequestResponse
  itinerary: ItineraryResponse
  mode: string
}

export interface TripRequestResponse {
  departureCity: string
  destinations: string[]
  startDate: string
  days: number
  travelers: number
  budget: number
  travelStyles: string[]
  transportPreference: string
  foodPreferences: string[]
  forbiddenItems: string[]
  accommodationPreferences: string[]
}

export interface UserResponse {
  id: number
  username: string
  email: string
  nickname: string
  phone: string
  role: string
  status: string
  createTime: string
}

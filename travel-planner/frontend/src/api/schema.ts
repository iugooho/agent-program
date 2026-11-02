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

export interface ApiResponseTripPlanResponse {
  code: number
  message: string
  data: TripPlanResponse
}

export interface BudgetSummaryResponse {
  attractions: number
  food: number
  accommodationAndTransport: number
  total: number
}

export interface DayPlanResponse {
  dayNumber: number
  date: string
  city: string
  items: ItineraryItemResponse[]
  notes: string[]
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

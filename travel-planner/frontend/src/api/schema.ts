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

export interface ApiResponseChatExchangeResponse {
  code: number
  message: string
  data: ChatExchangeResponse
}

export interface ApiResponseChatMessagePageResponse {
  code: number
  message: string
  data: ChatMessagePageResponse
}

export interface ApiResponseConversationListResponse {
  code: number
  message: string
  data: ConversationListResponse
}

export interface ApiResponseConversationResponse {
  code: number
  message: string
  data: ConversationResponse
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

export interface ApiResponseListRbacAuditLogResponse {
  code: number
  message: string
  data: RbacAuditLogResponse[]
}

export interface ApiResponseListTravelPlanSummaryResponse {
  code: number
  message: string
  data: TravelPlanSummaryResponse[]
}

export interface ApiResponseRbacDepartmentResponse {
  code: number
  message: string
  data: RbacDepartmentResponse
}

export interface ApiResponseRbacOverviewResponse {
  code: number
  message: string
  data: RbacOverviewResponse
}

export interface ApiResponseRbacPermissionResponse {
  code: number
  message: string
  data: RbacPermissionResponse
}

export interface ApiResponseRbacRoleResponse {
  code: number
  message: string
  data: RbacRoleResponse
}

export interface ApiResponseSavedTravelPlanResponse {
  code: number
  message: string
  data: SavedTravelPlanResponse
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

export interface ApiResponseUserPreferencesResponse {
  code: number
  message: string
  data: UserPreferencesResponse
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

export interface ChatExchangeResponse {
  conversation: ConversationResponse
  userMessage: ChatMessageResponse
  reply: ChatMessageResponse
}

export interface ChatMessagePageResponse {
  items: ChatMessageResponse[]
  hasMore: boolean
  nextBeforeId?: number
}

export interface ChatMessageResponse {
  id: number
  conversationId: number
  role: string
  content: string
  msgType: string
  status: string
  model?: string
  errorMessage?: string
  createTime: string
  updateTime: string
}

export interface ConversationCreateRequest {
  title?: string
}

export interface ConversationListResponse {
  page: number
  size: number
  total: number
  items: ConversationResponse[]
}

export interface ConversationResponse {
  id: number
  title: string
  status: string
  messageCount: number
  lastMessagePreview: string
  lastMessageTime?: string
  createTime: string
  updateTime: string
}

export interface DayPlanResponse {
  dayNumber: number
  date: string
  city: string
  items: ItineraryItemResponse[]
  notes: string[]
}

export interface DepartmentCreateRequest {
  parentId?: number
  code: string
  name: string
  leaderUserId?: number
  sortOrder?: number
}

export interface DepartmentMembersRequest {
  userIds: number[]
}

export interface DepartmentUpdateRequest {
  parentId?: number
  name: string
  leaderUserId?: number
  status?: string
  sortOrder?: number
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

export interface PermissionCreateRequest {
  code: string
  name: string
  module: string
  type: string
  parentId?: number
  routePath?: string
  riskLevel?: string
  description?: string
}

export interface ProfileUpdateRequest {
  nickname?: string
  phone?: string
  newUsername?: string
  newPassword?: string
  newEmail?: string
  verificationCode?: string
  newEmailCode?: string
}

export interface RbacAuditLogResponse {
  id: number
  operatorId?: number
  operatorName?: string
  targetType: string
  targetId?: number
  action: string
  beforeValue?: string
  afterValue?: string
  clientIp?: string
  createTime: string
}

export interface RbacDepartmentResponse {
  id: number
  parentId?: number
  code: string
  name: string
  leaderUserId?: number
  status: string
  sortOrder: number
  memberIds: number[]
}

export interface RbacOverviewResponse {
  roles: RbacRoleResponse[]
  permissionGroups: RbacPermissionGroupResponse[]
  permissionPackages: RbacPermissionPackageResponse[]
  permissionDependencies: RbacPermissionDependencyResponse[]
  departments: RbacDepartmentResponse[]
  users: RbacUserResponse[]
}

export interface RbacPermissionDependencyResponse {
  permissionId: number
  requiredPermissionId: number
}

export interface RbacPermissionGroupResponse {
  module: string
  permissions: RbacPermissionResponse[]
}

export interface RbacPermissionPackageResponse {
  id: number
  code: string
  name: string
  description?: string
  permissionIds: number[]
}

export interface RbacPermissionResponse {
  id: number
  parentId?: number
  code: string
  name: string
  module: string
  type: string
  routePath?: string
  description: string
  status: string
  riskLevel: string
  builtIn: boolean
}

export interface RbacRoleResponse {
  id: number
  code: string
  name: string
  description: string
  builtIn: boolean
  status: string
  userCount: number
  permissionIds: number[]
  dataScope: string
  dataScopeDepartmentIds: number[]
}

export interface RbacUserResponse {
  id: number
  username: string
  nickname: string
  email: string
  status: string
  roleIds: number[]
  roleCodes: string[]
  departmentIds: number[]
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

export interface RoleCopyRequest {
  code: string
  name: string
  description?: string
  copyPermissions?: boolean
  copyMembers?: boolean
}

export interface RoleCreateRequest {
  code: string
  name: string
  description?: string
  permissionIds?: number[]
}

export interface RoleDataScopeRequest {
  dataScope: string
  departmentIds: number[]
}

export interface RolePermissionsRequest {
  permissionIds: number[]
  confirmHighRisk?: boolean
}

export interface RoleStatusRequest {
  status: string
}

export interface RoleUpdateRequest {
  name: string
  description?: string
}

export interface SavedTravelPlanResponse {
  id: number
  ownerId: number
  departmentId?: number
  request: TripRequestResponse
  itinerary: ItineraryResponse
  mode: string
  createTime: string
}

export interface SendMessageRequest {
  content: string
}

export interface TravelPlanSummaryResponse {
  id: number
  ownerId: number
  departmentId?: number
  title: string
  destinations: string[]
  startDate: string
  days: number
  totalBudget: number
  mode: string
  createTime: string
}

export interface TripPlanRequest {
  message: string
}

export interface TripPlanResponse {
  request: TripRequestResponse
  itinerary: ItineraryResponse
  mode: string
  savedPlanId: number
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

export interface UserDepartmentsRequest {
  departmentIds: number[]
}

export interface UserPreferencesRequest {
  travelStyleDefault?: string
  foodHabit?: string
  forbiddenList?: string[]
  accommodationPreference?: string
  budgetLevel?: string
}

export interface UserPreferencesResponse {
  travelStyleDefault: string
  foodHabit: string
  forbiddenList: string[]
  accommodationPreference: string
  budgetLevel: string
  updateTime?: string
}

export interface UserResponse {
  id: number
  username: string
  email: string
  nickname: string
  phone: string
  role: string
  roles: string[]
  permissions: string[]
  status: string
  createTime: string
}

export interface UserRolesRequest {
  roleIds: number[]
}

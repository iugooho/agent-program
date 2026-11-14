import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  fetchCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
  sendEmailCode
} from '@/api/auth'
import type { CaptchaAnswer } from '@/api/captcha'
import type { EmailCodeResponse, UserResponse } from '@/api/schema'
import { clearTokens, getAccessToken, saveTokens } from '@/auth/tokenStorage'

/** 登录用户和令牌状态。 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(getAccessToken())
  const user = ref<UserResponse | null>(null)
  const authenticated = computed(() => Boolean(accessToken.value))
  const roles = computed(() => user.value?.roles ?? [])
  const permissions = computed(() => user.value?.permissions ?? [])

  async function register(
    username: string,
    password: string,
    email: string,
    emailCode: string
  ): Promise<void> {
    const token = await registerRequest({ username, password, email, emailCode })
    applyToken(token.accessToken, token.refreshToken)
    await loadUser()
  }

  async function loginWithPassword(
    username: string,
    password: string,
    captcha?: CaptchaAnswer
  ): Promise<void> {
    const token = await loginRequest({
      loginType: 'PASSWORD',
      username,
      password,
      captchaChallengeId: captcha?.challengeId,
      captchaAnswer: captcha?.answer
    })
    applyToken(token.accessToken, token.refreshToken)
    await loadUser()
  }

  async function loginWithEmailCode(email: string, emailCode: string): Promise<void> {
    const token = await loginRequest({ loginType: 'EMAIL_CODE', email, emailCode })
    applyToken(token.accessToken, token.refreshToken)
    await loadUser()
  }

  async function sendLoginCodeWithCaptcha(
    email: string,
    captcha: CaptchaAnswer
  ): Promise<EmailCodeResponse> {
    return sendEmailCode(email, 'LOGIN', captcha)
  }

  async function sendRegisterCode(
    email: string,
    captcha: CaptchaAnswer
  ): Promise<EmailCodeResponse> {
    return sendEmailCode(email, 'REGISTER', captcha)
  }

  async function loadUser(): Promise<void> {
    user.value = await fetchCurrentUser()
  }

  async function restore(): Promise<void> {
    accessToken.value = getAccessToken()
    if (!accessToken.value) {
      user.value = null
      return
    }
    try {
      await loadUser()
    } catch {
      clearSession()
    }
  }

  async function logout(): Promise<void> {
    try {
      if (accessToken.value) {
        await logoutRequest()
      }
    } finally {
      clearSession()
    }
  }

  function applyToken(access: string, refresh: string): void {
    saveTokens(access, refresh)
    accessToken.value = access
  }

  function clearSession(): void {
    clearTokens()
    accessToken.value = null
    user.value = null
  }

  function can(permissionCode: string): boolean {
    return permissions.value.includes(permissionCode)
  }

  function hasRole(roleCode: string): boolean {
    return roles.value.includes(roleCode)
  }

  return {
    user,
    authenticated,
    roles,
    permissions,
    register,
    loginWithPassword,
    loginWithEmailCode,
    sendLoginCodeWithCaptcha,
    sendRegisterCode,
    restore,
    logout,
    clearSession,
    can,
    hasRole
  }
})

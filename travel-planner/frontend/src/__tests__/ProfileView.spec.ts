import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

import { fetchPreferences, fetchProfileOverview } from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import ProfileView from '@/views/ProfileView.vue'

vi.mock('@/api/users', () => ({
  fetchProfileOverview: vi.fn(),
  fetchPreferences: vi.fn(),
  updateCurrentUser: vi.fn(),
  updatePreferences: vi.fn(),
  uploadAvatar: vi.fn()
}))

vi.mock('@/api/account', () => ({
  fetchSessions: vi.fn().mockResolvedValue([
    {
      sessionId: 'current-session',
      clientIp: '127.0.0.1',
      userAgent: 'Chrome',
      createTime: '2026-11-24T08:30:00',
      lastSeenTime: '2026-11-24T09:00:00',
      expiresAt: '2026-12-01T08:30:00',
      current: true
    }
  ]),
  fetchSecurityEvents: vi.fn().mockResolvedValue([
    {
      id: 1,
      eventType: 'LOGIN',
      description: '账号登录成功',
      clientIp: '127.0.0.1',
      userAgent: 'Chrome',
      createTime: '2026-11-24T08:30:00'
    }
  ]),
  fetchNotificationPreferences: vi.fn().mockResolvedValue({
    tripReminder: true,
    tripChange: true,
    systemMessage: true,
    emailEnabled: true
  }),
  updateNotificationPreferences: vi.fn(),
  exportUserData: vi.fn(),
  deleteAccount: vi.fn(),
  revokeSession: vi.fn()
}))

const mockedFetchProfileOverview = vi.mocked(fetchProfileOverview)
const mockedFetchPreferences = vi.mocked(fetchPreferences)

describe('ProfileView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockedFetchProfileOverview.mockReset()
    mockedFetchPreferences.mockReset()
    const auth = useAuthStore()
    auth.user = {
      id: 1,
      username: 'demo',
      email: 'demo@example.com',
      nickname: '演示用户',
      phone: '',
      avatarUrl: '/api/v1/users/avatars/1-preview.png',
      bio: '喜欢慢节奏旅行',
      role: 'USER',
      roles: ['USER'],
      permissions: ['user.profile.read', 'user.profile.update'],
      status: 'ACTIVE',
      createTime: '2026-11-10T00:00:00',
      lastLoginTime: '2026-11-24T08:30:00'
    }
    mockedFetchPreferences.mockResolvedValue({
      travelStyleDefault: 'RELAXED',
      foodHabit: '清淡',
      forbiddenList: ['不购物'],
      accommodationPreference: '近地铁',
      budgetLevel: 'COMFORT',
      updateTime: '2026-11-10T00:00:00'
    })
    mockedFetchProfileOverview.mockResolvedValue({
      totalTrips: 1,
      upcomingTrips: 1,
      cities: 1,
      totalDays: 4,
      nextTrip: {
        id: 12,
        ownerId: 1,
        departmentId: 2,
        title: '杭州轻松四日游',
        destinations: ['杭州'],
        startDate: '2026-12-01',
        days: 4,
        totalBudget: 8000,
        mode: 'AI',
        createTime: '2026-11-20T10:00:00'
      },
      recentTrips: [
        {
          id: 12,
          ownerId: 1,
          departmentId: 2,
          title: '杭州轻松四日游',
          destinations: ['杭州'],
          startDate: '2026-12-01',
          days: 4,
          totalBudget: 8000,
          mode: 'AI',
          createTime: '2026-11-20T10:00:00'
        }
      ]
    })
  })

  it('展示个人概览，并可切换资料偏好与安全设置', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/profile', component: ProfileView },
        { path: '/trips', component: { template: '<div />' } }
      ]
    })
    await router.push('/profile')
    await router.isReady()

    const wrapper = mount(
      { template: '<RouterView />' },
      { global: { plugins: [router], stubs: { teleport: true } } }
    )
    await flushPromises()

    expect(wrapper.text()).toContain('个人中心')
    expect(wrapper.text()).toContain('累计行程')
    expect(wrapper.text()).toContain('杭州轻松四日游')
    expect(wrapper.text()).toContain('demo@example.com')
    expect(wrapper.text()).toContain('喜欢慢节奏旅行')
    expect(wrapper.text()).not.toContain('其他部门的行程')

    const editButton = wrapper.findAll('button').find((button) => button.text() === '编辑资料')
    await editButton?.trigger('click')
    expect(wrapper.text()).toContain('编辑个人资料')
    expect(wrapper.text()).toContain('个人简介')

    const closeDialog = wrapper.find('button[aria-label="关闭编辑资料弹窗"]')
    await closeDialog.trigger('click')

    const tabs = wrapper.findAll('[role="tab"]')
    expect(tabs).toHaveLength(3)
    await tabs[1].trigger('click')
    expect(wrapper.text()).toContain('长期偏好')
    expect(wrapper.text()).toContain('轻松休闲')
    expect(wrapper.text()).toContain('不购物')

    await tabs[2].trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('账号凭证')
    expect(wrapper.text()).toContain('活跃会话')
    expect(wrapper.text()).toContain('消息提醒')
    expect(wrapper.text()).toContain('导出账号数据')

    const credentialsButton = wrapper
      .findAll('button')
      .find((button) => button.text() === '修改账号与密码')
    await credentialsButton?.trigger('click')
    expect(wrapper.text()).toContain('修改账号与密码')
    expect(wrapper.text()).toContain('验证当前邮箱')
  })
})

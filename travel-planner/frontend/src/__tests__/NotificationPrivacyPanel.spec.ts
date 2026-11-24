import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

import { fetchNotificationPreferences, updateNotificationPreferences } from '@/api/account'
import NotificationPrivacyPanel from '@/components/profile/NotificationPrivacyPanel.vue'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/account', () => ({
  fetchNotificationPreferences: vi.fn(),
  updateNotificationPreferences: vi.fn(),
  exportUserData: vi.fn(),
  deleteAccount: vi.fn()
}))

vi.mock('@/api/auth', () => ({
  sendEmailCode: vi.fn()
}))

const mockedFetchPreferences = vi.mocked(fetchNotificationPreferences)
const mockedUpdatePreferences = vi.mocked(updateNotificationPreferences)

describe('NotificationPrivacyPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const auth = useAuthStore()
    auth.user = {
      id: 1,
      username: 'demo',
      email: 'demo@example.com',
      nickname: '演示用户',
      phone: '',
      avatarUrl: '',
      bio: '',
      role: 'USER',
      roles: ['USER'],
      permissions: ['user.profile.read', 'user.profile.update'],
      status: 'ACTIVE',
      createTime: '2026-11-10T00:00:00',
      lastLoginTime: '2026-11-24T08:30:00'
    }
    mockedFetchPreferences.mockReset()
    mockedUpdatePreferences.mockReset()
    mockedFetchPreferences.mockResolvedValue({
      tripReminder: true,
      tripChange: true,
      systemMessage: true,
      emailEnabled: true
    })
    mockedUpdatePreferences.mockResolvedValue({
      tripReminder: false,
      tripChange: true,
      systemMessage: false,
      emailEnabled: false
    })
  })

  it('读取并保存通知偏好，同时展示数据和注销入口', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/profile', component: NotificationPrivacyPanel }]
    })
    await router.push('/profile')
    await router.isReady()
    const wrapper = mount(NotificationPrivacyPanel, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('消息提醒')
    expect(wrapper.text()).toContain('导出账号数据')
    expect(wrapper.text()).toContain('开始注销流程')

    const switches = wrapper.findAll('input[role="switch"]')
    await switches[0].setValue(false)
    await switches[2].setValue(false)
    await switches[3].setValue(false)
    const saveButton = wrapper.findAll('button').find((button) => button.text() === '保存通知偏好')
    await saveButton?.trigger('click')
    await flushPromises()

    expect(mockedUpdatePreferences).toHaveBeenCalledWith({
      tripReminder: false,
      tripChange: true,
      systemMessage: false,
      emailEnabled: false
    })
  })
})

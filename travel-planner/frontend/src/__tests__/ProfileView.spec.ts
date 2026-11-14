import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchPreferences } from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import ProfileView from '@/views/ProfileView.vue'

vi.mock('@/api/users', () => ({
  fetchPreferences: vi.fn(),
  updateCurrentUser: vi.fn(),
  updatePreferences: vi.fn()
}))

const mockedFetchPreferences = vi.mocked(fetchPreferences)

describe('ProfileView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const auth = useAuthStore()
    auth.user = {
      id: 1,
      username: 'demo',
      email: 'demo@example.com',
      nickname: '演示用户',
      phone: '',
      role: 'USER',
      roles: ['USER'],
      permissions: ['user.profile.read', 'user.profile.update'],
      status: 'ACTIVE',
      createTime: '2026-11-10T00:00:00'
    }
    mockedFetchPreferences.mockResolvedValue({
      travelStyleDefault: 'RELAXED',
      foodHabit: '清淡',
      forbiddenList: ['不购物'],
      accommodationPreference: '近地铁',
      budgetLevel: 'COMFORT',
      updateTime: '2026-11-10T00:00:00'
    })
  })

  it('展示个人资料和长期偏好', async () => {
    const wrapper = mount(ProfileView)
    await flushPromises()

    expect(wrapper.text()).toContain('个人中心')
    expect(wrapper.text()).toContain('长期旅行偏好')
    expect(wrapper.text()).toContain('demo@example.com')
  })
})

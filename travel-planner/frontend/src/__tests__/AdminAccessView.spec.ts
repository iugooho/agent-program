import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { AdminAccessError, checkAdminAccess } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import AdminAccessView from '@/views/AdminAccessView.vue'

vi.mock('@/api/admin', () => {
  class MockAdminAccessError extends Error {
    constructor(
      readonly status: number,
      message: string
    ) {
      super(message)
    }
  }

  return {
    AdminAccessError: MockAdminAccessError,
    checkAdminAccess: vi.fn()
  }
})

const mockedCheckAdminAccess = vi.mocked(checkAdminAccess)

describe('AdminAccessView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const auth = useAuthStore()
    auth.user = {
      id: 1,
      username: 'admin',
      email: 'admin@example.com',
      nickname: '本地管理员',
      phone: '',
      role: 'ADMIN',
      status: 'ACTIVE',
      createTime: '2026-11-10T00:00:00'
    }
  })

  it('管理员访问成功后展示通过状态', async () => {
    mockedCheckAdminAccess.mockResolvedValue({
      username: 'admin',
      role: 'ADMIN',
      accessGranted: true,
      message: '管理员权限校验通过'
    })

    const wrapper = mount(AdminAccessView)
    await flushPromises()

    expect(wrapper.text()).toContain('访问通过')
    expect(wrapper.text()).toContain('管理员接口访问成功')
  })

  it('普通用户收到 403 时展示拒绝状态', async () => {
    const auth = useAuthStore()
    auth.user = { ...auth.user!, username: 'demo', role: 'USER' }
    mockedCheckAdminAccess.mockRejectedValue(new AdminAccessError(403, '无访问权限'))

    const wrapper = mount(AdminAccessView)
    await flushPromises()

    expect(wrapper.text()).toContain('访问被拒绝')
    expect(wrapper.text()).toContain('403')
  })
})

import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { updateCurrentUser } from '@/api/users'
import AccountCredentialsDialog from '@/components/profile/AccountCredentialsDialog.vue'

vi.mock('@/api/auth', () => ({
  sendEmailCode: vi.fn()
}))

vi.mock('@/api/users', () => ({
  updateCurrentUser: vi.fn()
}))

const mockedUpdateCurrentUser = vi.mocked(updateCurrentUser)

describe('AccountCredentialsDialog', () => {
  beforeEach(() => {
    mockedUpdateCurrentUser.mockReset()
  })

  it('验证后更新账号安全资料', async () => {
    const user = {
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
    mockedUpdateCurrentUser.mockResolvedValue({
      ...user,
      username: 'new_demo'
    })
    const wrapper = mount(AccountCredentialsDialog, {
      props: { open: true, user },
      global: {
        stubs: {
          teleport: true,
          CaptchaChallenge: true
        }
      }
    })
    await flushPromises()

    await wrapper.find('#credentials-username').setValue('new_demo')
    await wrapper.find('#credentials-password').setValue('Password123')
    await wrapper.find('#credentials-confirm').setValue('Password123')
    await wrapper.find('#credentials-current-code').setValue('123456')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedUpdateCurrentUser).toHaveBeenCalledWith({
      newUsername: 'new_demo',
      newPassword: 'Password123',
      verificationCode: '123456'
    })
    expect(wrapper.emitted('saved')?.[0]?.[0]).toMatchObject({
      username: 'new_demo'
    })
    expect(wrapper.emitted('update:open')?.[0]).toEqual([false])
  })
})

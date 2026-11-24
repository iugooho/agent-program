import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { updateCurrentUser, uploadAvatar } from '@/api/users'
import ProfileEditDialog from '@/components/profile/ProfileEditDialog.vue'

vi.mock('@/api/users', () => ({
  updateCurrentUser: vi.fn(),
  uploadAvatar: vi.fn()
}))

const mockedUpdateCurrentUser = vi.mocked(updateCurrentUser)
const mockedUploadAvatar = vi.mocked(uploadAvatar)

describe('ProfileEditDialog', () => {
  beforeEach(() => {
    mockedUpdateCurrentUser.mockReset()
    mockedUploadAvatar.mockReset()
  })

  it('编辑并保存基础资料', async () => {
    const user = {
      id: 1,
      username: 'demo',
      email: 'demo@example.com',
      nickname: '演示用户',
      phone: '13800000000',
      avatarUrl: '',
      bio: '喜欢慢节奏旅行',
      role: 'USER',
      roles: ['USER'],
      permissions: ['user.profile.read', 'user.profile.update'],
      status: 'ACTIVE',
      createTime: '2026-11-10T00:00:00',
      lastLoginTime: '2026-11-24T08:30:00'
    }
    mockedUpdateCurrentUser.mockResolvedValue({
      ...user,
      nickname: '新的昵称',
      bio: '新的简介'
    })
    const wrapper = mount(ProfileEditDialog, {
      props: { open: true, user },
      global: { stubs: { teleport: true } }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('编辑个人资料')
    await wrapper.find('#dialog-nickname').setValue('新的昵称')
    await wrapper.find('#dialog-bio').setValue('新的简介')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedUpdateCurrentUser).toHaveBeenCalledWith({
      nickname: '新的昵称',
      phone: '13800000000',
      bio: '新的简介'
    })
    expect(wrapper.emitted('saved')?.[0]?.[0]).toMatchObject({
      nickname: '新的昵称',
      bio: '新的简介'
    })
    expect(wrapper.emitted('update:open')?.[0]).toEqual([false])
  })
})

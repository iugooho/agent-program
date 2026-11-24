import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchSecurityEvents, fetchSessions, revokeSession } from '@/api/account'
import AccountSecurityPanel from '@/components/profile/AccountSecurityPanel.vue'

vi.mock('@/api/account', () => ({
  fetchSessions: vi.fn(),
  fetchSecurityEvents: vi.fn(),
  revokeSession: vi.fn()
}))

const mockedFetchSessions = vi.mocked(fetchSessions)
const mockedFetchSecurityEvents = vi.mocked(fetchSecurityEvents)
const mockedRevokeSession = vi.mocked(revokeSession)

describe('AccountSecurityPanel', () => {
  beforeEach(() => {
    mockedFetchSessions.mockReset()
    mockedFetchSecurityEvents.mockReset()
    mockedRevokeSession.mockReset()
    mockedFetchSessions.mockResolvedValue([
      {
        sessionId: 'current',
        clientIp: '127.0.0.1',
        userAgent: 'Chrome',
        createTime: '2026-11-24T08:00:00',
        lastSeenTime: '2026-11-24T09:00:00',
        expiresAt: '2026-12-01T08:00:00',
        current: true
      },
      {
        sessionId: 'other',
        clientIp: '192.168.1.2',
        userAgent: 'Firefox',
        createTime: '2026-11-23T08:00:00',
        lastSeenTime: '2026-11-23T09:00:00',
        expiresAt: '2026-11-30T08:00:00',
        current: false
      }
    ])
    mockedFetchSecurityEvents.mockResolvedValue([
      {
        id: 1,
        eventType: 'LOGIN',
        description: '账号登录成功',
        clientIp: '127.0.0.1',
        userAgent: 'Chrome',
        createTime: '2026-11-24T08:00:00'
      }
    ])
  })

  it('展示活跃会话和安全日志，并允许撤销其他设备', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const wrapper = mount(AccountSecurityPanel)
    await flushPromises()

    expect(wrapper.text()).toContain('Chrome')
    expect(wrapper.text()).toContain('当前设备')
    expect(wrapper.text()).toContain('Firefox')
    expect(wrapper.text()).toContain('最近安全日志')

    const revokeButton = wrapper.findAll('button').find((button) => button.text() === '撤销')
    await revokeButton?.trigger('click')
    await flushPromises()
    expect(mockedRevokeSession).toHaveBeenCalledWith('other')
  })
})

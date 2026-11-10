import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchCaptchaChallenge } from '@/api/captcha'
import CaptchaChallenge from '@/components/CaptchaChallenge.vue'

vi.mock('@/api/captcha', () => ({
  fetchCaptchaChallenge: vi.fn()
}))

const mockedFetchCaptchaChallenge = vi.mocked(fetchCaptchaChallenge)

describe('CaptchaChallenge', () => {
  beforeEach(() => {
    mockedFetchCaptchaChallenge.mockResolvedValue({
      challengeId: 'challenge-1',
      imageDataUrl: 'data:image/svg+xml;base64,PHN2Zy8+',
      expiresIn: 120
    })
  })

  it('输入答案后把挑战和答案一起交给父组件', async () => {
    const wrapper = mount(CaptchaChallenge, {
      props: {
        purpose: 'EMAIL_CODE'
      }
    })
    await flushPromises()

    await wrapper.get('input').setValue('ABCDE')
    await flushPromises()

    expect(mockedFetchCaptchaChallenge).toHaveBeenCalledWith('EMAIL_CODE')
    expect(wrapper.emitted('changed')?.at(-1)).toEqual([
      { challengeId: 'challenge-1', answer: 'ABCDE' }
    ])
  })
})

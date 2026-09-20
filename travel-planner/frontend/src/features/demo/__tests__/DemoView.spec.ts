import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import DemoView from '../views/DemoView.vue'

const { fetchHello } = vi.hoisted(() => ({
  fetchHello: vi.fn()
}))

vi.mock('../api', () => ({
  fetchHello
}))

describe('DemoView', () => {
  beforeEach(() => {
    fetchHello.mockReset()
    fetchHello.mockResolvedValue('Hello, world!')
  })

  it('loads and renders the backend greeting', async () => {
    const wrapper = mount(DemoView)

    await flushPromises()

    expect(fetchHello).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('Hello, world!')
  })
})

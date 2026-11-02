import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import { planTrip } from '@/api/trips'
import TripPlannerView from '@/views/TripPlannerView.vue'

vi.mock('@/api/trips', () => ({
  planTrip: vi.fn()
}))

const mockedPlanTrip = vi.mocked(planTrip)

describe('TripPlannerView', () => {
  it('提交需求后展示结构化行程', async () => {
    mockedPlanTrip.mockResolvedValue({
      mode: 'deepseek+java',
      request: {
        departureCity: '北京',
        destinations: ['杭州'],
        startDate: '2026-11-03',
        days: 1,
        travelers: 2,
        budget: 8000,
        travelStyles: ['休闲'],
        transportPreference: '高铁',
        foodPreferences: [],
        forbiddenItems: ['购物'],
        accommodationPreferences: ['交通便利']
      },
      itinerary: {
        title: '杭州 1 日旅行计划',
        totalBudget: 1320,
        budgetSummary: {
          attractions: 160,
          food: 360,
          accommodationAndTransport: 800,
          total: 1320
        },
        days: [
          {
            dayNumber: 1,
            date: '2026-11-03',
            city: '杭州',
            notes: ['晴'],
            items: [
              {
                title: '西湖',
                type: '风景名胜',
                startTime: '09:00:00',
                endTime: '11:30:00',
                location: '杭州市西湖区',
                estimatedCost: 80,
                notes: '预留交通时间'
              }
            ]
          }
        ],
        warnings: ['价格以实时查询为准'],
        evidences: [
          { source: '高德地图', description: '杭州景点 POI', fetchedAt: '2026-10-27T08:00:00Z' }
        ]
      }
    })

    const wrapper = mount(TripPlannerView)
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mockedPlanTrip).toHaveBeenCalledWith(expect.stringContaining('杭州'))
    expect(wrapper.text()).toContain('杭州 1 日旅行计划')
    expect(wrapper.text()).toContain('西湖')
    expect(wrapper.text()).toContain('deepseek+java')
  })
})

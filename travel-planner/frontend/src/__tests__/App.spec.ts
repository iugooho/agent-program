import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'

import App from '@/App.vue'
import HomeView from '@/views/HomeView.vue'

// 选了 ECharts 时 App.vue 顶部会多一个指向 /dashboard 的导航，
// 这里放一个占位路由，避免单测里刷 "No match found for location" 警告。
const DashboardStub = { name: 'DashboardStub', render: () => null }

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/dashboard', component: DashboardStub }
  ]
})

describe('App', () => {
  it('渲染项目标题与首页内容', async () => {
    await router.push('/')
    await router.isReady()

    const wrapper = mount(App, { global: { plugins: [router] } })

    expect(wrapper.text()).toContain('travel-planner')
  })
})

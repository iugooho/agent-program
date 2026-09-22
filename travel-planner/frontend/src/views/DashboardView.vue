<script setup lang="ts">
import type { EChartsOption } from 'echarts'

import EChartsPanel from '@/components/EChartsPanel.vue'

// 示例数据，接入后端统计接口后替换成接口返回值
const option: EChartsOption = {
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 30, bottom: 30 },
  xAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  },
  yAxis: { type: 'value' },
  series: [{ name: '行程生成量', type: 'line', smooth: true, data: [12, 18, 15, 22, 30, 26, 34] }]
}
</script>

<template>
  <section>
    <h1>travel-planner 数据大屏</h1>
    <p>图表数据统一由后端统计接口提供，前端只负责展示。</p>
    <EChartsPanel title="近 7 天行程生成量" :option="option" />
  </section>
</template>

<script setup lang="ts">
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'

// 按需注册：只把用到的图表与组件打进产物，整包 import 'echarts' 会多带约 1 MB。
// 新增图表类型时在这里补 register，例如 BarChart、PieChart。
echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  title: string
  option: echarts.EChartsCoreOption
  height?: string
}>()

const container = ref<HTMLDivElement | null>(null)
const chart = shallowRef<echarts.ECharts | null>(null)

function resize() {
  chart.value?.resize()
}

onMounted(() => {
  if (!container.value) return
  chart.value = echarts.init(container.value)
  chart.value.setOption(props.option)
  window.addEventListener('resize', resize)
})

watch(
  () => props.option,
  (option) => chart.value?.setOption(option, true),
  { deep: true }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart.value?.dispose()
  chart.value = null
})
</script>

<template>
  <section class="chart-panel">
    <h2
      class="chart-panel__title"
      v-text="title"
    />
    <div
      ref="container"
      class="chart-panel__canvas"
      :style="{ height: height ?? '320px' }"
    />
  </section>
</template>

<style scoped>
.chart-panel {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.chart-panel__title {
  margin: 0 0 12px;
  font-size: 16px;
}

.chart-panel__canvas {
  width: 100%;
}
</style>

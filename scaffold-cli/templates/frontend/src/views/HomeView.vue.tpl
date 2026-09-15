<script setup lang="ts">
const hints: string[] = [
  '路由定义：src/router/index.ts',
  '接口调用统一封装在 src/api 下（不要直接调 axios）',
  '接口代理：/api -> http://localhost:8080',
  '单元测试：npm run test',
  '代码校验：npm run lint',
  '生产构建：npm run build'
]
</script>

<template>
  <section>
    <h1>{{projectName}}</h1>
    <p>这是脚手架生成的前端骨架，把这里替换成你的业务页面即可。</p>
    <ul>
      <li
        v-for="hint in hints"
        :key="hint"
        v-text="hint"
      />
    </ul>
  </section>
</template>

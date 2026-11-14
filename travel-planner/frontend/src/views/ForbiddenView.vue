<template>
  <section class="forbidden">
    <span>403</span>
    <h1>没有访问权限</h1>
    <p>当前账号没有进入该页面的权限，请联系超级管理员分配角色。</p>
    <RouterLink to="/">返回首页</RouterLink>
  </section>
</template>

<style scoped>
.forbidden {
  display: grid;
  justify-items: center;
  gap: 12px;
  max-width: 560px;
  margin: 80px auto;
  padding: 48px;
  border: 1px solid #e2e8f0;
  background: #fff;
  text-align: center;
}

.forbidden span {
  color: #dc2626;
  font-size: 64px;
  font-weight: 800;
}

.forbidden h1,
.forbidden p {
  margin: 0;
}

.forbidden p {
  color: #64748b;
  line-height: 1.7;
}
</style>

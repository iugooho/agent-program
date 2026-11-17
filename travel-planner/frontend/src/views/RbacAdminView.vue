<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createRole as createRoleRequest,
  deleteRole as deleteRoleRequest,
  fetchRbacOverview,
  RbacApiError,
  updateRole as updateRoleRequest,
  updateRolePermissions,
  updateRoleStatus as updateRoleStatusRequest,
  updateUserRoles
} from '@/api/rbac'
import type {
  RbacOverviewResponse,
  RbacPermissionResponse,
  RbacRoleResponse,
  RbacUserResponse
} from '@/api/schema'

const overview = ref<RbacOverviewResponse | null>(null)
const selectedRoleId = ref<number | null>(null)
const selectedUserId = ref<number | null>(null)
const draftPermissionIds = ref<number[]>([])
const draftRoleIds = ref<number[]>([])
const loading = ref(true)
const saving = ref('')
const message = ref('')
const errorMessage = ref('')
const roleSearch = ref('')
const userSearch = ref('')
const roleDialogMode = ref<'create' | 'edit' | null>(null)
const savingRoleDefinition = ref(false)
const roleDefinitionError = ref('')

const roleDefinitionForm = reactive({
  code: '',
  name: '',
  description: ''
})

const selectedRole = computed(
  () => overview.value?.roles.find((role) => role.id === selectedRoleId.value) ?? null
)
const selectedUser = computed(
  () => overview.value?.users.find((user) => user.id === selectedUserId.value) ?? null
)
const allPermissions = computed(() => {
  if (!overview.value) {
    return []
  }
  return overview.value.permissionGroups.flatMap((group) => group.permissions)
})
const isSuperAdminRole = computed(() => selectedRole.value?.code === 'SUPER_ADMIN')
const filteredRoles = computed(() => {
  const keyword = roleSearch.value.trim().toLowerCase()
  if (!keyword) {
    return overview.value?.roles ?? []
  }
  return (overview.value?.roles ?? []).filter(
    (role) => role.name.toLowerCase().includes(keyword) || role.code.toLowerCase().includes(keyword)
  )
})
const filteredUsers = computed(() => {
  const keyword = userSearch.value.trim().toLowerCase()
  if (!keyword) {
    return overview.value?.users ?? []
  }
  return (overview.value?.users ?? []).filter(
    (user) =>
      user.username.toLowerCase().includes(keyword) ||
      user.nickname.toLowerCase().includes(keyword) ||
      user.email.toLowerCase().includes(keyword)
  )
})
const roleDirty = computed(() => {
  if (!selectedRole.value) {
    return false
  }
  return !sameIds(draftPermissionIds.value, selectedRole.value.permissionIds)
})
const userDirty = computed(() => {
  if (!selectedUser.value) {
    return false
  }
  return !sameIds(draftRoleIds.value, selectedUser.value.roleIds)
})
const selectedPermissionCount = computed(
  () =>
    allPermissions.value.filter((permission) => draftPermissionIds.value.includes(permission.id))
      .length
)
const adminUserCount = computed(
  () =>
    overview.value?.users.filter((user) =>
      user.roleCodes.some((code) => code === 'ADMIN' || code === 'SUPER_ADMIN')
    ).length ?? 0
)

const canEditSelectedRole = computed(
  () => selectedRole.value !== null && !selectedRole.value.builtIn
)

onMounted(() => {
  void loadOverview()
})

async function loadOverview(): Promise<void> {
  loading.value = true
  clearMessages()
  try {
    overview.value = await fetchRbacOverview()
    const role =
      overview.value.roles.find((item) => item.id === selectedRoleId.value) ??
      overview.value.roles.find((item) => item.code === 'USER') ??
      overview.value.roles[0]
    const user =
      overview.value.users.find((item) => item.id === selectedUserId.value) ??
      overview.value.users[0]
    if (role) {
      selectRole(role)
    }
    if (user) {
      selectUser(user)
    }
  } catch (error: unknown) {
    errorMessage.value = error instanceof RbacApiError ? error.message : '权限数据加载失败'
  } finally {
    loading.value = false
  }
}

function selectRole(role: RbacRoleResponse): void {
  selectedRoleId.value = role.id
  draftPermissionIds.value = [...role.permissionIds].sort((left, right) => left - right)
  clearMessages()
}

function selectUser(user: RbacUserResponse): void {
  selectedUserId.value = user.id
  draftRoleIds.value = [...user.roleIds].sort((left, right) => left - right)
  clearMessages()
}

function togglePermission(permission: RbacPermissionResponse, event: Event): void {
  const checked = (event.target as HTMLInputElement).checked
  const next = new Set(draftPermissionIds.value)
  if (checked) {
    next.add(permission.id)
  } else {
    next.delete(permission.id)
  }
  draftPermissionIds.value = [...next].sort((left, right) => left - right)
}

function toggleRole(role: RbacRoleResponse, event: Event): void {
  const checked = (event.target as HTMLInputElement).checked
  const next = new Set(draftRoleIds.value)
  if (checked) {
    next.add(role.id)
  } else {
    next.delete(role.id)
  }
  draftRoleIds.value = [...next].sort((left, right) => left - right)
}

function editableGroupPermissions(permissions: RbacPermissionResponse[]): RbacPermissionResponse[] {
  return permissions.filter((permission) => !permissionDisabled(permission))
}

function groupChecked(permissions: RbacPermissionResponse[]): boolean {
  if (isSuperAdminRole.value) {
    return true
  }
  const editable = editableGroupPermissions(permissions)
  return (
    editable.length > 0 &&
    editable.every((permission) => draftPermissionIds.value.includes(permission.id))
  )
}

function groupDisabled(permissions: RbacPermissionResponse[]): boolean {
  return isSuperAdminRole.value || editableGroupPermissions(permissions).length === 0
}

function toggleGroup(permissions: RbacPermissionResponse[], event: Event): void {
  const checked = (event.target as HTMLInputElement).checked
  const groupIds = editableGroupPermissions(permissions).map((permission) => permission.id)
  const next = new Set(draftPermissionIds.value)
  groupIds.forEach((permissionId) => {
    if (checked) {
      next.add(permissionId)
    } else {
      next.delete(permissionId)
    }
  })
  draftPermissionIds.value = [...next].sort((left, right) => left - right)
}

async function saveRolePermissions(): Promise<void> {
  if (!selectedRole.value || isSuperAdminRole.value) {
    return
  }
  saving.value = 'role'
  clearMessages()
  try {
    await updateRolePermissions(selectedRole.value.id, draftPermissionIds.value)
    await loadOverview()
    message.value = `已保存“${selectedRole.value.name}”的权限`
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '角色权限保存失败'
  } finally {
    saving.value = ''
  }
}

async function saveUserRoles(): Promise<void> {
  if (!selectedUser.value) {
    return
  }
  saving.value = 'user'
  clearMessages()
  try {
    await updateUserRoles(selectedUser.value.id, draftRoleIds.value)
    await loadOverview()
    message.value = `已保存“${selectedUser.value.username}”的角色`
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '用户角色保存失败'
  } finally {
    saving.value = ''
  }
}

function openCreateRole(): void {
  roleDialogMode.value = 'create'
  roleDefinitionError.value = ''
  roleDefinitionForm.code = ''
  roleDefinitionForm.name = ''
  roleDefinitionForm.description = ''
}

function openEditRole(): void {
  if (!selectedRole.value || selectedRole.value.builtIn) {
    return
  }
  roleDialogMode.value = 'edit'
  roleDefinitionError.value = ''
  roleDefinitionForm.code = selectedRole.value.code
  roleDefinitionForm.name = selectedRole.value.name
  roleDefinitionForm.description = selectedRole.value.description
}

function closeRoleDialog(): void {
  if (savingRoleDefinition.value) {
    return
  }
  roleDialogMode.value = null
  roleDefinitionError.value = ''
}

async function saveRoleDefinition(): Promise<void> {
  roleDefinitionError.value = ''
  savingRoleDefinition.value = true
  const creating = roleDialogMode.value === 'create'
  try {
    let roleId = selectedRoleId.value
    if (creating) {
      const created = await createRoleRequest({
        code: roleDefinitionForm.code.trim().toUpperCase(),
        name: roleDefinitionForm.name.trim(),
        description: roleDefinitionForm.description.trim(),
        permissionIds: []
      })
      roleId = created.id
    } else if (roleId !== null) {
      await updateRoleRequest(roleId, {
        name: roleDefinitionForm.name.trim(),
        description: roleDefinitionForm.description.trim()
      })
    }
    roleDialogMode.value = null
    await loadOverview()
    const target = overview.value?.roles.find((role) => role.id === roleId)
    if (target) {
      selectRole(target)
    }
    message.value = creating ? '角色已创建' : '角色信息已更新'
  } catch (error: unknown) {
    roleDefinitionError.value = error instanceof Error ? error.message : '角色保存失败'
  } finally {
    savingRoleDefinition.value = false
  }
}

async function toggleSelectedRoleStatus(): Promise<void> {
  if (!selectedRole.value || selectedRole.value.builtIn) {
    return
  }
  const nextStatus = selectedRole.value.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  if (
    nextStatus === 'DISABLED' &&
    !window.confirm(`停用“${selectedRole.value.name}”后，其权限将立即失效。继续吗？`)
  ) {
    return
  }
  saving.value = 'role-status'
  clearMessages()
  try {
    const roleId = selectedRole.value.id
    await updateRoleStatusRequest(roleId, nextStatus)
    await loadOverview()
    const target = overview.value?.roles.find((role) => role.id === roleId)
    if (target) {
      selectRole(target)
    }
    message.value = nextStatus === 'ACTIVE' ? '角色已启用' : '角色已停用'
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '角色状态更新失败'
  } finally {
    saving.value = ''
  }
}

async function removeSelectedRole(): Promise<void> {
  if (!selectedRole.value || selectedRole.value.builtIn) {
    return
  }
  if (
    !window.confirm(
      `删除“${selectedRole.value.name}”会同时移除其权限和用户关联。此操作不能恢复，继续吗？`
    )
  ) {
    return
  }
  saving.value = 'role-delete'
  clearMessages()
  try {
    await deleteRoleRequest(selectedRole.value.id)
    selectedRoleId.value = null
    await loadOverview()
    message.value = '角色已删除'
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '角色删除失败'
  } finally {
    saving.value = ''
  }
}

function permissionDisabled(permission: RbacPermissionResponse): boolean {
  return isSuperAdminRole.value || permission.code === 'admin.rbac.manage'
}

function roleName(roleId: number): string {
  return overview.value?.roles.find((role) => role.id === roleId)?.name ?? String(roleId)
}

function clearMessages(): void {
  message.value = ''
  errorMessage.value = ''
}

function sameIds(left: number[], right: number[]): boolean {
  if (left.length !== right.length) {
    return false
  }
  const sortedLeft = [...left].sort((a, b) => a - b)
  const sortedRight = [...right].sort((a, b) => a - b)
  return sortedLeft.every((value, index) => value === sortedRight[index])
}
</script>

<template>
  <section class="rbac-page">
    <header class="page-heading">
      <div class="heading-copy">
        <div class="heading-kicker">
          <span class="security-mark">S</span>
          <span>Security / Access Control</span>
        </div>
        <h1>角色与权限控制</h1>
        <p>集中管理功能权限与用户角色。权限变更将在下一次请求立即生效。</p>
      </div>
      <div class="heading-status">
        <span>访问控制状态</span>
        <strong><i /> RBAC 已启用</strong>
        <small>超级管理员专属</small>
      </div>
    </header>

    <section class="summary-strip">
      <article>
        <span>系统角色</span>
        <strong>{{ overview?.roles.length ?? '—' }}</strong>
        <small>内置角色组</small>
      </article>
      <article>
        <span>功能权限</span>
        <strong>{{ allPermissions.length }}</strong>
        <small>当前可分配权限点</small>
      </article>
      <article>
        <span>管理账号</span>
        <strong>{{ adminUserCount }}</strong>
        <small>拥有后台角色</small>
      </article>
      <article>
        <span>授权模型</span>
        <strong class="model-value">USER → ROLE → PERMISSION</strong>
        <small>多对多权限关系</small>
      </article>
    </section>

    <p v-if="message" class="notice success">{{ message }}</p>
    <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>
    <div v-if="loading" class="loading-state">
      <span class="spinner" />
      正在读取权限配置
    </div>

    <div v-else-if="overview" class="workspace">
      <aside class="control-panel role-panel">
        <div class="panel-title">
          <div>
            <span>角色目录</span>
            <h2>选择角色</h2>
          </div>
          <div class="panel-title-actions">
            <b>{{ filteredRoles.length }}</b>
            <button type="button" class="create-role-button" @click="openCreateRole">+ 新建</button>
          </div>
        </div>
        <div class="panel-search">
          <input v-model="roleSearch" type="search" placeholder="搜索角色名称或编码" />
        </div>
        <div class="role-list">
          <button
            v-for="role in filteredRoles"
            :key="role.id"
            type="button"
            class="role-card"
            :class="{
              selected: role.id === selectedRoleId,
              super: role.code === 'SUPER_ADMIN',
              disabled: role.status === 'DISABLED'
            }"
            @click="selectRole(role)"
          >
            <span class="role-card-top">
              <span class="role-avatar">{{ role.name.slice(0, 1) }}</span>
              <span>
                <strong>{{ role.name }}</strong>
                <code>{{ role.code }}</code>
              </span>
              <span class="role-state" :class="role.status.toLowerCase()">
                {{ role.status === 'ACTIVE' ? '启用' : '停用' }}
              </span>
            </span>
            <small>{{ role.description }}</small>
            <span class="role-meta">
              <span>{{ role.permissionIds.length }} 项权限</span>
              <span>{{ role.userCount }} 位用户</span>
            </span>
          </button>
        </div>
      </aside>

      <main class="control-panel permission-panel">
        <div class="panel-title permission-heading">
          <div>
            <span>权限矩阵</span>
            <h2>{{ selectedRole?.name ?? '请选择角色' }}</h2>
          </div>
          <div class="selection-count">
            <strong>{{ selectedPermissionCount }}</strong>
            <span>/ {{ allPermissions.length }}</span>
          </div>
        </div>

        <div
          v-if="selectedRole"
          class="role-lifecycle-bar"
          :class="{ protected: !canEditSelectedRole }"
        >
          <span>
            {{
              canEditSelectedRole
                ? '自定义角色管理'
                : '内置角色受保护，如需完整生命周期管理请先新建自定义角色'
            }}
          </span>
          <button type="button" :disabled="!canEditSelectedRole" @click="openEditRole">
            编辑资料
          </button>
          <button
            type="button"
            :disabled="!canEditSelectedRole || saving === 'role-status'"
            @click="toggleSelectedRoleStatus"
          >
            {{ selectedRole?.status === 'ACTIVE' ? '停用角色' : '启用角色' }}
          </button>
          <button
            type="button"
            class="danger"
            :disabled="!canEditSelectedRole || saving === 'role-delete'"
            @click="removeSelectedRole"
          >
            删除角色
          </button>
        </div>

        <div v-if="selectedRole" class="permission-groups">
          <section
            v-for="group in overview.permissionGroups"
            :key="group.module"
            class="permission-group"
          >
            <header>
              <div>
                <h3>{{ group.module }}</h3>
                <span>
                  {{
                    group.permissions.filter((item) => draftPermissionIds.includes(item.id)).length
                  }}
                  / {{ group.permissions.length }}
                </span>
              </div>
              <label class="group-toggle" :class="{ disabled: groupDisabled(group.permissions) }">
                <input
                  type="checkbox"
                  :checked="groupChecked(group.permissions)"
                  :disabled="groupDisabled(group.permissions)"
                  @change="toggleGroup(group.permissions, $event)"
                />
                全选
              </label>
            </header>
            <div class="permission-list">
              <label
                v-for="permission in group.permissions"
                :key="permission.id"
                class="permission-item"
                :class="{
                  disabled: permissionDisabled(permission),
                  checked: draftPermissionIds.includes(permission.id)
                }"
              >
                <input
                  type="checkbox"
                  :checked="draftPermissionIds.includes(permission.id)"
                  :disabled="permissionDisabled(permission)"
                  @change="togglePermission(permission, $event)"
                />
                <span class="check-box">✓</span>
                <span class="permission-copy">
                  <strong>{{ permission.name }}</strong>
                  <small>{{ permission.code }}</small>
                </span>
              </label>
            </div>
          </section>
        </div>

        <footer class="panel-actions">
          <p v-if="isSuperAdminRole" class="locked-note">
            超级管理员固定拥有全部权限，无法在此取消。
          </p>
          <p v-else-if="roleDirty" class="dirty-note">当前存在尚未保存的权限变更。</p>
          <p v-else class="saved-note">权限配置已同步。</p>
          <button
            class="primary-action"
            type="button"
            :disabled="saving === 'role' || isSuperAdminRole || !roleDirty"
            @click="saveRolePermissions"
          >
            {{ saving === 'role' ? '保存中…' : '保存权限配置' }}
          </button>
        </footer>
      </main>

      <aside class="control-panel user-panel">
        <div class="panel-title">
          <div>
            <span>成员授权</span>
            <h2>用户角色</h2>
          </div>
          <b>{{ filteredUsers.length }}</b>
        </div>
        <div class="panel-search">
          <input v-model="userSearch" type="search" placeholder="搜索账号、昵称或邮箱" />
        </div>

        <div class="user-list">
          <button
            v-for="user in filteredUsers"
            :key="user.id"
            type="button"
            class="user-row"
            :class="{ selected: user.id === selectedUserId }"
            @click="selectUser(user)"
          >
            <span class="avatar">{{ user.username.slice(0, 1).toUpperCase() }}</span>
            <span class="user-copy">
              <strong>{{ user.username }}</strong>
              <small>{{ user.email }}</small>
              <span class="role-chips">
                <i v-for="roleId in user.roleIds" :key="roleId">{{ roleName(roleId) }}</i>
              </span>
            </span>
          </button>
        </div>

        <section v-if="selectedUser" class="user-editor">
          <header>
            <div>
              <span>正在编辑</span>
              <h3>{{ selectedUser.username }}</h3>
            </div>
            <code>{{ selectedUser.status }}</code>
          </header>
          <label v-for="role in overview.roles" :key="role.id" class="role-option">
            <input
              type="checkbox"
              :checked="draftRoleIds.includes(role.id)"
              :disabled="role.status !== 'ACTIVE' && !draftRoleIds.includes(role.id)"
              @change="toggleRole(role, $event)"
            />
            <span class="check-box" :class="{ checked: draftRoleIds.includes(role.id) }">✓</span>
            <span>
              <strong>{{ role.name }}</strong>
              <code>{{ role.code }} · {{ role.status === 'ACTIVE' ? '启用' : '停用' }}</code>
            </span>
          </label>
          <button
            class="primary-action full"
            type="button"
            :disabled="saving === 'user' || !userDirty"
            @click="saveUserRoles"
          >
            {{ saving === 'user' ? '保存中…' : '保存用户角色' }}
          </button>
        </section>
      </aside>
    </div>

    <div
      v-if="roleDialogMode"
      class="dialog-backdrop"
      role="presentation"
      @click.self="closeRoleDialog"
    >
      <form class="role-dialog" @submit.prevent="saveRoleDefinition">
        <header>
          <div>
            <span>ROLE DEFINITION</span>
            <h2>{{ roleDialogMode === 'create' ? '新建自定义角色' : '编辑角色资料' }}</h2>
          </div>
          <button type="button" @click="closeRoleDialog">×</button>
        </header>
        <label>
          角色编码
          <input
            v-model.trim="roleDefinitionForm.code"
            :disabled="roleDialogMode === 'edit'"
            required
            minlength="2"
            maxlength="50"
            pattern="[A-Z][A-Z0-9_]*"
            placeholder="例如 OPERATOR"
          />
        </label>
        <label>
          角色名称
          <input
            v-model.trim="roleDefinitionForm.name"
            required
            maxlength="100"
            placeholder="例如 运营人员"
          />
        </label>
        <label>
          角色说明
          <textarea
            v-model.trim="roleDefinitionForm.description"
            rows="3"
            maxlength="500"
            placeholder="说明该角色的职责范围"
          />
        </label>
        <p v-if="roleDialogMode === 'create'" class="dialog-note">
          创建后默认没有权限，可继续在中间权限矩阵中配置。
        </p>
        <p v-if="roleDefinitionError" class="dialog-error">{{ roleDefinitionError }}</p>
        <footer>
          <button type="button" class="secondary" @click="closeRoleDialog">取消</button>
          <button class="primary-action" type="submit" :disabled="savingRoleDefinition">
            {{
              savingRoleDefinition
                ? '保存中…'
                : roleDialogMode === 'create'
                  ? '创建角色'
                  : '保存修改'
            }}
          </button>
        </footer>
      </form>
    </div>
  </section>
</template>

<style scoped>
.rbac-page {
  display: grid;
  gap: 20px;
}

.page-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
}

.page-heading h1,
.page-heading p {
  margin: 0;
}

.page-heading h1 {
  font-size: clamp(28px, 4vw, 42px);
  letter-spacing: -0.04em;
}

.page-heading > div > p:last-child {
  margin-top: 8px;
  color: #64748b;
}

.eyebrow {
  margin-bottom: 8px !important;
  color: #2563eb !important;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.read-only-note {
  display: grid;
  gap: 4px;
  min-width: 230px;
  padding: 16px 20px;
  border-left: 4px solid #0f172a;
  background: #fff;
}

.read-only-note span {
  color: #64748b;
  font-size: 12px;
  text-transform: uppercase;
}

.notice,
.loading {
  margin: 0;
  padding: 12px 16px;
  border: 1px solid #dbe3ef;
  background: #fff;
}

.notice.success {
  border-color: #86efac;
  background: #f0fdf4;
  color: #166534;
}

.notice.error {
  border-color: #fca5a5;
  background: #fef2f2;
  color: #991b1b;
}

.workspace {
  display: grid;
  grid-template-columns: minmax(220px, 0.75fr) minmax(360px, 1.5fr) minmax(260px, 0.9fr);
  gap: 16px;
  align-items: start;
}

.role-panel,
.permission-panel,
.user-panel {
  border: 1px solid #dbe3ef;
  background: #fff;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.panel-title > span {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  background: #0f172a;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
}

.panel-title h2,
.panel-title p {
  margin: 0;
}

.panel-title h2 {
  font-size: 17px;
}

.panel-title p {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.role-panel,
.user-panel {
  padding-bottom: 12px;
}

.role-card {
  display: grid;
  gap: 5px;
  width: calc(100% - 24px);
  margin: 12px 12px 0;
  padding: 15px;
  border: 1px solid transparent;
  background: #f8fafc;
  color: #0f172a;
  text-align: left;
  cursor: pointer;
}

.role-card:hover,
.role-card.selected {
  border-color: #2563eb;
  background: #eff6ff;
}

.role-card.super {
  background: #fff7ed;
}

.role-code {
  color: #2563eb;
  font-family: Consolas, monospace;
  font-size: 11px;
  font-weight: 700;
}

.role-card small {
  color: #64748b;
  line-height: 1.5;
}

.role-meta {
  margin-top: 4px;
  color: #475569;
  font-size: 12px;
}

.permission-groups {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.permission-group {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.permission-group h3 {
  grid-column: 1 / -1;
  margin: 0 0 2px;
  color: #475569;
  font-size: 12px;
  text-transform: uppercase;
}

.permission-item,
.role-option {
  display: flex;
  align-items: start;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  cursor: pointer;
}

.permission-item.disabled {
  background: #f1f5f9;
  color: #94a3b8;
  cursor: not-allowed;
}

.permission-item span,
.role-option span {
  display: grid;
  gap: 3px;
}

.permission-item small,
.role-option code {
  color: #64748b;
  font-family: Consolas, monospace;
  font-size: 11px;
}

.panel-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-top: 1px solid #e2e8f0;
}

.panel-actions p {
  margin: 0;
  color: #92400e;
  font-size: 13px;
}

button {
  cursor: pointer;
}

.panel-actions button,
.user-editor > button {
  padding: 10px 16px;
  border: 0;
  background: #0f172a;
  color: #fff;
  font-weight: 700;
}

button:disabled {
  cursor: wait;
  opacity: 0.55;
}

.user-list {
  display: grid;
  gap: 8px;
  padding: 12px;
}

.user-row {
  display: flex;
  gap: 10px;
  padding: 10px;
  border: 1px solid transparent;
  background: #f8fafc;
  color: #0f172a;
  text-align: left;
}

.user-row:hover,
.user-row.selected {
  border-color: #2563eb;
  background: #eff6ff;
}

.avatar {
  display: grid;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 50%;
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 800;
}

.user-copy {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.user-copy small {
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
}

.role-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 3px;
}

.role-chips i {
  padding: 2px 5px;
  background: #e2e8f0;
  color: #475569;
  font-size: 10px;
  font-style: normal;
}

.user-editor {
  display: grid;
  gap: 8px;
  margin: 0 12px 12px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.user-editor h3 {
  margin: 0 0 4px;
  font-size: 14px;
}

.role-option {
  align-items: center;
}

@media (max-width: 1150px) {
  .workspace {
    grid-template-columns: minmax(220px, 0.7fr) minmax(400px, 1.3fr);
  }

  .user-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .page-heading {
    display: grid;
  }

  .workspace,
  .permission-group {
    grid-template-columns: 1fr;
  }

  .user-panel {
    grid-column: auto;
  }
}
</style>

<style scoped>
.panel-title-actions {
  display: flex;
  align-items: center;
  gap: 7px;
}

.create-role-button {
  min-height: 27px;
  padding: 0 9px;
  border: 1px solid #b9c9e7;
  border-radius: 5px;
  background: #f4f8ff;
  color: #295dac;
  font-size: 9px;
  font-weight: 700;
  cursor: pointer;
}

.create-role-button:hover {
  border-color: #6f96df;
  background: #eaf2ff;
}

.role-card-top {
  grid-template-columns: 29px minmax(0, 1fr) auto;
}

.role-state {
  padding: 2px 5px;
  border-radius: 3px;
  font-size: 7px;
  font-weight: 700;
}

.role-state.active {
  background: #e8f6ed;
  color: #247447;
}

.role-state.disabled {
  background: #f1f3f5;
  color: #7b8491;
}

.role-card.disabled {
  border-style: dashed;
  opacity: 0.72;
}

.role-card.disabled.selected {
  opacity: 1;
}

.role-lifecycle-bar {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 14px;
  border-bottom: 1px solid #e7ebf0;
  background: #f7f9fc;
}

.role-lifecycle-bar > span {
  margin-right: auto;
  color: #7d8796;
  font-size: 9px;
  font-weight: 650;
}

.role-lifecycle-bar.protected {
  background: #fffaf1;
}

.role-lifecycle-bar.protected > span {
  color: #9a6a21;
}

.role-lifecycle-bar button {
  min-height: 27px;
  padding: 0 9px;
  border: 1px solid #d8dee8;
  border-radius: 5px;
  background: #fff;
  color: #526077;
  font-size: 8px;
  cursor: pointer;
}

.role-lifecycle-bar button:hover {
  border-color: #9eb4d8;
  color: #285ba9;
}

.role-lifecycle-bar button.danger {
  border-color: #edc7cb;
  color: #a13a43;
}

.role-lifecycle-bar button:disabled {
  cursor: wait;
  opacity: 0.55;
}

.dialog-backdrop {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgb(15 23 42 / 34%);
  backdrop-filter: blur(3px);
}

.role-dialog {
  display: grid;
  gap: 14px;
  width: min(460px, calc(100vw - 40px));
  padding: 22px;
  border: 1px solid #dfe4eb;
  border-radius: 11px;
  background: #fff;
  box-shadow: 0 24px 70px rgb(15 23 42 / 24%);
}

.role-dialog > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #edf0f3;
}

.role-dialog > header span {
  color: #8d96a4;
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.role-dialog h2 {
  margin: 4px 0 0;
  color: #263148;
  font-size: 17px;
}

.role-dialog > header button {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 5px;
  background: #f1f3f6;
  color: #6f7886;
  font-size: 18px;
  cursor: pointer;
}

.role-dialog > label {
  display: grid;
  gap: 7px;
  color: #4e596a;
  font-size: 9px;
  font-weight: 650;
}

.role-dialog input,
.role-dialog textarea {
  width: 100%;
  padding: 9px 10px;
  border: 1px solid #d8dee6;
  border-radius: 6px;
  outline: none;
  color: #2e394d;
  font-size: 11px;
}

.role-dialog input:focus,
.role-dialog textarea:focus {
  border-color: #789ce0;
  box-shadow: 0 0 0 2px rgb(36 89 198 / 7%);
}

.role-dialog input:disabled {
  background: #f2f4f7;
  color: #8a93a0;
}

.role-dialog textarea {
  resize: vertical;
  line-height: 1.6;
}

.dialog-note,
.dialog-error {
  margin: 0;
  padding: 8px 10px;
  border-radius: 5px;
  font-size: 9px;
  line-height: 1.6;
}

.dialog-note {
  background: #f5f7fa;
  color: #75808f;
}

.dialog-error {
  border: 1px solid #f0c3c8;
  background: #fff6f7;
  color: #a1323d;
}

.role-dialog > footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 4px;
}

.role-dialog > footer button {
  min-height: 32px;
  padding: 0 13px;
  border-radius: 6px;
  font-size: 9px;
  font-weight: 700;
  cursor: pointer;
}

.role-dialog > footer .secondary {
  border: 1px solid #d9dfe7;
  background: #fff;
  color: #667181;
}

.role-option input:disabled {
  cursor: not-allowed;
}
</style>

<style scoped>
.rbac-page {
  --rb-blue: #2459c6;
  --rb-blue-soft: #edf3ff;
  --rb-ink: #182033;
  --rb-muted: #747e8e;
  --rb-line: #e2e6ec;
  display: grid;
  gap: 18px;
  max-width: 1480px;
  margin: 0 auto;
  color: #344054;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  padding: 4px 0 20px;
  border-bottom: 1px solid var(--rb-line);
}

.heading-kicker {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #7b8595;
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 0.11em;
  text-transform: uppercase;
}

.security-mark {
  display: grid;
  width: 25px;
  height: 25px;
  place-items: center;
  border-radius: 6px;
  background: #17233b;
  color: #fff;
  font-size: 11px;
  letter-spacing: 0;
}

.page-heading h1 {
  margin: 11px 0 8px;
  color: var(--rb-ink);
  font-size: 30px;
  font-weight: 760;
  letter-spacing: -0.04em;
}

.heading-copy > p {
  margin: 0;
  color: var(--rb-muted);
  font-size: 13px;
}

.heading-status {
  display: grid;
  gap: 5px;
  min-width: 210px;
  padding: 13px 16px;
  border: 1px solid #d9e5da;
  border-radius: 9px;
  background: #f6fbf7;
}

.heading-status > span,
.heading-status small {
  color: #7d8a82;
  font-size: 10px;
}

.heading-status strong {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #286541;
  font-size: 12px;
}

.heading-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22a45d;
  box-shadow: 0 0 0 4px rgb(34 164 93 / 10%);
}

.summary-strip {
  display: grid;
  grid-template-columns: 0.8fr 0.8fr 0.8fr 1.7fr;
  overflow: hidden;
  border: 1px solid var(--rb-line);
  border-radius: 11px;
  background: #fff;
}

.summary-strip article {
  display: grid;
  gap: 4px;
  padding: 15px 18px;
  border-right: 1px solid var(--rb-line);
}

.summary-strip article:last-child {
  border-right: 0;
}

.summary-strip span,
.summary-strip small {
  color: #8992a0;
  font-size: 9px;
}

.summary-strip span {
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.summary-strip strong {
  color: #253047;
  font-size: 20px;
  line-height: 1.2;
}

.summary-strip .model-value {
  color: #2c5aaf;
  font-family: Consolas, monospace;
  font-size: 13px;
  letter-spacing: -0.01em;
}

.notice,
.loading-state {
  margin: 0;
  padding: 11px 14px;
  border: 1px solid var(--rb-line);
  border-radius: 8px;
  background: #fff;
  font-size: 12px;
}

.notice.success {
  border-color: #b9e4c7;
  background: #f4fbf6;
  color: #24663f;
}

.notice.error {
  border-color: #f1bdc2;
  background: #fff7f7;
  color: #a1323d;
}

.loading-state {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6b7280;
}

.spinner {
  width: 13px;
  height: 13px;
  border: 2px solid #ced8e9;
  border-top-color: var(--rb-blue);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.workspace {
  display: grid;
  grid-template-columns: 250px minmax(500px, 1fr) 300px;
  gap: 14px;
  align-items: stretch;
  min-height: 660px;
}

.control-panel {
  overflow: hidden;
  border: 1px solid var(--rb-line);
  border-radius: 11px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(16 24 40 / 3%);
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 67px;
  padding: 13px 16px;
  border-bottom: 1px solid var(--rb-line);
}

.panel-title > div:first-child {
  display: grid;
  gap: 3px;
}

.panel-title span {
  color: #8f98a6;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.09em;
  text-transform: uppercase;
}

.panel-title h2 {
  margin: 0;
  color: #263148;
  font-size: 15px;
  letter-spacing: -0.02em;
}

.panel-title > b {
  display: grid;
  min-width: 25px;
  height: 25px;
  place-items: center;
  border-radius: 6px;
  background: #f1f3f6;
  color: #687385;
  font-size: 10px;
}

.panel-search {
  padding: 10px 12px;
  border-bottom: 1px solid #edf0f3;
  background: #fafbfc;
}

.panel-search input {
  width: 100%;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #dde2e9;
  border-radius: 6px;
  outline: none;
  background: #fff;
  color: #334155;
  font-size: 10px;
}

.panel-search input:focus {
  border-color: #8badef;
  box-shadow: 0 0 0 2px rgb(36 89 198 / 7%);
}

.role-list,
.user-list {
  display: grid;
  align-content: start;
  gap: 7px;
  padding: 10px;
}

.role-card {
  display: grid;
  gap: 8px;
  width: 100%;
  padding: 12px;
  border: 1px solid #e5e8ed;
  border-radius: 8px;
  background: #fff;
  color: #344054;
  text-align: left;
  cursor: pointer;
}

.role-card:hover {
  border-color: #b7c7e7;
  background: #f8faff;
}

.role-card.selected {
  border-color: #6f96df;
  background: #f1f6ff;
  box-shadow: inset 3px 0 var(--rb-blue);
}

.role-card.super {
  background: #fffbf3;
}

.role-card.super.selected {
  border-color: #d7ad63;
  background: #fff8eb;
  box-shadow: inset 3px 0 #c68a2d;
}

.role-card-top {
  display: grid;
  grid-template-columns: 29px minmax(0, 1fr) 8px;
  align-items: center;
  gap: 9px;
}

.role-avatar {
  display: grid;
  width: 29px;
  height: 29px;
  place-items: center;
  border-radius: 6px;
  background: #e8eef9;
  color: #345fae;
  font-size: 11px;
  font-weight: 800;
}

.role-card-top > span:nth-child(2) {
  display: grid;
  gap: 2px;
}

.role-card-top strong {
  font-size: 11px;
}

.role-card code {
  color: #9098a5;
  font-family: Consolas, monospace;
  font-size: 8px;
}

.role-card-top > i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--rb-blue);
}

.role-card > small {
  color: #7d8796;
  font-size: 9px;
  line-height: 1.6;
}

.role-meta {
  display: flex;
  gap: 12px;
  padding-top: 7px;
  border-top: 1px solid #edf0f3;
  color: #7a8493;
  font-size: 8px;
}

.permission-panel {
  display: flex;
  flex-direction: column;
}

.permission-heading {
  min-height: 67px;
}

.selection-count {
  display: flex;
  align-items: baseline;
  gap: 3px;
  padding: 5px 9px;
  border-radius: 7px;
  background: #f1f5fd;
}

.selection-count strong {
  color: #285bb9;
  font-size: 16px;
}

.selection-count span {
  color: #7e8ba0;
  font-size: 9px;
  letter-spacing: 0;
  text-transform: none;
}

.permission-groups {
  display: grid;
  flex: 1;
  align-content: start;
  gap: 10px;
  padding: 14px;
  background: #fafbfc;
}

.permission-group {
  overflow: hidden;
  border: 1px solid #e4e8ee;
  border-radius: 8px;
  background: #fff;
}

.permission-group > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 38px;
  padding: 8px 12px;
  border-bottom: 1px solid #edf0f3;
  background: #f8f9fb;
}

.permission-group > header > div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.permission-group h3 {
  margin: 0;
  color: #38445a;
  font-size: 10px;
}

.permission-group > header span {
  color: #98a0ac;
  font-size: 8px;
}

.group-toggle {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #6d7787;
  font-size: 9px;
  cursor: pointer;
}

.group-toggle.disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.permission-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
}

.permission-item {
  display: grid;
  grid-template-columns: 15px minmax(0, 1fr);
  gap: 8px;
  min-height: 48px;
  padding: 9px 12px;
  border-right: 1px solid #edf0f3;
  border-bottom: 1px solid #edf0f3;
  cursor: pointer;
}

.permission-item:nth-child(2n) {
  border-right: 0;
}

.permission-item:hover {
  background: #f8faff;
}

.permission-item.disabled {
  background: #fafafa;
  cursor: not-allowed;
  opacity: 0.58;
}

.permission-item > input,
.role-option > input,
.group-toggle > input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.check-box {
  display: grid;
  width: 15px;
  height: 15px;
  place-items: center;
  border: 1px solid #cbd2dc;
  border-radius: 4px;
  background: #fff;
  color: transparent;
  font-size: 9px;
  font-weight: 800;
}

.permission-item.checked .check-box,
.check-box.checked {
  border-color: #3268c9;
  background: #3268c9;
  color: #fff;
}

.permission-copy {
  display: grid;
  gap: 3px;
}

.permission-copy strong {
  color: #354056;
  font-size: 10px;
  font-weight: 650;
}

.permission-copy small {
  color: #959daa;
  font-family: Consolas, monospace;
  font-size: 8px;
}

.panel-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 58px;
  margin-top: auto;
  padding: 10px 15px;
  border-top: 1px solid var(--rb-line);
  background: #fff;
}

.panel-actions p {
  margin: 0;
  font-size: 9px;
}

.locked-note {
  color: #9a6a21;
}

.dirty-note {
  color: #b06120;
}

.saved-note {
  color: #6d7787;
}

.primary-action {
  min-height: 33px;
  padding: 0 14px;
  border: 1px solid #2459c6;
  border-radius: 6px;
  background: #2459c6;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 10px rgb(36 89 198 / 13%);
}

.primary-action:disabled {
  border-color: #d8dde5;
  background: #e9edf3;
  color: #9aa3af;
  cursor: not-allowed;
  box-shadow: none;
}

.primary-action.full {
  width: 100%;
  margin-top: 4px;
}

.user-list {
  max-height: 300px;
  overflow-y: auto;
  border-bottom: 1px solid #edf0f3;
}

.user-row {
  display: flex;
  gap: 9px;
  width: 100%;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: #fff;
  color: #344054;
  text-align: left;
  cursor: pointer;
}

.user-row:hover {
  background: #f8faff;
}

.user-row.selected {
  border-color: #b8c9e8;
  background: #f1f6ff;
}

.avatar {
  display: grid;
  flex: 0 0 31px;
  width: 31px;
  height: 31px;
  place-items: center;
  border-radius: 7px;
  background: #e8eef9;
  color: #315caa;
  font-size: 10px;
  font-weight: 800;
}

.user-copy {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.user-copy strong {
  font-size: 10px;
}

.user-copy small {
  overflow: hidden;
  color: #8c95a2;
  font-size: 8px;
  text-overflow: ellipsis;
}

.role-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
  margin-top: 3px;
}

.role-chips i {
  padding: 2px 5px;
  border-radius: 3px;
  background: #eef2f7;
  color: #667181;
  font-size: 7px;
  font-style: normal;
}

.user-editor {
  display: grid;
  gap: 7px;
  padding: 13px;
}

.user-editor > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 3px;
}

.user-editor > header > div {
  display: grid;
  gap: 2px;
}

.user-editor > header span {
  color: #929aa7;
  font-size: 8px;
}

.user-editor h3 {
  margin: 0;
  color: #354056;
  font-size: 11px;
}

.user-editor > header code {
  padding: 3px 6px;
  border-radius: 4px;
  background: #eff8f2;
  color: #267447;
  font-size: 8px;
}

.role-option {
  display: grid;
  grid-template-columns: 15px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  padding: 8px 9px;
  border: 1px solid #e5e8ed;
  border-radius: 6px;
  cursor: pointer;
}

.role-option:hover {
  background: #f8faff;
}

.role-option > span:last-child {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.role-option strong {
  color: #3c4659;
  font-size: 9px;
}

.role-option code {
  color: #99a1ad;
  font-family: Consolas, monospace;
  font-size: 7px;
}
</style>

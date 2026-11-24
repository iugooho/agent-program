<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  copyRole as copyRoleRequest,
  createDepartment as createDepartmentRequest,
  createPermission as createPermissionRequest,
  createRole as createRoleRequest,
  deleteDepartment as deleteDepartmentRequest,
  deletePermission as deletePermissionRequest,
  deleteRole as deleteRoleRequest,
  fetchRbacAuditLogs,
  fetchRbacOverview,
  RbacApiError,
  updateDepartment as updateDepartmentRequest,
  updateDepartmentMembers as updateDepartmentMembersRequest,
  updateRole as updateRoleRequest,
  updateRoleDataScope as updateRoleDataScopeRequest,
  updateRolePermissions,
  updateRoleStatus as updateRoleStatusRequest,
  updateUserDepartments as updateUserDepartmentsRequest,
  updateUserRoles
} from '@/api/rbac'
import type {
  RbacAuditLogResponse,
  RbacDepartmentResponse,
  RbacOverviewResponse,
  RbacPermissionGroupResponse,
  RbacPermissionPackageResponse,
  RbacPermissionResponse,
  RbacRoleResponse,
  RbacUserResponse
} from '@/api/schema'

type SectionKey = 'roles' | 'departments' | 'users' | 'audit'

const overview = ref<RbacOverviewResponse | null>(null)
const activeSection = ref<SectionKey>('roles')
const selectedRoleId = ref<number | null>(null)
const selectedDepartmentId = ref<number | null>(null)
const selectedUserId = ref<number | null>(null)
const draftPermissionIds = ref<number[]>([])
const draftRoleIds = ref<number[]>([])
const draftDepartmentMemberIds = ref<number[]>([])
const draftUserDepartmentIds = ref<number[]>([])
const draftRoleDataScope = ref('SELF')
const draftRoleDataScopeDepartmentIds = ref<number[]>([])
const loading = ref(true)
const saving = ref('')
const message = ref('')
const errorMessage = ref('')
const auditLoading = ref(false)
const auditLogs = ref<RbacAuditLogResponse[]>([])
const roleSearch = ref('')
const departmentSearch = ref('')
const userSearch = ref('')
const permissionSearch = ref('')
const collapsedGroups = ref<string[]>([])
const roleDialogMode = ref<'create' | 'edit' | null>(null)
const savingRoleDefinition = ref(false)
const roleDefinitionError = ref('')
const permissionDialogOpen = ref(false)
const savingPermission = ref(false)
const permissionDialogError = ref('')
const copyDialogOpen = ref(false)
const savingCopyRole = ref(false)
const copyRoleError = ref('')
const departmentDialogMode = ref<'create' | 'edit' | null>(null)
const savingDepartment = ref(false)
const departmentDialogError = ref('')

const departmentForm = reactive({
  parentId: undefined as number | undefined,
  code: '',
  name: '',
  leaderUserId: undefined as number | undefined,
  status: 'ACTIVE',
  sortOrder: 0
})

const roleDefinitionForm = reactive({
  code: '',
  name: '',
  description: ''
})

const permissionForm = reactive({
  code: '',
  name: '',
  module: '',
  type: 'BUTTON',
  parentId: undefined as number | undefined,
  routePath: '',
  riskLevel: 'LOW',
  description: ''
})

const copyRoleForm = reactive({
  code: '',
  name: '',
  description: '',
  copyPermissions: true,
  copyMembers: false
})

const selectedRole = computed(
  () => overview.value?.roles.find((role) => role.id === selectedRoleId.value) ?? null
)
const selectedDepartment = computed(
  () =>
    overview.value?.departments.find(
      (department) => department.id === selectedDepartmentId.value
    ) ?? null
)
const selectedUser = computed(
  () => overview.value?.users.find((user) => user.id === selectedUserId.value) ?? null
)
const departmentTree = computed(() => {
  const departments = overview.value?.departments ?? []
  const keyword = departmentSearch.value.trim().toLowerCase()
  const includedIds = new Set<number>()
  if (keyword) {
    departments.forEach((department) => {
      if (includesText(department.name, keyword) || includesText(department.code, keyword)) {
        includedIds.add(department.id)
        let parentId = department.parentId
        const visited = new Set<number>()
        while (parentId !== undefined && !visited.has(parentId)) {
          visited.add(parentId)
          includedIds.add(parentId)
          parentId = departments.find((item) => item.id === parentId)?.parentId
        }
      }
    })
  }
  const visibleDepartments = departments.filter(
    (department) => !keyword || includedIds.has(department.id)
  )
  const children = new Map<number | null, RbacDepartmentResponse[]>()
  visibleDepartments.forEach((department) => {
    const parentId =
      department.parentId !== undefined &&
      visibleDepartments.some((item) => item.id === department.parentId)
        ? department.parentId
        : null
    const siblings = children.get(parentId) ?? []
    siblings.push(department)
    children.set(parentId, siblings)
  })
  const result: Array<{ department: RbacDepartmentResponse; depth: number }> = []
  const visit = (parentId: number | null, depth: number): void => {
    const siblings = children.get(parentId) ?? []
    siblings
      .sort((left, right) => left.sortOrder - right.sortOrder || left.id - right.id)
      .forEach((department) => {
        result.push({ department, depth })
        visit(department.id, depth + 1)
      })
  }
  visit(null, 0)
  visibleDepartments.forEach((department) => {
    if (!result.some((item) => item.department.id === department.id)) {
      result.push({ department, depth: 0 })
    }
  })
  return result
})
const activeDepartmentCount = computed(
  () =>
    overview.value?.departments.filter((department) => department.status === 'ACTIVE').length ?? 0
)
const departmentParentOptions = computed(() =>
  (overview.value?.departments ?? []).filter(
    (department) => department.id !== selectedDepartmentId.value && department.status !== 'DELETED'
  )
)
const departmentMemberDirty = computed(() => {
  if (!selectedDepartment.value) {
    return false
  }
  return !sameIds(draftDepartmentMemberIds.value, selectedDepartment.value.memberIds)
})
const allPermissions = computed(() => {
  if (!overview.value) {
    return []
  }
  return overview.value.permissionGroups.flatMap((group) => group.permissions)
})
const assignablePermissions = computed(() =>
  allPermissions.value.filter(
    (permission) => permission.type !== 'CATALOG' && permission.status === 'ACTIVE'
  )
)
const permissionParentOptions = computed(() =>
  allPermissions.value.filter(
    (permission) =>
      permission.status === 'ACTIVE' &&
      (permission.type === 'CATALOG' || permission.type === 'MENU')
  )
)
const dependencyMap = computed(() => {
  const map = new Map<number, number[]>()
  overview.value?.permissionDependencies.forEach((dependency) => {
    const required = map.get(dependency.permissionId) ?? []
    required.push(dependency.requiredPermissionId)
    map.set(dependency.permissionId, required)
  })
  return map
})
const reverseDependencyMap = computed(() => {
  const map = new Map<number, number[]>()
  overview.value?.permissionDependencies.forEach((dependency) => {
    const dependents = map.get(dependency.requiredPermissionId) ?? []
    dependents.push(dependency.permissionId)
    map.set(dependency.requiredPermissionId, dependents)
  })
  return map
})
const customRoleCount = computed(
  () => overview.value?.roles.filter((role) => !role.builtIn).length ?? 0
)
const activeRoleCount = computed(
  () => overview.value?.roles.filter((role) => role.status === 'ACTIVE').length ?? 0
)
const isSuperAdminRole = computed(() => selectedRole.value?.code === 'SUPER_ADMIN')

const filteredRoles = computed(() => {
  const keyword = roleSearch.value.trim().toLowerCase()
  if (!keyword) {
    return overview.value?.roles ?? []
  }
  return (overview.value?.roles ?? []).filter(
    (role) =>
      includesText(role.name, keyword) ||
      includesText(role.code, keyword) ||
      includesText(role.description, keyword)
  )
})

const filteredUsers = computed(() => {
  const keyword = userSearch.value.trim().toLowerCase()
  if (!keyword) {
    return overview.value?.users ?? []
  }
  return (overview.value?.users ?? []).filter(
    (user) =>
      includesText(user.username, keyword) ||
      includesText(user.nickname, keyword) ||
      includesText(user.email, keyword) ||
      user.roleCodes.some((code) => code.toLowerCase().includes(keyword))
  )
})

const filteredPermissionGroups = computed<RbacPermissionGroupResponse[]>(() => {
  const groups = overview.value?.permissionGroups ?? []
  const keyword = permissionSearch.value.trim().toLowerCase()
  if (!keyword) {
    return groups.map((group) => ({
      ...group,
      permissions: orderPermissionTree(group.permissions)
    }))
  }
  return groups
    .map((group) => ({
      ...group,
      permissions: orderPermissionTree(
        group.permissions.filter(
          (permission) =>
            includesText(permission.name, keyword) ||
            includesText(permission.code, keyword) ||
            includesText(permission.description, keyword) ||
            includesText(permission.routePath, keyword)
        )
      )
    }))
    .filter((group) => group.permissions.length > 0)
})

const roleDirty = computed(() => {
  if (!selectedRole.value) {
    return false
  }
  return !sameIds(draftPermissionIds.value, selectedRole.value.permissionIds)
})
const roleAddedPermissions = computed(() => {
  if (!selectedRole.value) {
    return []
  }
  return draftPermissionIds.value
    .filter((id) => !selectedRole.value?.permissionIds.includes(id))
    .map((id) => allPermissions.value.find((permission) => permission.id === id))
    .filter((permission): permission is RbacPermissionResponse => Boolean(permission))
})
const roleRemovedPermissions = computed(() => {
  if (!selectedRole.value) {
    return []
  }
  return selectedRole.value.permissionIds
    .filter((id) => !draftPermissionIds.value.includes(id))
    .map((id) => allPermissions.value.find((permission) => permission.id === id))
    .filter((permission): permission is RbacPermissionResponse => Boolean(permission))
})
const highRiskAdditions = computed(() =>
  roleAddedPermissions.value.filter((permission) => permission.riskLevel === 'HIGH')
)

const userDirty = computed(() => {
  if (!selectedUser.value) {
    return false
  }
  return !sameIds(draftRoleIds.value, selectedUser.value.roleIds)
})
const userDepartmentDirty = computed(() => {
  if (!selectedUser.value) {
    return false
  }
  return !sameIds(draftUserDepartmentIds.value, selectedUser.value.departmentIds)
})
const roleDataScopeDirty = computed(() => {
  if (!selectedRole.value) {
    return false
  }
  return (
    draftRoleDataScope.value !== selectedRole.value.dataScope ||
    !sameIds(
      draftRoleDataScope.value === 'DEPT_CUSTOM' ? draftRoleDataScopeDepartmentIds.value : [],
      selectedRole.value.dataScope === 'DEPT_CUSTOM'
        ? selectedRole.value.dataScopeDepartmentIds
        : []
    )
  )
})

const selectedPermissionCount = computed(
  () =>
    assignablePermissions.value.filter((permission) =>
      draftPermissionIds.value.includes(permission.id)
    ).length
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
const rolePermissionPercent = computed(() => {
  if (assignablePermissions.value.length === 0) {
    return 0
  }
  return Math.round((selectedPermissionCount.value / assignablePermissions.value.length) * 100)
})

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
    const department =
      overview.value.departments.find((item) => item.id === selectedDepartmentId.value) ??
      overview.value.departments[0]
    if (role) {
      selectRole(role)
    }
    if (user) {
      selectUser(user)
    }
    if (department) {
      selectDepartment(department)
    }
  } catch (error: unknown) {
    errorMessage.value = error instanceof RbacApiError ? error.message : '权限数据加载失败'
  } finally {
    loading.value = false
  }
}

async function switchSection(section: SectionKey): Promise<void> {
  activeSection.value = section
  clearMessages()
  if (section === 'audit') {
    await loadAuditLogs()
  }
}

async function loadAuditLogs(): Promise<void> {
  auditLoading.value = true
  clearMessages()
  try {
    auditLogs.value = await fetchRbacAuditLogs(100)
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '授权变更记录加载失败'
  } finally {
    auditLoading.value = false
  }
}

function selectRole(role: RbacRoleResponse): void {
  selectedRoleId.value = role.id
  draftPermissionIds.value = [...role.permissionIds].sort((left, right) => left - right)
  draftRoleDataScope.value = role.dataScope
  draftRoleDataScopeDepartmentIds.value = [...role.dataScopeDepartmentIds].sort(
    (left, right) => left - right
  )
  clearMessages()
}

function selectDepartment(department: RbacDepartmentResponse): void {
  selectedDepartmentId.value = department.id
  draftDepartmentMemberIds.value = [...department.memberIds].sort((left, right) => left - right)
  clearMessages()
}

function selectUser(user: RbacUserResponse): void {
  selectedUserId.value = user.id
  draftRoleIds.value = [...user.roleIds].sort((left, right) => left - right)
  draftUserDepartmentIds.value = [...user.departmentIds].sort((left, right) => left - right)
  clearMessages()
}

function togglePermission(permission: RbacPermissionResponse, event: Event): void {
  const checked = (event.target as HTMLInputElement).checked
  const next = new Set(draftPermissionIds.value)
  if (checked) {
    next.add(permission.id)
    addRequiredPermissions(next, permission.id)
  } else {
    removePermissionAndDependents(next, permission.id)
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

function groupSelectedCount(permissions: RbacPermissionResponse[]): number {
  return permissions.filter(
    (permission) =>
      permission.type !== 'CATALOG' && draftPermissionIds.value.includes(permission.id)
  ).length
}

function toggleGroup(permissions: RbacPermissionResponse[], event: Event): void {
  const checked = (event.target as HTMLInputElement).checked
  const groupIds = editableGroupPermissions(permissions).map((permission) => permission.id)
  const next = new Set(draftPermissionIds.value)
  groupIds.forEach((permissionId) => {
    if (checked) {
      next.add(permissionId)
      addRequiredPermissions(next, permissionId)
    } else {
      removePermissionAndDependents(next, permissionId)
    }
  })
  draftPermissionIds.value = [...next].sort((left, right) => left - right)
}

function addRequiredPermissions(target: Set<number>, permissionId: number): void {
  for (const requiredId of dependencyMap.value.get(permissionId) ?? []) {
    if (!target.has(requiredId)) {
      target.add(requiredId)
      addRequiredPermissions(target, requiredId)
    }
  }
}

function removePermissionAndDependents(target: Set<number>, permissionId: number): void {
  target.delete(permissionId)
  for (const dependentId of reverseDependencyMap.value.get(permissionId) ?? []) {
    if (target.has(dependentId)) {
      removePermissionAndDependents(target, dependentId)
    }
  }
}

function applyPermissionPackage(permissionPackage: RbacPermissionPackageResponse): void {
  const next = new Set(draftPermissionIds.value)
  permissionPackage.permissionIds.forEach((permissionId) => {
    const permission = allPermissions.value.find((item) => item.id === permissionId)
    if (!permission || permissionDisabled(permission)) {
      return
    }
    next.add(permissionId)
    addRequiredPermissions(next, permissionId)
  })
  draftPermissionIds.value = [...next].sort((left, right) => left - right)
}

function toggleGroupCollapse(module: string): void {
  collapsedGroups.value = collapsedGroups.value.includes(module)
    ? collapsedGroups.value.filter((item) => item !== module)
    : [...collapsedGroups.value, module]
}

function isGroupCollapsed(module: string): boolean {
  return collapsedGroups.value.includes(module)
}

async function saveRolePermissions(): Promise<void> {
  if (!selectedRole.value || isSuperAdminRole.value) {
    return
  }
  let confirmHighRisk = false
  if (highRiskAdditions.value.length > 0) {
    const names = highRiskAdditions.value.map((permission) => permission.name).join('、')
    if (!window.confirm(`本次将新增高风险权限：${names}。确认继续保存吗？`)) {
      return
    }
    confirmHighRisk = true
  }
  saving.value = 'role'
  clearMessages()
  try {
    await updateRolePermissions(selectedRole.value.id, draftPermissionIds.value, confirmHighRisk)
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

async function saveRoleDataScope(): Promise<void> {
  if (!selectedRole.value || isSuperAdminRole.value || !roleDataScopeDirty.value) {
    return
  }
  saving.value = 'role-data-scope'
  clearMessages()
  try {
    await updateRoleDataScopeRequest(
      selectedRole.value.id,
      draftRoleDataScope.value,
      draftRoleDataScope.value === 'DEPT_CUSTOM' ? draftRoleDataScopeDepartmentIds.value : []
    )
    await loadOverview()
    message.value = `已更新“${selectedRole.value.name}”的数据范围`
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '角色数据范围保存失败'
  } finally {
    saving.value = ''
  }
}

function toggleRoleDataScopeDepartment(departmentId: number, event: Event): void {
  const checked = (event.target as HTMLInputElement).checked
  const next = new Set(draftRoleDataScopeDepartmentIds.value)
  if (checked) {
    next.add(departmentId)
  } else {
    next.delete(departmentId)
  }
  draftRoleDataScopeDepartmentIds.value = [...next].sort((left, right) => left - right)
}

function toggleUserDepartment(departmentId: number, event: Event): void {
  const checked = (event.target as HTMLInputElement).checked
  const next = new Set(draftUserDepartmentIds.value)
  if (checked) {
    next.add(departmentId)
  } else {
    next.delete(departmentId)
  }
  draftUserDepartmentIds.value = [...next].sort((left, right) => left - right)
}

async function saveUserDepartments(): Promise<void> {
  if (!selectedUser.value || !userDepartmentDirty.value) {
    return
  }
  saving.value = 'user-departments'
  clearMessages()
  try {
    await updateUserDepartmentsRequest(selectedUser.value.id, draftUserDepartmentIds.value)
    await loadOverview()
    message.value = `已更新“${selectedUser.value.username}”的所属部门`
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '用户所属部门保存失败'
  } finally {
    saving.value = ''
  }
}

function toggleDepartmentMember(userId: number, event: Event): void {
  const checked = (event.target as HTMLInputElement).checked
  const next = new Set(draftDepartmentMemberIds.value)
  if (checked) {
    next.add(userId)
  } else {
    next.delete(userId)
  }
  draftDepartmentMemberIds.value = [...next].sort((left, right) => left - right)
}

async function saveDepartmentMembers(): Promise<void> {
  if (!selectedDepartment.value || !departmentMemberDirty.value) {
    return
  }
  saving.value = 'department-members'
  clearMessages()
  try {
    await updateDepartmentMembersRequest(
      selectedDepartment.value.id,
      draftDepartmentMemberIds.value
    )
    await loadOverview()
    message.value = `已更新“${selectedDepartment.value.name}”的成员`
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '部门成员保存失败'
  } finally {
    saving.value = ''
  }
}

function openCreateDepartment(parentId?: number): void {
  departmentDialogMode.value = 'create'
  departmentDialogError.value = ''
  departmentForm.parentId = parentId
  departmentForm.code = ''
  departmentForm.name = ''
  departmentForm.leaderUserId = undefined
  departmentForm.status = 'ACTIVE'
  departmentForm.sortOrder = (overview.value?.departments.length ?? 0) * 10 + 10
}

function openEditDepartment(): void {
  if (!selectedDepartment.value) {
    return
  }
  departmentDialogMode.value = 'edit'
  departmentDialogError.value = ''
  departmentForm.parentId = selectedDepartment.value.parentId
  departmentForm.code = selectedDepartment.value.code
  departmentForm.name = selectedDepartment.value.name
  departmentForm.leaderUserId = selectedDepartment.value.leaderUserId
  departmentForm.status = selectedDepartment.value.status
  departmentForm.sortOrder = selectedDepartment.value.sortOrder
}

function closeDepartmentDialog(): void {
  if (savingDepartment.value) {
    return
  }
  departmentDialogMode.value = null
  departmentDialogError.value = ''
}

async function saveDepartmentDefinition(): Promise<void> {
  departmentDialogError.value = ''
  savingDepartment.value = true
  const creating = departmentDialogMode.value === 'create'
  try {
    let departmentId = selectedDepartmentId.value
    if (creating) {
      const created = await createDepartmentRequest({
        parentId: departmentForm.parentId,
        code: departmentForm.code.trim().toUpperCase(),
        name: departmentForm.name.trim(),
        leaderUserId: departmentForm.leaderUserId,
        sortOrder: departmentForm.sortOrder
      })
      departmentId = created.id
    } else if (departmentId !== null) {
      await updateDepartmentRequest(departmentId, {
        parentId: departmentForm.parentId,
        name: departmentForm.name.trim(),
        leaderUserId: departmentForm.leaderUserId,
        status: departmentForm.status,
        sortOrder: departmentForm.sortOrder
      })
    }
    departmentDialogMode.value = null
    await loadOverview()
    const target = overview.value?.departments.find((department) => department.id === departmentId)
    if (target) {
      selectDepartment(target)
    }
    message.value = creating ? '部门已创建' : '部门资料已更新'
  } catch (error: unknown) {
    departmentDialogError.value = error instanceof Error ? error.message : '部门保存失败'
  } finally {
    savingDepartment.value = false
  }
}

async function toggleSelectedDepartmentStatus(): Promise<void> {
  if (!selectedDepartment.value) {
    return
  }
  const nextStatus = selectedDepartment.value.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  saving.value = 'department-status'
  clearMessages()
  try {
    const departmentId = selectedDepartment.value.id
    await updateDepartmentRequest(departmentId, {
      parentId: selectedDepartment.value.parentId,
      name: selectedDepartment.value.name,
      leaderUserId: selectedDepartment.value.leaderUserId,
      status: nextStatus,
      sortOrder: selectedDepartment.value.sortOrder
    })
    await loadOverview()
    const target = overview.value?.departments.find((department) => department.id === departmentId)
    if (target) {
      selectDepartment(target)
    }
    message.value = nextStatus === 'ACTIVE' ? '部门已启用' : '部门已停用'
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '部门状态更新失败'
  } finally {
    saving.value = ''
  }
}

async function removeSelectedDepartment(): Promise<void> {
  if (!selectedDepartment.value) {
    return
  }
  if (!window.confirm(`删除“${selectedDepartment.value.name}”前必须先删除其子部门。确认继续吗？`)) {
    return
  }
  saving.value = 'department-delete'
  clearMessages()
  try {
    await deleteDepartmentRequest(selectedDepartment.value.id)
    selectedDepartmentId.value = null
    await loadOverview()
    message.value = '部门已删除'
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '部门删除失败'
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

function openCopyRole(): void {
  if (!selectedRole.value) {
    return
  }
  copyDialogOpen.value = true
  copyRoleError.value = ''
  copyRoleForm.code = `${selectedRole.value.code}_COPY`
  copyRoleForm.name = `${selectedRole.value.name}副本`
  copyRoleForm.description = selectedRole.value.description
  copyRoleForm.copyPermissions = true
  copyRoleForm.copyMembers = selectedRole.value.userCount > 0
}

function closeCopyRoleDialog(): void {
  if (savingCopyRole.value) {
    return
  }
  copyDialogOpen.value = false
  copyRoleError.value = ''
}

async function saveCopyRole(): Promise<void> {
  if (!selectedRole.value) {
    return
  }
  copyRoleError.value = ''
  savingCopyRole.value = true
  try {
    const created = await copyRoleRequest(selectedRole.value.id, {
      code: copyRoleForm.code.trim().toUpperCase(),
      name: copyRoleForm.name.trim(),
      description: copyRoleForm.description.trim(),
      copyPermissions: copyRoleForm.copyPermissions,
      copyMembers: copyRoleForm.copyMembers
    })
    copyDialogOpen.value = false
    await loadOverview()
    const target = overview.value?.roles.find((role) => role.id === created.id)
    if (target) {
      selectRole(target)
    }
    message.value = `已复制角色“${created.name}”`
  } catch (error: unknown) {
    copyRoleError.value = error instanceof Error ? error.message : '角色复制失败'
  } finally {
    savingCopyRole.value = false
  }
}

function openCreatePermission(): void {
  permissionDialogOpen.value = true
  permissionDialogError.value = ''
  permissionForm.code = ''
  permissionForm.name = ''
  permissionForm.module = ''
  permissionForm.type = 'BUTTON'
  permissionForm.parentId = undefined
  permissionForm.routePath = ''
  permissionForm.riskLevel = 'LOW'
  permissionForm.description = ''
}

function syncPermissionModule(): void {
  const parent = allPermissions.value.find(
    (permission) => permission.id === permissionForm.parentId
  )
  if (parent) {
    permissionForm.module = parent.module
  }
}

function closePermissionDialog(): void {
  if (savingPermission.value) {
    return
  }
  permissionDialogOpen.value = false
  permissionDialogError.value = ''
}

async function savePermissionDefinition(): Promise<void> {
  permissionDialogError.value = ''
  savingPermission.value = true
  try {
    await createPermissionRequest({
      code: permissionForm.code.trim().toLowerCase(),
      name: permissionForm.name.trim(),
      module: permissionForm.module.trim(),
      type: permissionForm.type,
      parentId: permissionForm.parentId,
      routePath: permissionForm.routePath.trim(),
      riskLevel: permissionForm.riskLevel,
      description: permissionForm.description.trim()
    })
    permissionDialogOpen.value = false
    await loadOverview()
    message.value = '权限点已创建，并已自动授予超级管理员'
  } catch (error: unknown) {
    permissionDialogError.value = error instanceof Error ? error.message : '权限创建失败'
  } finally {
    savingPermission.value = false
  }
}

async function removePermission(permission: RbacPermissionResponse): Promise<void> {
  if (permission.builtIn) {
    return
  }
  if (
    !window.confirm(
      `删除权限“${permission.name}”后将立即从所有角色中失效，并保留审计记录。继续吗？`
    )
  ) {
    return
  }
  saving.value = 'permission-delete'
  clearMessages()
  try {
    await deletePermissionRequest(permission.id)
    await loadOverview()
    message.value = `已删除权限“${permission.name}”`
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '权限删除失败'
  } finally {
    saving.value = ''
  }
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
  return (
    isSuperAdminRole.value ||
    permission.type === 'CATALOG' ||
    permission.status !== 'ACTIVE' ||
    permission.code === 'admin.rbac.manage'
  )
}

function roleName(roleId: number): string {
  return overview.value?.roles.find((role) => role.id === roleId)?.name ?? String(roleId)
}

function departmentName(departmentId: number): string {
  return (
    overview.value?.departments.find((department) => department.id === departmentId)?.name ??
    String(departmentId)
  )
}

function userName(userId: number): string {
  const user = overview.value?.users.find((item) => item.id === userId)
  return user?.nickname || user?.username || String(userId)
}

function dataScopeLabel(scope: string): string {
  const labels: Record<string, string> = {
    ALL: '全部数据',
    DEPT_CUSTOM: '指定部门',
    DEPT_ONLY: '本部门',
    DEPT_AND_CHILD: '本部门及下级部门',
    SELF: '仅本人'
  }
  return labels[scope] ?? scope
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

function includesText(value: string | null | undefined, keyword: string): boolean {
  return (value ?? '').toLowerCase().includes(keyword)
}

function orderPermissionTree(permissions: RbacPermissionResponse[]): RbacPermissionResponse[] {
  const byParent = new Map<number | null, RbacPermissionResponse[]>()
  permissions.forEach((permission) => {
    const parentId =
      permission.parentId !== undefined &&
      permissions.some((item) => item.id === permission.parentId)
        ? permission.parentId
        : null
    const siblings = byParent.get(parentId) ?? []
    siblings.push(permission)
    byParent.set(parentId, siblings)
  })
  const result: RbacPermissionResponse[] = []
  const visit = (parentId: number | null): void => {
    const children = byParent.get(parentId) ?? []
    children.sort((left, right) => left.id - right.id)
    children.forEach((permission) => {
      result.push(permission)
      visit(permission.id)
    })
  }
  visit(null)
  permissions.forEach((permission) => {
    if (!result.some((item) => item.id === permission.id)) {
      result.push(permission)
    }
  })
  return result
}

function permissionDepth(permission: RbacPermissionResponse): number {
  let depth = 0
  let parentId = permission.parentId
  const visited = new Set<number>()
  while (parentId !== undefined && !visited.has(parentId)) {
    visited.add(parentId)
    const parent = allPermissions.value.find((item) => item.id === parentId)
    if (!parent) {
      break
    }
    depth += 1
    parentId = parent.parentId
  }
  return depth
}

function permissionTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    CATALOG: '目录',
    MENU: '菜单',
    PAGE: '页面',
    BUTTON: '按钮',
    API: '接口'
  }
  return labels[type] ?? type
}

function auditActionLabel(action: string): string {
  const labels: Record<string, string> = {
    PERMISSION_CREATE: '新增权限',
    PERMISSION_DELETE: '删除权限',
    ROLE_CREATE: '新增角色',
    ROLE_UPDATE: '编辑角色',
    ROLE_STATUS_UPDATE: '角色状态',
    ROLE_DELETE: '删除角色',
    ROLE_PERMISSIONS_UPDATE: '调整角色权限',
    ROLE_DATA_SCOPE_UPDATE: '调整数据范围',
    DEPARTMENT_CREATE: '新增部门',
    DEPARTMENT_UPDATE: '编辑部门',
    DEPARTMENT_DELETE: '删除部门',
    DEPARTMENT_MEMBERS_UPDATE: '调整部门成员',
    USER_DEPARTMENTS_UPDATE: '调整用户部门',
    USER_ROLES_UPDATE: '调整用户角色'
  }
  return labels[action] ?? action
}

function auditTargetLabel(log: RbacAuditLogResponse): string {
  const labels: Record<string, string> = {
    PERMISSION: '权限',
    ROLE: '角色',
    DEPARTMENT: '部门',
    USER: '用户'
  }
  return `${labels[log.targetType] ?? log.targetType} #${log.targetId ?? '—'}`
}

function formatAuditTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(new Date(value))
}
</script>

<template>
  <section class="access-page">
    <header class="page-hero">
      <div class="hero-copy">
        <div class="eyebrow">
          <span class="eyebrow-mark">AC</span>
          <span>Security / Access Control</span>
        </div>
        <h1>访问控制</h1>
        <p>集中维护角色权限、组织部门、数据范围和成员授权。配置保存后立即生效。</p>
      </div>
      <div class="hero-actions">
        <span class="system-status"><i /> RBAC 已启用</span>
        <button type="button" class="secondary-button" :disabled="loading" @click="loadOverview">
          {{ loading ? '刷新中…' : '刷新数据' }}
        </button>
      </div>
    </header>

    <section class="metric-grid" aria-label="访问控制概览">
      <article class="metric-card">
        <span>系统角色</span>
        <strong>{{ overview?.roles.length ?? '—' }}</strong>
        <small>{{ activeRoleCount }} 个启用角色</small>
      </article>
      <article class="metric-card">
        <span>功能权限</span>
        <strong>{{ assignablePermissions.length }}</strong>
        <small>覆盖 {{ overview?.permissionGroups.length ?? 0 }} 个功能模块</small>
      </article>
      <article class="metric-card">
        <span>管理账号</span>
        <strong>{{ adminUserCount }}</strong>
        <small>{{ activeDepartmentCount }} 个启用部门</small>
      </article>
      <article class="metric-card accent">
        <span>自定义角色</span>
        <strong>{{ customRoleCount }}</strong>
        <small>不含 USER、ADMIN、SUPER_ADMIN</small>
      </article>
    </section>

    <p v-if="message" class="feedback success">{{ message }}</p>
    <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>

    <div v-if="loading" class="state-card">
      <span class="spinner" />
      <div>
        <strong>正在读取权限配置</strong>
        <p>正在同步角色、权限点与成员数据。</p>
      </div>
    </div>

    <div v-else-if="overview" class="control-shell">
      <aside class="section-nav">
        <div class="nav-caption">控制面板</div>
        <button
          type="button"
          class="nav-item"
          :class="{ active: activeSection === 'roles' }"
          :aria-current="activeSection === 'roles' ? 'page' : undefined"
          @click="switchSection('roles')"
        >
          <span class="nav-icon">R</span>
          <span>
            <strong>角色控制</strong>
            <small>角色与权限矩阵</small>
          </span>
          <i>›</i>
        </button>
        <button
          type="button"
          class="nav-item"
          :class="{ active: activeSection === 'departments' }"
          :aria-current="activeSection === 'departments' ? 'page' : undefined"
          @click="switchSection('departments')"
        >
          <span class="nav-icon">D</span>
          <span>
            <strong>部门控制</strong>
            <small>组织树与数据归属</small>
          </span>
          <i>›</i>
        </button>
        <button
          type="button"
          class="nav-item"
          :class="{ active: activeSection === 'audit' }"
          :aria-current="activeSection === 'audit' ? 'page' : undefined"
          @click="switchSection('audit')"
        >
          <span class="nav-icon">A</span>
          <span>
            <strong>变更记录</strong>
            <small>授权操作审计</small>
          </span>
          <i>›</i>
        </button>
        <button
          type="button"
          class="nav-item"
          :class="{ active: activeSection === 'users' }"
          :aria-current="activeSection === 'users' ? 'page' : undefined"
          @click="switchSection('users')"
        >
          <span class="nav-icon">M</span>
          <span>
            <strong>成员授权</strong>
            <small>用户与角色分配</small>
          </span>
          <i>›</i>
        </button>
        <div class="nav-note">
          <strong>权限即时生效</strong>
          <p>角色停用或权限调整后，将在下一次请求重新计算有效权限。</p>
        </div>
      </aside>

      <main class="section-content">
        <template v-if="activeSection === 'roles'">
          <header class="content-heading">
            <div>
              <span>Role Management</span>
              <h2>角色与权限</h2>
              <p>选择角色后配置功能权限，并管理自定义角色的生命周期。</p>
            </div>
            <button type="button" class="primary-button" @click="openCreateRole">
              <span>＋</span>
              新建角色
            </button>
          </header>

          <div class="role-layout">
            <aside class="surface role-directory">
              <header class="surface-head">
                <div>
                  <span>角色目录</span>
                  <h3>全部角色</h3>
                </div>
                <b>{{ filteredRoles.length }}</b>
              </header>
              <div class="search-box">
                <span>⌕</span>
                <input
                  v-model="roleSearch"
                  type="search"
                  aria-label="搜索角色"
                  placeholder="搜索名称、编码或说明"
                />
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
                    inactive: role.status !== 'ACTIVE'
                  }"
                  @click="selectRole(role)"
                >
                  <span class="role-card-head">
                    <span class="role-avatar">{{ role.name.slice(0, 1) }}</span>
                    <span class="role-title">
                      <strong>{{ role.name }}</strong>
                      <code>{{ role.code }}</code>
                    </span>
                    <span
                      class="status-chip"
                      :class="role.status === 'ACTIVE' ? 'active' : 'disabled'"
                    >
                      {{ role.status === 'ACTIVE' ? '启用' : '停用' }}
                    </span>
                  </span>
                  <p>{{ role.description || '暂无角色说明' }}</p>
                  <span class="role-card-meta">
                    <span>{{ role.permissionIds.length }} 项权限</span>
                    <span>{{ role.userCount }} 位成员</span>
                    <span>{{ role.builtIn ? '内置角色' : '自定义' }}</span>
                  </span>
                </button>
                <div v-if="filteredRoles.length === 0" class="compact-empty">没有匹配的角色</div>
              </div>
            </aside>

            <section class="surface permission-editor">
              <template v-if="selectedRole">
                <header class="selected-role-head">
                  <div class="selected-role-copy">
                    <span class="role-avatar large">{{ selectedRole.name.slice(0, 1) }}</span>
                    <div>
                      <span class="section-label">当前角色</span>
                      <h3>{{ selectedRole.name }}</h3>
                      <p>{{ selectedRole.description || '暂无角色说明' }}</p>
                    </div>
                  </div>
                  <div class="role-head-meta">
                    <span
                      class="status-chip"
                      :class="selectedRole.status === 'ACTIVE' ? 'active' : 'disabled'"
                    >
                      {{ selectedRole.status === 'ACTIVE' ? '已启用' : '已停用' }}
                    </span>
                    <code>{{ selectedRole.code }}</code>
                  </div>
                </header>

                <div class="lifecycle-bar" :class="{ protected: !canEditSelectedRole }">
                  <div>
                    <strong>
                      {{ canEditSelectedRole ? '自定义角色生命周期' : '内置角色受到系统保护' }}
                    </strong>
                    <span>
                      {{
                        canEditSelectedRole
                          ? '可修改角色资料、启停状态与权限配置。'
                          : '内置角色不可编辑、停用或删除，权限仍可查看。'
                      }}
                    </span>
                  </div>
                  <div class="lifecycle-actions">
                    <button
                      type="button"
                      class="ghost-button"
                      :disabled="!canEditSelectedRole"
                      @click="openEditRole"
                    >
                      编辑资料
                    </button>
                    <button type="button" class="ghost-button" @click="openCopyRole">
                      复制角色
                    </button>
                    <button
                      type="button"
                      class="ghost-button"
                      :disabled="!canEditSelectedRole || saving === 'role-status'"
                      @click="toggleSelectedRoleStatus"
                    >
                      {{ selectedRole.status === 'ACTIVE' ? '停用角色' : '启用角色' }}
                    </button>
                    <button
                      type="button"
                      class="ghost-button danger"
                      :disabled="!canEditSelectedRole || saving === 'role-delete'"
                      @click="removeSelectedRole"
                    >
                      删除角色
                    </button>
                  </div>
                </div>

                <section class="data-scope-panel" :class="{ protected: !canEditSelectedRole }">
                  <header>
                    <div>
                      <span class="section-label">数据权限</span>
                      <h3>角色可见数据范围</h3>
                      <p>控制该角色在列表和单条资源详情中可访问的业务数据。</p>
                    </div>
                    <span class="scope-current">
                      {{ dataScopeLabel(selectedRole.dataScope) }}
                    </span>
                  </header>
                  <div class="scope-controls">
                    <label>
                      <span>数据范围</span>
                      <select
                        v-model="draftRoleDataScope"
                        :disabled="!canEditSelectedRole || saving === 'role-data-scope'"
                      >
                        <option value="SELF">仅本人</option>
                        <option value="DEPT_ONLY">本部门</option>
                        <option value="DEPT_AND_CHILD">本部门及下级部门</option>
                        <option value="DEPT_CUSTOM">指定部门</option>
                        <option value="ALL">全部数据</option>
                      </select>
                    </label>
                    <button
                      type="button"
                      class="primary-button compact"
                      :disabled="
                        !canEditSelectedRole || !roleDataScopeDirty || saving === 'role-data-scope'
                      "
                      @click="saveRoleDataScope"
                    >
                      {{ saving === 'role-data-scope' ? '保存中…' : '保存数据范围' }}
                    </button>
                  </div>
                  <div v-if="draftRoleDataScope === 'DEPT_CUSTOM'" class="scope-department-grid">
                    <label
                      v-for="department in overview.departments"
                      :key="department.id"
                      class="scope-department"
                      :class="{
                        checked: draftRoleDataScopeDepartmentIds.includes(department.id),
                        disabled: !canEditSelectedRole || department.status !== 'ACTIVE'
                      }"
                    >
                      <input
                        type="checkbox"
                        :checked="draftRoleDataScopeDepartmentIds.includes(department.id)"
                        :disabled="!canEditSelectedRole || department.status !== 'ACTIVE'"
                        @change="toggleRoleDataScopeDepartment(department.id, $event)"
                      />
                      <span class="checkbox-visual">✓</span>
                      <span>
                        <strong>{{ department.name }}</strong>
                        <small>{{ department.code }}</small>
                      </span>
                    </label>
                  </div>
                  <p v-if="!canEditSelectedRole" class="scope-note">
                    内置角色由系统保护，只能查看当前数据范围。
                  </p>
                </section>

                <div class="permission-toolbar">
                  <div class="permission-overview">
                    <span class="section-label">权限配置</span>
                    <strong>
                      {{ selectedPermissionCount }} / {{ assignablePermissions.length }}
                    </strong>
                    <small>已选择权限</small>
                    <div class="coverage-track">
                      <i :style="{ width: `${rolePermissionPercent}%` }" />
                    </div>
                  </div>
                  <div class="permission-tools">
                    <label class="search-box permission-search">
                      <span>⌕</span>
                      <input
                        v-model="permissionSearch"
                        type="search"
                        aria-label="搜索权限"
                        placeholder="搜索权限名称或权限码"
                      />
                    </label>
                    <button type="button" class="secondary-button" @click="openCreatePermission">
                      ＋ 新增权限
                    </button>
                  </div>
                </div>

                <div class="package-bar">
                  <span>快速套用权限包</span>
                  <button
                    v-for="permissionPackage in overview.permissionPackages"
                    :key="permissionPackage.id"
                    type="button"
                    class="package-chip"
                    :disabled="isSuperAdminRole"
                    :title="permissionPackage.description || permissionPackage.name"
                    @click="applyPermissionPackage(permissionPackage)"
                  >
                    {{ permissionPackage.name }}
                    <b>{{ permissionPackage.permissionIds.length }}</b>
                  </button>
                </div>

                <div v-if="roleDirty" class="change-preview">
                  <div>
                    <strong>变更预览</strong>
                    <span>
                      新增 {{ roleAddedPermissions.length }} 项，移除
                      {{ roleRemovedPermissions.length }} 项
                    </span>
                  </div>
                  <div class="change-groups">
                    <span
                      v-for="permission in roleAddedPermissions.slice(0, 5)"
                      :key="`add-${permission.id}`"
                      class="change-chip added"
                    >
                      + {{ permission.name }}
                    </span>
                    <span
                      v-for="permission in roleRemovedPermissions.slice(0, 5)"
                      :key="`remove-${permission.id}`"
                      class="change-chip removed"
                    >
                      − {{ permission.name }}
                    </span>
                    <span
                      v-if="roleAddedPermissions.length + roleRemovedPermissions.length > 10"
                      class="change-chip more"
                    >
                      还有
                      {{ roleAddedPermissions.length + roleRemovedPermissions.length - 10 }}
                      项
                    </span>
                  </div>
                  <p v-if="highRiskAdditions.length > 0" class="high-risk-note">
                    包含 {{ highRiskAdditions.length }} 项高风险权限，保存前需要二次确认。
                  </p>
                </div>

                <div class="permission-scroll">
                  <section
                    v-for="group in filteredPermissionGroups"
                    :key="group.module"
                    class="permission-group"
                  >
                    <header class="group-head">
                      <button
                        type="button"
                        class="group-collapse"
                        @click="toggleGroupCollapse(group.module)"
                      >
                        <span :class="{ collapsed: isGroupCollapsed(group.module) }">⌄</span>
                        <span>
                          <strong>{{ group.module }}</strong>
                          <small>
                            {{ groupSelectedCount(group.permissions) }} /
                            {{ group.permissions.filter((item) => item.type !== 'CATALOG').length }}
                            项已选
                          </small>
                        </span>
                      </button>
                      <label
                        class="group-toggle"
                        :class="{ disabled: groupDisabled(group.permissions) }"
                      >
                        <input
                          type="checkbox"
                          :checked="groupChecked(group.permissions)"
                          :disabled="groupDisabled(group.permissions)"
                          @change="toggleGroup(group.permissions, $event)"
                        />
                        <span class="checkbox-visual">✓</span>
                        全选
                      </label>
                    </header>
                    <div v-if="!isGroupCollapsed(group.module)" class="permission-grid">
                      <label
                        v-for="permission in group.permissions"
                        :key="permission.id"
                        class="permission-item"
                        :class="{
                          disabled: permissionDisabled(permission),
                          checked: draftPermissionIds.includes(permission.id),
                          catalog: permission.type === 'CATALOG',
                          inactive: permission.status !== 'ACTIVE'
                        }"
                        :style="{ '--permission-depth': permissionDepth(permission) }"
                      >
                        <input
                          type="checkbox"
                          :checked="draftPermissionIds.includes(permission.id)"
                          :disabled="permissionDisabled(permission)"
                          @change="togglePermission(permission, $event)"
                        />
                        <span class="checkbox-visual">✓</span>
                        <span class="permission-copy">
                          <strong>{{ permission.name }}</strong>
                          <code>
                            {{ permission.code }}
                            <template v-if="permission.routePath">
                              · {{ permission.routePath }}
                            </template>
                          </code>
                          <small>{{ permission.description }}</small>
                        </span>
                        <span class="permission-badges">
                          <span class="type-chip">{{ permissionTypeLabel(permission.type) }}</span>
                          <span v-if="permission.code === 'admin.rbac.manage'" class="locked-chip">
                            超级管理员专属
                          </span>
                          <span v-else-if="permission.builtIn" class="builtin-chip">内置</span>
                          <span
                            v-if="permission.riskLevel !== 'LOW'"
                            class="risk-chip"
                            :class="permission.riskLevel.toLowerCase()"
                          >
                            {{ permission.riskLevel === 'HIGH' ? '高风险' : '中风险' }}
                          </span>
                          <button
                            v-if="!permission.builtIn"
                            type="button"
                            class="permission-delete"
                            :disabled="saving === 'permission-delete'"
                            @click.stop.prevent="removePermission(permission)"
                          >
                            删除
                          </button>
                        </span>
                      </label>
                    </div>
                  </section>
                  <div v-if="filteredPermissionGroups.length === 0" class="empty-state">
                    <strong>没有匹配的权限</strong>
                    <p>尝试更换关键词，或清空搜索条件。</p>
                  </div>
                </div>

                <footer class="save-bar">
                  <div>
                    <strong v-if="isSuperAdminRole">超级管理员固定拥有全部权限</strong>
                    <strong v-else-if="roleDirty">存在尚未保存的权限变更</strong>
                    <strong v-else>权限配置已同步</strong>
                    <span>
                      {{
                        isSuperAdminRole
                          ? '该角色的权限配置不可取消。'
                          : roleDirty
                            ? `已选择 ${selectedPermissionCount} 项权限，保存后立即生效。`
                            : '切换角色前无需额外操作。'
                      }}
                    </span>
                  </div>
                  <button
                    type="button"
                    class="primary-button"
                    :disabled="saving === 'role' || isSuperAdminRole || !roleDirty"
                    @click="saveRolePermissions"
                  >
                    {{ saving === 'role' ? '保存中…' : '保存权限配置' }}
                  </button>
                </footer>
              </template>
              <div v-else class="empty-state large">
                <strong>请选择一个角色</strong>
                <p>从左侧角色目录选择后即可配置权限。</p>
              </div>
            </section>
          </div>
        </template>

        <template v-else-if="activeSection === 'departments'">
          <header class="content-heading">
            <div>
              <span>Organization Management</span>
              <h2>部门与组织归属</h2>
              <p>维护部门树、负责人和成员归属，为角色数据范围提供组织边界。</p>
            </div>
            <button type="button" class="primary-button" @click="openCreateDepartment()">
              <span>＋</span>
              新建根部门
            </button>
          </header>

          <div class="department-layout">
            <aside class="surface department-directory">
              <header class="surface-head">
                <div>
                  <span>组织架构</span>
                  <h3>部门树</h3>
                </div>
                <b>{{ overview.departments.length }}</b>
              </header>
              <div class="search-box">
                <span>⌕</span>
                <input
                  v-model="departmentSearch"
                  type="search"
                  aria-label="搜索部门"
                  placeholder="搜索部门名称或编码"
                />
              </div>
              <div class="department-tree">
                <button
                  v-for="node in departmentTree"
                  :key="node.department.id"
                  type="button"
                  class="department-node"
                  :class="{
                    selected: node.department.id === selectedDepartmentId,
                    inactive: node.department.status !== 'ACTIVE'
                  }"
                  :style="{ '--department-depth': node.depth }"
                  @click="selectDepartment(node.department)"
                >
                  <span class="tree-branch">{{ node.depth === 0 ? '◆' : '└' }}</span>
                  <span>
                    <strong>{{ node.department.name }}</strong>
                    <small>{{ node.department.code }}</small>
                  </span>
                  <b>{{ node.department.memberIds.length }}</b>
                </button>
                <div v-if="departmentTree.length === 0" class="compact-empty">没有匹配的部门</div>
              </div>
            </aside>

            <section class="surface department-editor">
              <template v-if="selectedDepartment">
                <header class="selected-role-head">
                  <div class="selected-role-copy">
                    <span class="role-avatar large">
                      {{ selectedDepartment.name.slice(0, 1) }}
                    </span>
                    <div>
                      <span class="section-label">当前部门</span>
                      <h3>{{ selectedDepartment.name }}</h3>
                      <p>{{ selectedDepartment.code }}</p>
                    </div>
                  </div>
                  <div class="role-head-meta">
                    <span
                      class="status-chip"
                      :class="selectedDepartment.status === 'ACTIVE' ? 'active' : 'disabled'"
                    >
                      {{ selectedDepartment.status === 'ACTIVE' ? '已启用' : '已停用' }}
                    </span>
                    <code>#{{ selectedDepartment.id }}</code>
                  </div>
                </header>

                <div class="lifecycle-bar">
                  <div>
                    <strong>部门生命周期</strong>
                    <span>存在子部门时不能删除；停用后不再参与数据范围计算。</span>
                  </div>
                  <div class="lifecycle-actions">
                    <button
                      type="button"
                      class="ghost-button"
                      @click="openCreateDepartment(selectedDepartment.id)"
                    >
                      新建子部门
                    </button>
                    <button type="button" class="ghost-button" @click="openEditDepartment">
                      编辑部门
                    </button>
                    <button
                      type="button"
                      class="ghost-button"
                      :disabled="saving === 'department-status'"
                      @click="toggleSelectedDepartmentStatus"
                    >
                      {{ selectedDepartment.status === 'ACTIVE' ? '停用部门' : '启用部门' }}
                    </button>
                    <button
                      type="button"
                      class="ghost-button danger"
                      :disabled="saving === 'department-delete'"
                      @click="removeSelectedDepartment"
                    >
                      删除部门
                    </button>
                  </div>
                </div>

                <div class="member-summary department-summary">
                  <article>
                    <span>直属成员</span>
                    <strong>{{ selectedDepartment.memberIds.length }}</strong>
                  </article>
                  <article>
                    <span>子部门</span>
                    <strong>
                      {{
                        overview.departments.filter(
                          (department) => department.parentId === selectedDepartment?.id
                        ).length
                      }}
                    </strong>
                  </article>
                  <article>
                    <span>负责人</span>
                    <strong class="text-value">
                      {{
                        selectedDepartment.leaderUserId
                          ? userName(selectedDepartment.leaderUserId)
                          : '未设置'
                      }}
                    </strong>
                  </article>
                  <article>
                    <span>排序值</span>
                    <strong>{{ selectedDepartment.sortOrder }}</strong>
                  </article>
                </div>

                <div class="department-member-panel">
                  <header>
                    <div>
                      <span class="section-label">成员维护</span>
                      <h3>选择直属部门成员</h3>
                      <p>一个用户可以属于多个部门，移除这里的关系不会删除账号。</p>
                    </div>
                    <button
                      type="button"
                      class="secondary-button"
                      :disabled="!departmentMemberDirty || saving === 'department-members'"
                      @click="saveDepartmentMembers"
                    >
                      {{ saving === 'department-members' ? '保存中…' : '保存成员' }}
                    </button>
                  </header>
                  <div class="department-member-grid">
                    <label
                      v-for="user in overview.users"
                      :key="user.id"
                      class="department-member"
                      :class="{
                        checked: draftDepartmentMemberIds.includes(user.id)
                      }"
                    >
                      <input
                        type="checkbox"
                        :checked="draftDepartmentMemberIds.includes(user.id)"
                        @change="toggleDepartmentMember(user.id, $event)"
                      />
                      <span class="checkbox-visual">✓</span>
                      <span class="member-avatar">
                        {{ user.username.slice(0, 1).toUpperCase() }}
                      </span>
                      <span>
                        <strong>{{ user.nickname || user.username }}</strong>
                        <small>{{ user.email }}</small>
                      </span>
                    </label>
                  </div>
                </div>
              </template>
              <div v-else class="empty-state large">
                <strong>请选择一个部门</strong>
                <p>从左侧部门树选择后即可维护组织信息。</p>
              </div>
            </section>
          </div>
        </template>

        <template v-else-if="activeSection === 'users'">
          <header class="content-heading">
            <div>
              <span>Member Authorization</span>
              <h2>成员授权</h2>
              <p>选择成员并分配角色。停用角色不会继续贡献有效权限。</p>
            </div>
            <span class="member-total">{{ filteredUsers.length }} 位成员</span>
          </header>

          <div class="user-layout">
            <aside class="surface user-directory">
              <header class="surface-head">
                <div>
                  <span>成员目录</span>
                  <h3>全部用户</h3>
                </div>
                <b>{{ filteredUsers.length }}</b>
              </header>
              <div class="search-box">
                <span>⌕</span>
                <input
                  v-model="userSearch"
                  type="search"
                  aria-label="搜索成员"
                  placeholder="搜索账号、昵称、邮箱或角色"
                />
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
                  <span class="member-avatar">{{ user.username.slice(0, 1).toUpperCase() }}</span>
                  <span class="member-copy">
                    <span>
                      <strong>{{ user.nickname || user.username }}</strong>
                      <code>{{ user.username }}</code>
                    </span>
                    <small>{{ user.email }}</small>
                    <span class="role-chips">
                      <i v-for="roleId in user.roleIds" :key="roleId">{{ roleName(roleId) }}</i>
                      <em v-if="user.roleIds.length === 0">未分配角色</em>
                    </span>
                    <span class="department-chips">
                      <i v-for="departmentId in user.departmentIds" :key="departmentId">
                        {{ departmentName(departmentId) }}
                      </i>
                      <em v-if="user.departmentIds.length === 0">未分配部门</em>
                    </span>
                  </span>
                  <span
                    class="status-dot"
                    :class="user.status === 'ACTIVE' ? 'active' : 'disabled'"
                  />
                </button>
                <div v-if="filteredUsers.length === 0" class="compact-empty">没有匹配的成员</div>
              </div>
            </aside>

            <section class="surface member-editor">
              <template v-if="selectedUser">
                <header class="member-profile">
                  <span class="member-avatar large">
                    {{ selectedUser.username.slice(0, 1).toUpperCase() }}
                  </span>
                  <div>
                    <span class="section-label">当前成员</span>
                    <h3>{{ selectedUser.nickname || selectedUser.username }}</h3>
                    <p>{{ selectedUser.email }}</p>
                  </div>
                  <div class="member-profile-meta">
                    <code>{{ selectedUser.username }}</code>
                    <span
                      class="status-chip"
                      :class="selectedUser.status === 'ACTIVE' ? 'active' : 'disabled'"
                    >
                      {{ selectedUser.status === 'ACTIVE' ? '账号正常' : '账号停用' }}
                    </span>
                  </div>
                </header>

                <div class="member-summary">
                  <article>
                    <span>当前角色</span>
                    <strong>{{ selectedUser.roleIds.length }}</strong>
                  </article>
                  <article>
                    <span>待保存变更</span>
                    <strong>{{ userDirty ? '有' : '无' }}</strong>
                  </article>
                  <article>
                    <span>所属部门</span>
                    <strong>{{ selectedUser.departmentIds.length }}</strong>
                  </article>
                </div>

                <div class="department-assignment">
                  <header>
                    <div>
                      <span class="section-label">组织归属</span>
                      <h3>设置成员所属部门</h3>
                    </div>
                    <span>主部门用于新资源的默认归属</span>
                  </header>
                  <div class="department-option-grid">
                    <label
                      v-for="department in overview.departments"
                      :key="department.id"
                      class="department-option"
                      :class="{
                        checked: draftUserDepartmentIds.includes(department.id),
                        disabled: department.status !== 'ACTIVE'
                      }"
                    >
                      <input
                        type="checkbox"
                        :checked="draftUserDepartmentIds.includes(department.id)"
                        :disabled="department.status !== 'ACTIVE'"
                        @change="toggleUserDepartment(department.id, $event)"
                      />
                      <span class="checkbox-visual">✓</span>
                      <span>
                        <strong>{{ department.name }}</strong>
                        <small>
                          {{ department.code }} ·
                          {{ department.status === 'ACTIVE' ? '启用' : '停用' }}
                        </small>
                      </span>
                    </label>
                  </div>
                  <div class="department-save-row">
                    <span>
                      {{ userDepartmentDirty ? '部门归属有未保存变更' : '部门归属已同步' }}
                    </span>
                    <button
                      type="button"
                      class="secondary-button"
                      :disabled="!userDepartmentDirty || saving === 'user-departments'"
                      @click="saveUserDepartments"
                    >
                      {{ saving === 'user-departments' ? '保存中…' : '保存部门归属' }}
                    </button>
                  </div>
                </div>

                <div class="role-assignment">
                  <header>
                    <div>
                      <span class="section-label">角色分配</span>
                      <h3>为该成员选择角色</h3>
                    </div>
                    <span>支持多角色，权限取并集</span>
                  </header>
                  <div class="role-option-grid">
                    <label
                      v-for="role in overview.roles"
                      :key="role.id"
                      class="role-option"
                      :class="{
                        checked: draftRoleIds.includes(role.id),
                        disabled: role.status !== 'ACTIVE' && !draftRoleIds.includes(role.id)
                      }"
                    >
                      <input
                        type="checkbox"
                        :checked="draftRoleIds.includes(role.id)"
                        :disabled="role.status !== 'ACTIVE' && !draftRoleIds.includes(role.id)"
                        @change="toggleRole(role, $event)"
                      />
                      <span class="checkbox-visual">✓</span>
                      <span class="role-option-copy">
                        <span>
                          <strong>{{ role.name }}</strong>
                          <code>{{ role.code }}</code>
                        </span>
                        <small>{{ role.description || '暂无角色说明' }}</small>
                        <em>
                          {{ role.builtIn ? '内置角色' : '自定义角色' }} ·
                          {{ role.status === 'ACTIVE' ? '启用' : '停用' }}
                        </em>
                      </span>
                    </label>
                  </div>
                </div>

                <footer class="save-bar">
                  <div>
                    <strong>{{ userDirty ? '成员角色有未保存变更' : '成员角色已同步' }}</strong>
                    <span>
                      {{
                        userDirty
                          ? '保存后，该成员下一次请求将使用新的角色权限。'
                          : '可继续选择其他成员进行授权。'
                      }}
                    </span>
                  </div>
                  <button
                    type="button"
                    class="primary-button"
                    :disabled="saving === 'user' || !userDirty"
                    @click="saveUserRoles"
                  >
                    {{ saving === 'user' ? '保存中…' : '保存成员角色' }}
                  </button>
                </footer>
              </template>
              <div v-else class="empty-state large">
                <strong>请选择一个成员</strong>
                <p>从左侧成员目录选择后即可分配角色。</p>
              </div>
            </section>
          </div>
        </template>

        <template v-else>
          <header class="content-heading">
            <div>
              <span>Authorization Audit</span>
              <h2>授权变更记录</h2>
              <p>记录角色、权限和成员授权操作，便于追踪权限变化。</p>
            </div>
            <button
              type="button"
              class="secondary-button"
              :disabled="auditLoading"
              @click="loadAuditLogs"
            >
              {{ auditLoading ? '刷新中…' : '刷新记录' }}
            </button>
          </header>

          <section class="surface audit-panel">
            <header class="surface-head">
              <div>
                <span>最近操作</span>
                <h3>授权审计日志</h3>
              </div>
              <b>{{ auditLogs.length }}</b>
            </header>

            <div v-if="auditLoading" class="state-card audit-state">
              <span class="spinner" />
              <div>
                <strong>正在读取授权记录</strong>
                <p>正在加载最近的权限和角色变更。</p>
              </div>
            </div>

            <div v-else-if="auditLogs.length > 0" class="audit-list">
              <article v-for="log in auditLogs" :key="log.id" class="audit-row">
                <span class="audit-action">{{ auditActionLabel(log.action) }}</span>
                <div class="audit-main">
                  <div class="audit-heading">
                    <strong>{{ auditTargetLabel(log) }}</strong>
                    <time>{{ formatAuditTime(log.createTime) }}</time>
                  </div>
                  <p>
                    操作人：{{ log.operatorName || '未知' }}
                    <span v-if="log.clientIp"> · IP {{ log.clientIp }}</span>
                  </p>
                  <details v-if="log.beforeValue || log.afterValue">
                    <summary>查看变更内容</summary>
                    <div class="audit-diff">
                      <div>
                        <span>变更前</span>
                        <pre>{{ log.beforeValue || '无' }}</pre>
                      </div>
                      <div>
                        <span>变更后</span>
                        <pre>{{ log.afterValue || '无' }}</pre>
                      </div>
                    </div>
                  </details>
                </div>
                <code>#{{ log.id }}</code>
              </article>
            </div>

            <div v-else class="empty-state large">
              <strong>暂无授权变更记录</strong>
              <p>保存角色权限或成员角色后，记录会显示在这里。</p>
            </div>
          </section>
        </template>
      </main>
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
            <span>{{ roleDialogMode === 'create' ? 'NEW ROLE' : 'EDIT ROLE' }}</span>
            <h2>{{ roleDialogMode === 'create' ? '新建自定义角色' : '编辑角色资料' }}</h2>
            <p>角色编码用于系统识别，创建后不可修改。</p>
          </div>
          <button type="button" aria-label="关闭" @click="closeRoleDialog">×</button>
        </header>
        <label>
          <span>角色编码</span>
          <input
            v-model.trim="roleDefinitionForm.code"
            :disabled="roleDialogMode === 'edit'"
            required
            minlength="2"
            maxlength="50"
            pattern="[A-Z][A-Z0-9_]*"
            placeholder="例如 OPERATOR"
          />
          <small>仅支持大写字母、数字和下划线，并以字母开头。</small>
        </label>
        <label>
          <span>角色名称</span>
          <input
            v-model.trim="roleDefinitionForm.name"
            required
            maxlength="100"
            placeholder="例如 运营人员"
          />
        </label>
        <label>
          <span>角色说明</span>
          <textarea
            v-model.trim="roleDefinitionForm.description"
            rows="4"
            maxlength="500"
            placeholder="说明该角色的职责和适用范围"
          />
        </label>
        <p v-if="roleDialogMode === 'create'" class="dialog-note">
          新角色默认没有任何权限，创建后请在权限矩阵中继续配置。
        </p>
        <p v-if="roleDefinitionError" class="dialog-error">{{ roleDefinitionError }}</p>
        <footer>
          <button type="button" class="secondary-button" @click="closeRoleDialog">取消</button>
          <button type="submit" class="primary-button" :disabled="savingRoleDefinition">
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

    <div
      v-if="permissionDialogOpen"
      class="dialog-backdrop"
      role="presentation"
      @click.self="closePermissionDialog"
    >
      <form class="role-dialog" @submit.prevent="savePermissionDefinition">
        <header>
          <div>
            <span>NEW PERMISSION</span>
            <h2>新增权限点</h2>
            <p>权限点用于 Controller 方法上的功能授权校验。</p>
          </div>
          <button type="button" aria-label="关闭" @click="closePermissionDialog">×</button>
        </header>
        <label>
          <span>上级权限</span>
          <select v-model="permissionForm.parentId" @change="syncPermissionModule">
            <option :value="undefined">无上级权限</option>
            <option v-for="parent in permissionParentOptions" :key="parent.id" :value="parent.id">
              {{ parent.module }} / {{ parent.name }}（{{ permissionTypeLabel(parent.type) }}）
            </option>
          </select>
        </label>
        <label>
          <span>所属模块</span>
          <input
            v-model.trim="permissionForm.module"
            required
            maxlength="100"
            placeholder="例如 报表中心"
          />
        </label>
        <label>
          <span>权限类型</span>
          <select v-model="permissionForm.type">
            <option value="MENU">菜单</option>
            <option value="PAGE">页面</option>
            <option value="BUTTON">按钮</option>
            <option value="API">接口</option>
          </select>
        </label>
        <label>
          <span>权限编码</span>
          <input
            v-model.trim="permissionForm.code"
            required
            maxlength="100"
            pattern="[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*"
            placeholder="例如 report.export"
          />
          <small>使用小写字母、数字、下划线，多个层级之间用点分隔。</small>
        </label>
        <label>
          <span>权限名称</span>
          <input
            v-model.trim="permissionForm.name"
            required
            maxlength="100"
            placeholder="例如 导出报表"
          />
        </label>
        <label>
          <span>前端路由</span>
          <input
            v-model.trim="permissionForm.routePath"
            maxlength="200"
            placeholder="例如 /admin/reports"
          />
          <small>菜单和页面类型建议填写，按钮和接口权限可以留空。</small>
        </label>
        <label>
          <span>风险等级</span>
          <select v-model="permissionForm.riskLevel">
            <option value="LOW">低风险</option>
            <option value="MEDIUM">中风险</option>
            <option value="HIGH">高风险</option>
          </select>
        </label>
        <label>
          <span>权限说明</span>
          <textarea
            v-model.trim="permissionForm.description"
            rows="4"
            maxlength="500"
            placeholder="说明该权限允许执行的操作"
          />
        </label>
        <p class="dialog-note">新权限会自动授予超级管理员，默认不会分配给其他角色。</p>
        <p v-if="permissionDialogError" class="dialog-error">{{ permissionDialogError }}</p>
        <footer>
          <button type="button" class="secondary-button" @click="closePermissionDialog">
            取消
          </button>
          <button type="submit" class="primary-button" :disabled="savingPermission">
            {{ savingPermission ? '创建中…' : '创建权限' }}
          </button>
        </footer>
      </form>
    </div>

    <div
      v-if="copyDialogOpen"
      class="dialog-backdrop"
      role="presentation"
      @click.self="closeCopyRoleDialog"
    >
      <form class="role-dialog" @submit.prevent="saveCopyRole">
        <header>
          <div>
            <span>COPY ROLE</span>
            <h2>复制角色</h2>
            <p>基于“{{ selectedRole?.name }}”创建新的自定义角色。</p>
          </div>
          <button type="button" aria-label="关闭" @click="closeCopyRoleDialog">×</button>
        </header>
        <label>
          <span>新角色编码</span>
          <input
            v-model.trim="copyRoleForm.code"
            required
            minlength="2"
            maxlength="50"
            pattern="[A-Z][A-Z0-9_]*"
            placeholder="例如 OPERATOR_COPY"
          />
        </label>
        <label>
          <span>新角色名称</span>
          <input
            v-model.trim="copyRoleForm.name"
            required
            maxlength="100"
            placeholder="例如 运营人员副本"
          />
        </label>
        <label>
          <span>角色说明</span>
          <textarea
            v-model.trim="copyRoleForm.description"
            rows="3"
            maxlength="500"
            placeholder="说明该角色的职责范围"
          />
        </label>
        <label class="copy-option">
          <input v-model="copyRoleForm.copyPermissions" type="checkbox" />
          <span class="checkbox-visual">✓</span>
          复制源角色的权限和依赖权限
        </label>
        <label class="copy-option">
          <input v-model="copyRoleForm.copyMembers" type="checkbox" />
          <span class="checkbox-visual">✓</span>
          将源角色的成员同步加入新角色
        </label>
        <p v-if="copyRoleError" class="dialog-error">{{ copyRoleError }}</p>
        <footer>
          <button type="button" class="secondary-button" @click="closeCopyRoleDialog">取消</button>
          <button type="submit" class="primary-button" :disabled="savingCopyRole">
            {{ savingCopyRole ? '复制中…' : '确认复制' }}
          </button>
        </footer>
      </form>
    </div>

    <div
      v-if="departmentDialogMode"
      class="dialog-backdrop"
      role="presentation"
      @click.self="closeDepartmentDialog"
    >
      <form class="role-dialog" @submit.prevent="saveDepartmentDefinition">
        <header>
          <div>
            <span>
              {{ departmentDialogMode === 'create' ? 'NEW DEPARTMENT' : 'EDIT DEPARTMENT' }}
            </span>
            <h2>
              {{ departmentDialogMode === 'create' ? '新建部门' : '编辑部门资料' }}
            </h2>
            <p>部门层级决定“本部门及下级部门”数据范围的覆盖边界。</p>
          </div>
          <button type="button" aria-label="关闭" @click="closeDepartmentDialog">×</button>
        </header>
        <label>
          <span>上级部门</span>
          <select v-model="departmentForm.parentId">
            <option :value="undefined">无（根部门）</option>
            <option
              v-for="department in departmentParentOptions"
              :key="department.id"
              :value="department.id"
            >
              {{ department.name }} · {{ department.code }}
            </option>
          </select>
        </label>
        <label>
          <span>部门编码</span>
          <input
            v-model.trim="departmentForm.code"
            required
            minlength="2"
            maxlength="50"
            pattern="[A-Z][A-Z0-9_]*"
            :disabled="departmentDialogMode === 'edit'"
            placeholder="例如 PRODUCT_CENTER"
          />
        </label>
        <label>
          <span>部门名称</span>
          <input
            v-model.trim="departmentForm.name"
            required
            maxlength="100"
            placeholder="例如 产品中心"
          />
        </label>
        <label>
          <span>部门负责人</span>
          <select v-model="departmentForm.leaderUserId">
            <option :value="undefined">暂不设置</option>
            <option v-for="user in overview?.users ?? []" :key="user.id" :value="user.id">
              {{ user.nickname || user.username }} · {{ user.email }}
            </option>
          </select>
        </label>
        <label v-if="departmentDialogMode === 'edit'">
          <span>部门状态</span>
          <select v-model="departmentForm.status">
            <option value="ACTIVE">启用</option>
            <option value="DISABLED">停用</option>
          </select>
        </label>
        <label>
          <span>排序值</span>
          <input v-model.number="departmentForm.sortOrder" type="number" min="0" max="9999" />
        </label>
        <p v-if="departmentDialogError" class="dialog-error">
          {{ departmentDialogError }}
        </p>
        <footer>
          <button type="button" class="secondary-button" @click="closeDepartmentDialog">
            取消
          </button>
          <button type="submit" class="primary-button" :disabled="savingDepartment">
            {{ savingDepartment ? '保存中…' : '保存部门' }}
          </button>
        </footer>
      </form>
    </div>
  </section>
</template>

<style scoped>
.access-page {
  --accent: #2459c6;
  --accent-soft: #eef4ff;
  --ink: #172033;
  --muted: #687386;
  --line: #e2e7ee;
  --surface: #ffffff;
  --canvas: #f5f7fa;
  display: grid;
  gap: 20px;
  max-width: 1560px;
  margin: 0 auto;
  color: #344054;
  font-size: 13px;
}

.access-page *,
.access-page *::before,
.access-page *::after {
  box-sizing: border-box;
}

.page-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  padding: 8px 0 22px;
  border-bottom: 1px solid var(--line);
}

.eyebrow {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #7a8494;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.eyebrow-mark {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 7px;
  background: #17233b;
  color: #fff;
  font-size: 10px;
  letter-spacing: 0;
}

.page-hero h1 {
  margin: 12px 0 8px;
  color: var(--ink);
  font-size: 32px;
  line-height: 1.15;
  letter-spacing: -0.035em;
}

.page-hero p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.system-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 13px;
  border: 1px solid #cfe4d5;
  border-radius: 8px;
  background: #f4fbf6;
  color: #267247;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.system-status i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22a45d;
  box-shadow: 0 0 0 4px rgb(34 164 93 / 12%);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  position: relative;
  display: grid;
  gap: 6px;
  min-height: 112px;
  padding: 18px 20px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface);
  box-shadow: 0 1px 2px rgb(16 24 40 / 3%);
}

.metric-card::after {
  position: absolute;
  top: -30px;
  right: -20px;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: #f0f4fb;
  content: '';
}

.metric-card.accent::after {
  background: #fff4df;
}

.metric-card > span,
.metric-card > small {
  position: relative;
  z-index: 1;
  color: #7d8797;
  font-size: 11px;
}

.metric-card > span {
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.metric-card > strong {
  position: relative;
  z-index: 1;
  color: #253047;
  font-size: 28px;
  line-height: 1.1;
}

.metric-card.accent > strong {
  color: #a86516;
}

.feedback {
  margin: 0;
  padding: 11px 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  font-size: 12px;
}

.feedback.success {
  border-color: #b9dfc5;
  background: #f4fbf6;
  color: #24663f;
}

.feedback.error {
  border-color: #efc0c5;
  background: #fff7f7;
  color: #a1323d;
}

.state-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 120px;
  padding: 24px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
}

.state-card strong {
  color: #344054;
}

.state-card p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid #ccd7e8;
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.control-shell {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.section-nav {
  position: sticky;
  top: 20px;
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(16 24 40 / 3%);
}

.nav-caption {
  padding: 4px 8px 8px;
  color: #939ba8;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.nav-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  width: 100%;
  min-height: 64px;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 9px;
  background: transparent;
  color: #4f5b6d;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    background 0.16s ease,
    color 0.16s ease;
}

.nav-item:hover {
  background: #f7f9fc;
}

.nav-item.active {
  border-color: #b8c9e9;
  background: var(--accent-soft);
  color: #244f9e;
  box-shadow: inset 3px 0 var(--accent);
}

.nav-icon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  background: #edf1f6;
  color: #566174;
  font-size: 11px;
  font-weight: 800;
}

.nav-item.active .nav-icon {
  background: #dce8ff;
  color: #2459c6;
}

.nav-item > span:nth-child(2) {
  display: grid;
  gap: 3px;
}

.nav-item strong {
  font-size: 13px;
}

.nav-item small {
  color: #8791a0;
  font-size: 10px;
  line-height: 1.4;
}

.nav-item > i {
  color: #9ca4af;
  font-size: 18px;
  font-style: normal;
}

.nav-note {
  margin-top: 8px;
  padding: 12px;
  border: 1px solid #ece4d3;
  border-radius: 8px;
  background: #fffbf2;
}

.nav-note strong {
  color: #815819;
  font-size: 11px;
}

.nav-note p {
  margin: 5px 0 0;
  color: #927545;
  font-size: 10px;
  line-height: 1.55;
}

.section-content {
  display: grid;
  min-width: 0;
  gap: 14px;
}

.content-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 2px 2px 4px;
}

.content-heading > div {
  display: grid;
  gap: 3px;
}

.content-heading > div > span,
.section-label {
  color: #929aa7;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.content-heading h2 {
  margin: 2px 0;
  color: #263148;
  font-size: 21px;
  letter-spacing: -0.02em;
}

.content-heading p {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
}

.member-total {
  padding: 7px 11px;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: #fff;
  color: #687385;
  font-size: 11px;
  font-weight: 700;
}

.primary-button,
.secondary-button,
.ghost-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    background 0.16s ease,
    color 0.16s ease;
}

.primary-button {
  border: 1px solid #2459c6;
  background: #2459c6;
  color: #fff;
  box-shadow: 0 5px 14px rgb(36 89 198 / 16%);
}

.primary-button:hover:not(:disabled) {
  border-color: #194caf;
  background: #194caf;
}

.secondary-button {
  border: 1px solid #d7dde6;
  background: #fff;
  color: #596476;
}

.secondary-button:hover:not(:disabled) {
  border-color: #aebbd0;
  color: #334155;
}

.ghost-button {
  min-height: 31px;
  padding: 0 10px;
  border: 1px solid #d8dee7;
  background: #fff;
  color: #5f6a7b;
}

.ghost-button:hover:not(:disabled) {
  border-color: #9fb1ce;
  color: #285ba9;
}

.ghost-button.danger {
  border-color: #edc8cc;
  color: #a43d47;
}

.ghost-button.danger:hover:not(:disabled) {
  border-color: #dc9da4;
  background: #fff8f8;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.52;
}

.role-layout,
.user-layout {
  display: grid;
  grid-template-columns: 292px minmax(0, 1fr);
  gap: 14px;
  min-height: 680px;
  align-items: stretch;
}

.user-layout {
  grid-template-columns: 360px minmax(0, 1fr);
}

.surface {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface);
  box-shadow: 0 1px 2px rgb(16 24 40 / 3%);
}

.role-directory,
.user-directory {
  display: flex;
  flex-direction: column;
}

.surface-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 76px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
}

.surface-head > div {
  display: grid;
  gap: 3px;
}

.surface-head span {
  color: #929aa7;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.09em;
  text-transform: uppercase;
}

.surface-head h3 {
  margin: 0;
  color: #2f3a4f;
  font-size: 15px;
}

.surface-head > b {
  display: grid;
  min-width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 7px;
  background: #f0f3f7;
  color: #697486;
  font-size: 11px;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  margin: 12px;
}

.search-box > span {
  position: absolute;
  left: 11px;
  color: #98a1ae;
  font-size: 16px;
  pointer-events: none;
}

.search-box input {
  width: 100%;
  height: 36px;
  padding: 0 12px 0 33px;
  border: 1px solid #dce2ea;
  border-radius: 7px;
  outline: none;
  background: #fbfcfe;
  color: #334155;
  font-size: 12px;
}

.search-box input:focus {
  border-color: #86a5df;
  background: #fff;
  box-shadow: 0 0 0 3px rgb(36 89 198 / 8%);
}

.role-list,
.user-list {
  display: grid;
  align-content: start;
  gap: 7px;
  min-height: 0;
  max-height: 575px;
  padding: 0 12px 12px;
  overflow-y: auto;
}

.role-card {
  display: grid;
  gap: 8px;
  width: 100%;
  padding: 12px;
  border: 1px solid #e5e9ef;
  border-radius: 9px;
  background: #fff;
  color: #344054;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    background 0.16s ease;
}

.role-card:hover {
  border-color: #b9c8e3;
  background: #f9fbff;
}

.role-card.selected {
  border-color: #7d9fdf;
  background: #f2f7ff;
  box-shadow: inset 3px 0 var(--accent);
}

.role-card.super.selected {
  border-color: #d4ad6c;
  background: #fff9ee;
  box-shadow: inset 3px 0 #bf8125;
}

.role-card.inactive {
  background: #fafafb;
}

.role-card-head {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
}

.role-avatar,
.member-avatar {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 8px;
  background: #e7eefb;
  color: #315eaf;
  font-size: 12px;
  font-weight: 800;
}

.role-avatar.large,
.member-avatar.large {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  font-size: 15px;
}

.role-title {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.role-title strong {
  overflow: hidden;
  color: #303b4f;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-title code,
.member-copy code,
.member-profile-meta code {
  color: #8c96a5;
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 9px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 7px;
  border-radius: 5px;
  font-size: 9px;
  font-weight: 700;
  white-space: nowrap;
}

.status-chip.active {
  background: #eaf7ef;
  color: #267447;
}

.status-chip.disabled {
  background: #f1f3f5;
  color: #7c8491;
}

.role-card > p {
  margin: 0;
  overflow: hidden;
  color: #7a8493;
  font-size: 10px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-card-meta {
  display: flex;
  gap: 8px;
  padding-top: 7px;
  border-top: 1px solid #edf0f4;
}

.role-card-meta span {
  color: #84909f;
  font-size: 9px;
}

.role-card-meta span:not(:last-child)::after {
  margin-left: 8px;
  color: #d0d6de;
  content: '·';
}

.compact-empty {
  padding: 36px 16px;
  color: #929aa7;
  font-size: 12px;
  text-align: center;
}

.permission-editor,
.member-editor {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.member-editor {
  overflow: visible;
}

.selected-role-head,
.member-profile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 88px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--line);
}

.selected-role-copy,
.member-profile > div:nth-child(2) {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 12px;
}

.selected-role-copy > div,
.member-profile > div:nth-child(2) {
  display: grid;
  gap: 2px;
}

.selected-role-copy h3,
.member-profile h3 {
  margin: 0;
  color: #293449;
  font-size: 17px;
}

.selected-role-copy p,
.member-profile p {
  margin: 0;
  color: #7c8695;
  font-size: 11px;
}

.role-head-meta,
.member-profile-meta {
  display: flex;
  align-items: center;
  gap: 9px;
}

.lifecycle-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 12px 18px;
  border-bottom: 1px solid #e7ebf0;
  background: #f7f9fc;
}

.lifecycle-bar.protected {
  background: #fffaf1;
}

.lifecycle-bar > div:first-child {
  display: grid;
  gap: 3px;
}

.lifecycle-bar strong {
  color: #4b5668;
  font-size: 11px;
}

.lifecycle-bar span {
  color: #8992a0;
  font-size: 10px;
}

.lifecycle-actions {
  display: flex;
  gap: 7px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.permission-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--line);
}

.permission-overview {
  display: grid;
  grid-template-columns: auto auto;
  align-items: baseline;
  gap: 2px 9px;
  min-width: 210px;
}

.permission-overview .section-label {
  grid-column: 1 / -1;
}

.permission-overview strong {
  color: #2d58a8;
  font-size: 19px;
}

.permission-overview small {
  color: #84909f;
  font-size: 10px;
}

.coverage-track {
  grid-column: 1 / -1;
  height: 4px;
  margin-top: 5px;
  overflow: hidden;
  border-radius: 999px;
  background: #e8edf5;
}

.coverage-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #3d72d8;
  transition: width 0.2s ease;
}

.permission-search {
  width: min(360px, 100%);
  margin: 0;
}

.permission-tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  width: min(470px, 58%);
}

.permission-tools .secondary-button {
  flex: 0 0 auto;
  min-height: 36px;
  white-space: nowrap;
}

.permission-scroll {
  display: grid;
  flex: 1;
  align-content: start;
  gap: 10px;
  min-height: 420px;
  max-height: 535px;
  padding: 14px 18px;
  overflow-y: auto;
  background: #fafbfc;
}

.permission-group {
  overflow: hidden;
  border: 1px solid #e4e8ee;
  border-radius: 9px;
  background: #fff;
}

.group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: 50px;
  padding: 9px 13px;
  background: #f8f9fb;
}

.group-collapse {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #344054;
  text-align: left;
  cursor: pointer;
}

.group-collapse > span:first-child {
  display: grid;
  width: 23px;
  height: 23px;
  place-items: center;
  border-radius: 6px;
  background: #eaf0fa;
  color: #45679f;
  font-size: 14px;
  transition: transform 0.16s ease;
}

.group-collapse > span:first-child.collapsed {
  transform: rotate(-90deg);
}

.group-collapse > span:last-child {
  display: grid;
  gap: 2px;
}

.group-collapse strong {
  color: #38445a;
  font-size: 12px;
}

.group-collapse small {
  color: #8a94a3;
  font-size: 9px;
}

.group-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #6b7687;
  font-size: 10px;
  cursor: pointer;
  user-select: none;
}

.group-toggle.disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.permission-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.permission-item {
  position: relative;
  display: grid;
  grid-template-columns: 17px minmax(0, 1fr);
  gap: 9px;
  min-height: 70px;
  padding: 11px 13px 11px calc(13px + var(--permission-depth, 0) * 16px);
  border-right: 1px solid #edf0f3;
  border-bottom: 1px solid #edf0f3;
  background: #fff;
  cursor: pointer;
}

.permission-item.catalog {
  background: #f7f9fc;
}

.permission-item.inactive {
  background: #f5f5f6;
}

.permission-item:nth-child(2n) {
  border-right: 0;
}

.permission-item:hover {
  background: #f9fbff;
}

.permission-item.checked {
  background: #f5f9ff;
}

.permission-item.disabled {
  background: #fafafa;
  cursor: not-allowed;
  opacity: 0.62;
}

.permission-item > input,
.group-toggle > input,
.role-option > input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.checkbox-visual {
  display: grid;
  width: 17px;
  height: 17px;
  place-items: center;
  border: 1px solid #c9d1dc;
  border-radius: 4px;
  background: #fff;
  color: transparent;
  font-size: 10px;
  font-weight: 800;
}

.permission-item.checked .checkbox-visual,
.role-option.checked .checkbox-visual,
.scope-department.checked .checkbox-visual,
.department-option.checked .checkbox-visual,
.department-member.checked .checkbox-visual,
.group-toggle input:checked + .checkbox-visual {
  border-color: #3268c9;
  background: #3268c9;
  color: #fff;
}

.permission-copy {
  display: grid;
  min-width: 0;
  gap: 2px;
  padding-right: 132px;
}

.permission-copy strong {
  color: #354056;
  font-size: 11px;
}

.permission-copy code {
  color: #6d7f9a;
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 9px;
}

.permission-copy small {
  overflow: hidden;
  color: #929ba9;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.permission-badges {
  position: absolute;
  top: 9px;
  right: 10px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.locked-chip,
.builtin-chip,
.type-chip {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 8px;
  white-space: nowrap;
}

.locked-chip {
  background: #fff3de;
  color: #99611c;
}

.builtin-chip {
  background: #eef2f7;
  color: #687385;
}

.type-chip {
  background: #edf4ff;
  color: #3764ad;
}

.risk-chip {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 8px;
  font-weight: 700;
  white-space: nowrap;
}

.risk-chip.medium {
  background: #fff6e6;
  color: #9a6519;
}

.risk-chip.high {
  background: #fff0f1;
  color: #b23842;
}

.permission-delete {
  padding: 2px 7px;
  border: 1px solid #edc8cc;
  border-radius: 4px;
  background: #fff;
  color: #a43d47;
  font-size: 8px;
  cursor: pointer;
}

.permission-delete:hover:not(:disabled) {
  border-color: #dc9da4;
  background: #fff8f8;
}

.save-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 68px;
  margin-top: auto;
  padding: 12px 18px;
  border-top: 1px solid var(--line);
  background: #fff;
}

.save-bar > div {
  display: grid;
  gap: 3px;
}

.save-bar strong {
  color: #465164;
  font-size: 11px;
}

.save-bar span {
  color: #8791a0;
  font-size: 10px;
}

.user-list {
  gap: 6px;
}

.user-row {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 8px;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 11px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: #fff;
  color: #344054;
  text-align: left;
  cursor: pointer;
}

.user-row:hover {
  background: #f8faff;
}

.user-row.selected {
  border-color: #b6c8e8;
  background: #f2f7ff;
}

.member-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
}

.member-copy {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.member-copy > span:first-child {
  display: flex;
  align-items: baseline;
  min-width: 0;
  gap: 7px;
}

.member-copy strong {
  overflow: hidden;
  color: #344054;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-copy small {
  overflow: hidden;
  color: #8a94a2;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 2px;
}

.role-chips i,
.role-chips em {
  padding: 2px 5px;
  border-radius: 4px;
  background: #eef2f7;
  color: #667181;
  font-size: 8px;
  font-style: normal;
}

.role-chips em {
  background: #fff7e8;
  color: #93631d;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.status-dot.active {
  background: #29a361;
}

.status-dot.disabled {
  background: #aab1bb;
}

.member-profile {
  justify-content: flex-start;
}

.member-profile-meta {
  margin-left: auto;
}

.member-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  border-bottom: 1px solid var(--line);
  background: var(--line);
}

.member-summary article {
  display: grid;
  gap: 4px;
  padding: 15px 18px;
  background: #fafbfc;
}

.member-summary span {
  color: #8993a2;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.member-summary strong {
  color: #344054;
  font-size: 19px;
}

.role-assignment {
  min-height: 0;
  padding: 18px;
}

.role-assignment > header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.role-assignment > header > div {
  display: grid;
  gap: 2px;
}

.role-assignment h3 {
  margin: 0;
  color: #344054;
  font-size: 15px;
}

.role-assignment > header > span {
  color: #8a94a3;
  font-size: 10px;
}

.role-option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
  max-height: clamp(260px, calc(100vh - 360px), 480px);
  padding: 2px 6px 2px 2px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.role-option {
  position: relative;
  display: grid;
  grid-template-columns: 17px minmax(0, 1fr);
  gap: 9px;
  min-height: 88px;
  padding: 12px;
  border: 1px solid #e4e8ee;
  border-radius: 9px;
  background: #fff;
  cursor: pointer;
}

.role-option:hover {
  border-color: #b9c8e1;
  background: #f9fbff;
}

.role-option.checked {
  border-color: #91addf;
  background: #f3f8ff;
}

.role-option.disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.role-option-copy {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.role-option-copy > span {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.role-option-copy strong {
  color: #3a4558;
  font-size: 11px;
}

.role-option-copy code {
  color: #8b95a4;
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 8px;
}

.role-option-copy small {
  overflow: hidden;
  color: #8993a2;
  font-size: 9px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-option-copy em {
  color: #7f8a9a;
  font-size: 8px;
  font-style: normal;
}

.empty-state {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 5px;
  min-height: 180px;
  color: #8993a2;
  text-align: center;
}

.empty-state strong {
  color: #566174;
  font-size: 13px;
}

.empty-state p {
  margin: 0;
  font-size: 11px;
}

.empty-state.large {
  flex: 1;
  min-height: 540px;
}

.dialog-backdrop {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: grid;
  padding: 24px;
  place-items: center;
  background: rgb(15 23 42 / 38%);
  backdrop-filter: blur(4px);
}

.role-dialog {
  display: grid;
  gap: 15px;
  width: min(500px, 100%);
  padding: 24px;
  border: 1px solid #dfe5ec;
  border-radius: 13px;
  background: #fff;
  box-shadow: 0 28px 80px rgb(15 23 42 / 26%);
}

.role-dialog > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf0f4;
}

.role-dialog > header > div {
  display: grid;
  gap: 3px;
}

.role-dialog > header span {
  color: #8e98a7;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.role-dialog h2 {
  margin: 2px 0 0;
  color: #293449;
  font-size: 20px;
}

.role-dialog > header p {
  margin: 2px 0 0;
  color: #7f8998;
  font-size: 11px;
}

.role-dialog > header button {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border: 0;
  border-radius: 7px;
  background: #f1f3f6;
  color: #697486;
  font-size: 20px;
  cursor: pointer;
}

.role-dialog > label {
  display: grid;
  gap: 7px;
}

.role-dialog > label > span {
  color: #4f5a6c;
  font-size: 11px;
  font-weight: 700;
}

.role-dialog input,
.role-dialog textarea,
.role-dialog select {
  width: 100%;
  padding: 10px 11px;
  border: 1px solid #d8dee7;
  border-radius: 7px;
  outline: none;
  color: #2e394d;
  font: inherit;
}

.role-dialog input {
  height: 39px;
}

.role-dialog select {
  height: 39px;
  background: #fff;
}

.role-dialog textarea {
  resize: vertical;
  line-height: 1.6;
}

.role-dialog input:focus,
.role-dialog textarea:focus,
.role-dialog select:focus {
  border-color: #83a3dc;
  box-shadow: 0 0 0 3px rgb(36 89 198 / 8%);
}

.audit-panel {
  min-height: 640px;
}

.audit-state {
  border: 0;
  border-radius: 0;
}

.audit-list {
  display: grid;
}

.audit-row {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: start;
  padding: 16px 18px;
  border-bottom: 1px solid #edf0f4;
}

.audit-row:last-child {
  border-bottom: 0;
}

.audit-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  padding: 0 8px;
  border-radius: 6px;
  background: #eef4ff;
  color: #2f5fae;
  font-size: 10px;
  font-weight: 700;
}

.audit-main {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.audit-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 14px;
}

.audit-heading strong {
  color: #344054;
  font-size: 12px;
}

.audit-heading time,
.audit-main p {
  color: #8c95a2;
  font-size: 10px;
}

.audit-main p {
  margin: 0;
}

.audit-row > code {
  color: #9aa2ae;
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 9px;
}

.audit-main details {
  margin-top: 6px;
}

.audit-main summary {
  color: #3b67b0;
  font-size: 10px;
  cursor: pointer;
}

.audit-diff {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 8px;
}

.audit-diff > div {
  min-width: 0;
  padding: 9px;
  border: 1px solid #e6eaf0;
  border-radius: 7px;
  background: #fafbfc;
}

.audit-diff span {
  color: #8993a1;
  font-size: 9px;
  font-weight: 700;
}

.audit-diff pre {
  max-height: 180px;
  margin: 6px 0 0;
  overflow: auto;
  color: #4c586b;
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 9px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.role-dialog input:disabled {
  background: #f2f4f7;
  color: #8a93a0;
}

.role-dialog label small {
  color: #9099a7;
  font-size: 9px;
}

.dialog-note,
.dialog-error {
  margin: 0;
  padding: 9px 11px;
  border-radius: 7px;
  font-size: 10px;
  line-height: 1.6;
}

.dialog-note {
  background: #f5f7fa;
  color: #75808f;
}

.dialog-error {
  border: 1px solid #efc2c7;
  background: #fff7f8;
  color: #a1323d;
}

.role-dialog > footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 2px;
}

.package-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  padding: 11px 18px;
  border-bottom: 1px solid var(--line);
  background: #fff;
}

.package-bar > span {
  margin-right: 2px;
  color: #7e8998;
  font-size: 10px;
  font-weight: 700;
}

.package-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  padding: 0 9px;
  border: 1px solid #d8e2f2;
  border-radius: 999px;
  background: #f7faff;
  color: #3e64a2;
  font-size: 9px;
  cursor: pointer;
}

.package-chip b {
  display: grid;
  min-width: 17px;
  height: 17px;
  place-items: center;
  border-radius: 50%;
  background: #e6eefc;
  font-size: 8px;
}

.change-preview {
  display: grid;
  gap: 8px;
  padding: 12px 18px;
  border-bottom: 1px solid #eadfca;
  background: #fffbf3;
}

.change-preview > div:first-child {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.change-preview strong {
  color: #6e5120;
  font-size: 11px;
}

.change-preview > div:first-child span {
  color: #927647;
  font-size: 10px;
}

.change-groups {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.change-chip {
  padding: 3px 7px;
  border-radius: 5px;
  font-size: 9px;
}

.change-chip.added {
  background: #eaf7ef;
  color: #267447;
}

.change-chip.removed {
  background: #fff0f1;
  color: #a53d47;
}

.change-chip.more {
  background: #f1f3f6;
  color: #6e7888;
}

.high-risk-note {
  margin: 0;
  color: #a1323d;
  font-size: 10px;
}

.role-dialog > label.copy-option {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #4f5a6c;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
}

.copy-option > input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.copy-option input:checked + .checkbox-visual {
  border-color: #3268c9;
  background: #3268c9;
  color: #fff;
}

.data-scope-panel {
  margin: 14px;
  padding: 16px;
  border: 1px solid #dfe5ee;
  border-radius: 10px;
  background: linear-gradient(135deg, #fbfcff, #f5f8fd);
}

.data-scope-panel.protected {
  background: #fafbfc;
}

.data-scope-panel > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.data-scope-panel > header > div {
  display: grid;
  gap: 3px;
}

.data-scope-panel h3 {
  margin: 0;
  color: #344054;
  font-size: 14px;
}

.data-scope-panel p {
  margin: 0;
  color: #7b8594;
  font-size: 10px;
}

.scope-current {
  padding: 4px 9px;
  border: 1px solid #cbd8ee;
  border-radius: 999px;
  background: #fff;
  color: #315b9f;
  font-size: 9px;
  font-weight: 700;
}

.scope-controls {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  margin-top: 13px;
}

.scope-controls > label {
  display: grid;
  flex: 1;
  gap: 6px;
  max-width: 340px;
}

.scope-controls label > span {
  color: #667085;
  font-size: 10px;
  font-weight: 700;
}

.scope-controls select,
.role-dialog select {
  min-height: 38px;
  padding: 0 12px;
  border: 1px solid #d5dce6;
  border-radius: 8px;
  background: #fff;
  color: #344054;
  font: inherit;
}

.primary-button.compact {
  min-height: 38px;
  padding: 0 15px;
}

.scope-department-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.scope-department,
.department-option,
.department-member {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 9px 10px;
  border: 1px solid #e1e6ee;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.scope-department > input,
.department-option > input,
.department-member > input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.scope-department.checked,
.department-option.checked,
.department-member.checked {
  border-color: #91addf;
  background: #f2f7ff;
}

.scope-department.disabled,
.department-option.disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.scope-department > span:last-child,
.department-option > span:last-child,
.department-member > span:last-child {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.scope-department strong,
.department-option strong,
.department-member strong {
  overflow: hidden;
  color: #344054;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scope-department small,
.department-option small,
.department-member small {
  overflow: hidden;
  color: #8993a2;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scope-note {
  margin-top: 10px !important;
  color: #93722b !important;
}

.department-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.department-chips i,
.department-chips em {
  padding: 2px 6px;
  border-radius: 4px;
  background: #f2f4f7;
  color: #667085;
  font-size: 8px;
  font-style: normal;
}

.department-chips em {
  color: #a06c18;
}

.department-layout {
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr);
  gap: 14px;
  min-height: 680px;
}

.department-directory {
  display: flex;
  flex-direction: column;
}

.department-tree {
  display: grid;
  align-content: start;
  gap: 3px;
  min-height: 0;
  padding: 8px;
  overflow-y: auto;
}

.department-node {
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 10px 10px calc(10px + var(--department-depth) * 15px);
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.department-node:hover {
  background: #f7f9fc;
}

.department-node.selected {
  border-color: #cad8ef;
  background: #eef4ff;
}

.department-node.inactive {
  opacity: 0.58;
}

.tree-branch {
  color: #9ca6b5;
  font-size: 9px;
}

.department-node > span:nth-child(2) {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.department-node strong {
  overflow: hidden;
  color: #344054;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.department-node small {
  color: #929ba8;
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 8px;
}

.department-node b {
  min-width: 21px;
  padding: 2px 6px;
  border-radius: 999px;
  background: #e7ebf1;
  color: #667085;
  font-size: 8px;
  text-align: center;
}

.department-editor {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.department-summary {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.department-summary strong.text-value {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.department-member-panel,
.department-assignment {
  padding: 18px;
  border-top: 1px solid var(--line);
}

.department-member-panel > header,
.department-assignment > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 13px;
}

.department-member-panel > header > div,
.department-assignment > header > div {
  display: grid;
  gap: 3px;
}

.department-member-panel h3,
.department-assignment h3 {
  margin: 0;
  color: #344054;
  font-size: 15px;
}

.department-member-panel p,
.department-assignment > header > span {
  margin: 0;
  color: #8993a2;
  font-size: 9px;
}

.department-member-grid,
.department-option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  max-height: 310px;
  padding: 2px 5px 2px 2px;
  overflow-y: auto;
}

.department-member .member-avatar {
  flex: 0 0 auto;
  width: 28px;
  height: 28px;
  font-size: 10px;
}

.department-save-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e1e6ee;
  color: #7e8897;
  font-size: 9px;
}

@media (max-width: 1180px) {
  .control-shell {
    grid-template-columns: 190px minmax(0, 1fr);
  }

  .role-layout {
    grid-template-columns: 255px minmax(0, 1fr);
  }

  .user-layout {
    grid-template-columns: 300px minmax(0, 1fr);
  }

  .department-layout {
    grid-template-columns: 280px minmax(0, 1fr);
  }

  .permission-grid,
  .role-option-grid {
    grid-template-columns: 1fr;
  }

  .role-option-grid {
    max-height: none;
    padding-right: 2px;
  }

  .permission-item {
    border-right: 0;
  }
}

@media (max-width: 900px) {
  .page-hero,
  .lifecycle-bar,
  .permission-toolbar,
  .save-bar,
  .content-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .control-shell,
  .role-layout,
  .user-layout,
  .department-layout {
    grid-template-columns: 1fr;
  }

  .section-nav {
    position: static;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .nav-caption,
  .nav-note {
    grid-column: 1 / -1;
  }

  .role-list,
  .user-list {
    max-height: 340px;
  }

  .permission-search {
    width: 100%;
  }

  .permission-tools {
    width: 100%;
  }

  .scope-controls,
  .scope-department-grid,
  .department-option-grid,
  .department-member-grid,
  .department-summary {
    grid-template-columns: 1fr;
  }

  .scope-controls {
    align-items: stretch;
    flex-direction: column;
  }

  .scope-controls > label {
    max-width: none;
  }

  .department-member-panel > header,
  .department-assignment > header,
  .department-save-row {
    align-items: stretch;
    flex-direction: column;
  }

  .audit-row {
    grid-template-columns: 1fr;
  }

  .audit-diff {
    grid-template-columns: 1fr;
  }
}
</style>

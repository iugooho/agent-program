import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  createPermission,
  copyRole,
  deletePermission,
  fetchRbacAuditLogs,
  fetchRbacOverview
} from '@/api/rbac'
import RbacAdminView from '@/views/RbacAdminView.vue'

vi.mock('@/api/rbac', () => {
  class MockRbacApiError extends Error {
    constructor(
      readonly status: number,
      message: string
    ) {
      super(message)
    }
  }

  return {
    RbacApiError: MockRbacApiError,
    fetchRbacOverview: vi.fn(),
    fetchRbacAuditLogs: vi.fn(),
    createPermission: vi.fn(),
    createDepartment: vi.fn(),
    copyRole: vi.fn(),
    deleteDepartment: vi.fn(),
    deletePermission: vi.fn(),
    createRole: vi.fn(),
    updateDepartment: vi.fn(),
    updateDepartmentMembers: vi.fn(),
    updateRole: vi.fn(),
    updateRoleDataScope: vi.fn(),
    updateRoleStatus: vi.fn(),
    deleteRole: vi.fn(),
    updateRolePermissions: vi.fn(),
    updateUserDepartments: vi.fn(),
    updateUserRoles: vi.fn()
  }
})

const mockedFetchRbacOverview = vi.mocked(fetchRbacOverview)
const mockedFetchRbacAuditLogs = vi.mocked(fetchRbacAuditLogs)
const mockedCreatePermission = vi.mocked(createPermission)
const mockedCopyRole = vi.mocked(copyRole)
const mockedDeletePermission = vi.mocked(deletePermission)

describe('RbacAdminView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedFetchRbacAuditLogs.mockResolvedValue([])
    mockedFetchRbacOverview.mockResolvedValue({
      roles: [
        {
          id: 1,
          code: 'USER',
          name: '普通用户',
          description: '使用基础功能',
          builtIn: true,
          status: 'ACTIVE',
          userCount: 1,
          permissionIds: [1],
          dataScope: 'SELF',
          dataScopeDepartmentIds: []
        },
        {
          id: 3,
          code: 'SUPER_ADMIN',
          name: '超级管理员',
          description: '管理系统权限',
          builtIn: true,
          status: 'ACTIVE',
          userCount: 1,
          permissionIds: [1, 2],
          dataScope: 'ALL',
          dataScopeDepartmentIds: []
        }
      ],
      permissionGroups: [
        {
          module: '行程规划',
          permissions: [
            {
              id: 1,
              code: 'trip.create',
              name: '创建行程',
              module: '行程规划',
              type: 'API',
              routePath: '/trips',
              description: '发起旅行规划',
              status: 'ACTIVE',
              riskLevel: 'LOW',
              builtIn: true
            },
            {
              id: 2,
              code: 'admin.rbac.manage',
              name: '管理角色权限',
              module: '行程规划',
              type: 'PAGE',
              routePath: '/admin/rbac',
              description: '维护角色与权限',
              status: 'ACTIVE',
              riskLevel: 'HIGH',
              builtIn: true
            },
            {
              id: 3,
              code: 'report.export',
              name: '导出报表',
              module: '行程规划',
              type: 'BUTTON',
              description: '导出行程报表',
              status: 'ACTIVE',
              riskLevel: 'LOW',
              builtIn: false
            }
          ]
        }
      ],
      permissionPackages: [
        {
          id: 1,
          code: 'TRIP_OPERATOR',
          name: '行程操作包',
          description: '行程操作权限',
          permissionIds: [1]
        }
      ],
      permissionDependencies: [
        {
          permissionId: 2,
          requiredPermissionId: 1
        }
      ],
      departments: [
        {
          id: 1,
          code: 'HEADQUARTERS',
          name: '总部',
          status: 'ACTIVE',
          sortOrder: 10,
          memberIds: [1]
        },
        {
          id: 2,
          parentId: 1,
          code: 'TECH',
          name: '技术部',
          status: 'ACTIVE',
          sortOrder: 20,
          memberIds: []
        }
      ],
      users: [
        {
          id: 1,
          username: 'demo',
          nickname: '演示用户',
          email: 'demo@example.com',
          status: 'ACTIVE',
          roleIds: [1],
          roleCodes: ['USER'],
          departmentIds: [1]
        }
      ]
    })
  })

  it('在角色控制和成员授权之间切换右侧内容', async () => {
    const wrapper = mount(RbacAdminView)
    await flushPromises()

    expect(wrapper.text()).toContain('角色与权限')
    expect(wrapper.text()).toContain('权限配置')

    const memberNav = wrapper.findAll('.nav-item').find((item) => item.text().includes('成员授权'))
    expect(memberNav).toBeDefined()
    await memberNav!.trigger('click')

    expect(wrapper.text()).toContain('为该成员选择角色')
    expect(wrapper.text()).toContain('demo@example.com')
  })

  it('切换部门控制后显示部门树和组织归属', async () => {
    const wrapper = mount(RbacAdminView)
    await flushPromises()

    const departmentNav = wrapper
      .findAll('.nav-item')
      .find((item) => item.text().includes('部门控制'))
    expect(departmentNav).toBeDefined()
    await departmentNav!.trigger('click')

    expect(wrapper.text()).toContain('部门与组织归属')
    expect(wrapper.text()).toContain('技术部')
    expect(wrapper.text()).toContain('选择直属部门成员')
  })

  it('可以打开新增权限弹窗并提交', async () => {
    mockedCreatePermission.mockResolvedValue({
      id: 4,
      code: 'audit.export',
      name: '导出审计记录',
      module: '审计中心',
      type: 'BUTTON',
      description: '导出审计日志',
      status: 'ACTIVE',
      riskLevel: 'LOW',
      builtIn: false
    })
    const wrapper = mount(RbacAdminView)
    await flushPromises()

    const createButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('新增权限'))
    expect(createButton).toBeDefined()
    await createButton!.trigger('click')

    await wrapper.find('input[placeholder="例如 报表中心"]').setValue('审计中心')
    await wrapper.find('input[placeholder="例如 report.export"]').setValue('audit.export')
    await wrapper.find('input[placeholder="例如 导出报表"]').setValue('导出审计记录')
    await wrapper.find('form.role-dialog').trigger('submit')
    await flushPromises()

    expect(mockedCreatePermission).toHaveBeenCalledWith({
      code: 'audit.export',
      name: '导出审计记录',
      module: '审计中心',
      type: 'BUTTON',
      parentId: undefined,
      routePath: '',
      riskLevel: 'LOW',
      description: ''
    })
  })

  it('可以复制当前角色', async () => {
    mockedCopyRole.mockResolvedValue({
      id: 4,
      code: 'USER_COPY',
      name: '普通用户副本',
      description: '使用基础功能',
      builtIn: false,
      status: 'ACTIVE',
      userCount: 1,
      permissionIds: [1],
      dataScope: 'SELF',
      dataScopeDepartmentIds: []
    })
    const wrapper = mount(RbacAdminView)
    await flushPromises()

    const copyButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('复制角色'))
    expect(copyButton).toBeDefined()
    await copyButton!.trigger('click')
    await wrapper.find('form.role-dialog').trigger('submit')
    await flushPromises()

    expect(mockedCopyRole).toHaveBeenCalledWith(1, {
      code: 'USER_COPY',
      name: '普通用户副本',
      description: '使用基础功能',
      copyPermissions: true,
      copyMembers: true
    })
  })

  it('可以删除自定义权限', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true)
    const wrapper = mount(RbacAdminView)
    await flushPromises()

    await wrapper.find('.permission-delete').trigger('click')
    await flushPromises()

    expect(mockedDeletePermission).toHaveBeenCalledWith(3)
    confirm.mockRestore()
  })

  it('可以查看授权变更记录', async () => {
    mockedFetchRbacAuditLogs.mockResolvedValue([
      {
        id: 1,
        operatorId: 2,
        operatorName: 'admin',
        targetType: 'ROLE',
        targetId: 1,
        action: 'ROLE_PERMISSIONS_UPDATE',
        beforeValue: '{"permissionIds":[1]}',
        afterValue: '{"permissionIds":[1,2]}',
        clientIp: '127.0.0.1',
        createTime: '2026-11-24T10:00:00'
      }
    ])
    const wrapper = mount(RbacAdminView)
    await flushPromises()

    const auditNav = wrapper.findAll('.nav-item').find((item) => item.text().includes('变更记录'))
    expect(auditNav).toBeDefined()
    await auditNav!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('授权变更记录')
    expect(wrapper.text()).toContain('调整角色权限')
    expect(wrapper.text()).toContain('操作人：admin')
  })
})

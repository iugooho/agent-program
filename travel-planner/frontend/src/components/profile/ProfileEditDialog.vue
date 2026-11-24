<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'

import type { UserResponse } from '@/api/schema'
import { updateCurrentUser, uploadAvatar } from '@/api/users'

const props = defineProps<{
  open: boolean
  user: UserResponse
}>()

const emit = defineEmits<{
  'update:open': [open: boolean]
  saved: [user: UserResponse]
}>()

const dialogRef = ref<HTMLElement | null>(null)
const nicknameRef = ref<HTMLInputElement | null>(null)
const saving = ref(false)
const uploading = ref(false)
const error = ref('')
const avatarUrl = ref('')
let previousFocus: HTMLElement | null = null
let closeAfterSave = false

const form = reactive({
  nickname: '',
  phone: '',
  bio: ''
})
const baseline = reactive({
  nickname: '',
  phone: '',
  bio: ''
})

const avatarText = computed(
  () => (form.nickname || props.user.username).trim().slice(0, 1).toUpperCase() || 'U'
)
const dirty = computed(
  () =>
    form.nickname.trim() !== baseline.nickname.trim() ||
    form.phone.trim() !== baseline.phone.trim() ||
    form.bio.trim() !== baseline.bio.trim()
)

watch(
  () => props.open,
  async (open) => {
    if (open) {
      resetForm()
      previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
      document.body.style.overflow = 'hidden'
      await nextTick()
      nicknameRef.value?.focus()
      return
    }
    document.body.style.overflow = ''
    closeAfterSave = false
    previousFocus?.focus()
    previousFocus = null
  },
  { immediate: true }
)

watch(
  () => props.user,
  (user) => {
    avatarUrl.value = resolveAvatarUrl(user.avatarUrl)
    if (!props.open) {
      return
    }
    resetForm()
  },
  { deep: true }
)

onBeforeUnmount(() => {
  document.body.style.overflow = ''
})

async function save(): Promise<void> {
  error.value = ''
  if (!dirty.value) {
    requestClose()
    return
  }
  saving.value = true
  try {
    const updated = await updateCurrentUser({
      nickname: form.nickname.trim(),
      phone: form.phone.trim(),
      bio: form.bio.trim()
    })
    closeAfterSave = true
    emit('saved', updated)
    emit('update:open', false)
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '个人资料更新失败'
  } finally {
    saving.value = false
  }
}

async function handleAvatarChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  error.value = ''
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    error.value = '头像仅支持 JPEG 或 PNG'
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    error.value = '头像图片不能超过 2MB'
    return
  }
  uploading.value = true
  try {
    const updated = await uploadAvatar(file)
    avatarUrl.value = resolveAvatarUrl(updated.avatarUrl)
    emit('saved', updated)
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '头像上传失败'
  } finally {
    uploading.value = false
  }
}

function requestClose(): void {
  if (!closeAfterSave && dirty.value && !window.confirm('基础资料还有未保存的修改，确认关闭吗？')) {
    return
  }
  closeAfterSave = false
  emit('update:open', false)
}

function handleOverlayClick(event: MouseEvent): void {
  if (event.target === event.currentTarget) {
    requestClose()
  }
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') {
    event.preventDefault()
    requestClose()
    return
  }
  if (event.key !== 'Tab') {
    return
  }
  const focusable = dialogRef.value?.querySelectorAll<HTMLElement>(
    'button:not(:disabled), input:not(:disabled), textarea:not(:disabled), [href], [tabindex]:not([tabindex="-1"])'
  )
  if (!focusable || focusable.length === 0) {
    return
  }
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

function resetForm(): void {
  form.nickname = props.user.nickname
  form.phone = props.user.phone
  form.bio = props.user.bio
  baseline.nickname = form.nickname
  baseline.phone = form.phone
  baseline.bio = form.bio
  avatarUrl.value = resolveAvatarUrl(props.user.avatarUrl)
  error.value = ''
}

function resolveAvatarUrl(value: string): string {
  if (!value) {
    return ''
  }
  if (/^(https?:|data:|blob:)/.test(value)) {
    return value
  }
  const baseUrl = import.meta.env.VITE_API_BASE_URL
  if (baseUrl && /^https?:\/\//.test(baseUrl)) {
    return new URL(value, baseUrl).toString()
  }
  return value
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="dialog-overlay"
      @mousedown.self="handleOverlayClick"
      @keydown="handleKeydown"
    >
      <section
        ref="dialogRef"
        class="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="profile-edit-title"
      >
        <header class="dialog-header">
          <div>
            <p>基础资料</p>
            <h2 id="profile-edit-title">编辑个人资料</h2>
          </div>
          <button type="button" aria-label="关闭编辑资料弹窗" @click="requestClose">×</button>
        </header>

        <form @submit.prevent="save">
          <div class="avatar-editor">
            <div class="avatar-preview">
              <img v-if="avatarUrl" :src="avatarUrl" :alt="`${form.nickname}的头像`" />
              <span v-else aria-hidden="true">{{ avatarText }}</span>
            </div>
            <div class="avatar-copy">
              <strong>头像</strong>
              <span>支持 JPEG、PNG，原图不超过 2MB。</span>
              <label class="upload-button">
                {{ uploading ? '上传中…' : '更换头像' }}
                <input
                  type="file"
                  accept="image/jpeg,image/png"
                  :disabled="uploading"
                  @change="handleAvatarChange"
                />
              </label>
            </div>
          </div>

          <div class="field-grid">
            <label for="dialog-nickname">
              昵称
              <input
                id="dialog-nickname"
                ref="nicknameRef"
                v-model="form.nickname"
                maxlength="50"
                autocomplete="nickname"
                placeholder="请输入昵称"
              />
            </label>
            <label for="dialog-phone">
              联系电话
              <input
                id="dialog-phone"
                v-model="form.phone"
                maxlength="20"
                autocomplete="tel"
                placeholder="可选"
              />
            </label>
          </div>
          <label for="dialog-bio">
            个人简介
            <textarea
              id="dialog-bio"
              v-model="form.bio"
              maxlength="500"
              rows="4"
              placeholder="介绍一下你的旅行习惯或期待"
            />
          </label>
          <p class="field-hint">修改登录账号、邮箱或密码，请前往“账号与安全”分区完成邮箱验证。</p>
          <p v-if="error" class="message error" role="alert">{{ error }}</p>

          <footer class="dialog-actions">
            <button class="secondary-button" type="button" @click="requestClose">取消</button>
            <button class="primary-button" type="submit" :disabled="saving || uploading">
              {{ saving ? '保存中…' : '保存资料' }}
            </button>
          </footer>
        </form>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.dialog-overlay {
  position: fixed;
  z-index: 60;
  inset: 0;
  display: grid;
  overflow: auto;
  padding: 24px;
  place-items: center;
  background: rgb(15 23 42 / 45%);
}

.dialog {
  width: min(100%, 620px);
  max-height: calc(100vh - 48px);
  overflow: auto;
  border: 1px solid #dce3ec;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 24px 70px rgb(15 23 42 / 24%);
}

.dialog-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px 16px;
  border-bottom: 1px solid #edf0f3;
}

.dialog-header p {
  margin: 0 0 4px;
  color: #355f9c;
  font-size: 12px;
  font-weight: 700;
}

.dialog-header h2 {
  margin: 0;
  color: #172033;
  font-size: 20px;
}

.dialog-header button {
  display: grid;
  width: 32px;
  height: 32px;
  padding: 0;
  place-items: center;
  border: 1px solid #d6dde6;
  border-radius: 6px;
  background: #ffffff;
  color: #526071;
  font-size: 20px;
  cursor: pointer;
}

form {
  display: grid;
  gap: 16px;
  padding: 20px 22px 0;
}

.avatar-editor {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px;
  border: 1px solid #e2e7ee;
  border-radius: 6px;
  background: #fafbfc;
}

.avatar-preview {
  display: grid;
  flex: 0 0 auto;
  width: 64px;
  height: 64px;
  overflow: hidden;
  place-items: center;
  border-radius: 50%;
  background: #23395d;
  color: #ffffff;
  font-size: 24px;
  font-weight: 800;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-copy {
  display: grid;
  gap: 5px;
}

.avatar-copy strong {
  color: #344054;
  font-size: 13px;
}

.avatar-copy span,
.field-hint {
  color: #667085;
  font-size: 11px;
  line-height: 1.55;
}

.upload-button {
  justify-self: start;
  margin-top: 2px;
  padding: 6px 10px;
  border: 1px solid #c8d5e8;
  border-radius: 5px;
  background: #eef4fd;
  color: #285594;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.upload-button input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

label {
  display: grid;
  gap: 7px;
  color: #344054;
  font-size: 12px;
  font-weight: 700;
}

input,
textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 40px;
  padding: 9px 11px;
  border: 1px solid #cfd6df;
  border-radius: 6px;
  background: #ffffff;
  color: #172033;
  font: inherit;
  font-size: 13px;
}

textarea {
  resize: vertical;
  line-height: 1.6;
}

input:focus,
textarea:focus {
  border-color: #7b9dd7;
  box-shadow: 0 0 0 3px rgb(36 89 198 / 10%);
  outline: none;
}

.field-hint {
  margin: -6px 0 0;
}

.dialog-actions {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: flex-end;
  gap: 9px;
  margin: 2px -22px 0;
  padding: 14px 22px 18px;
  border-top: 1px solid #edf0f3;
  background: rgb(255 255 255 / 96%);
}

.primary-button,
.secondary-button {
  min-height: 40px;
  padding: 9px 15px;
  border-radius: 6px;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.primary-button {
  border: 1px solid #2459c6;
  background: #2459c6;
  color: #ffffff;
}

.secondary-button {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #344054;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.message {
  margin: 0;
  padding: 9px 11px;
  border-radius: 6px;
  font-size: 12px;
}

.message.error {
  border: 1px solid #f1c8cc;
  background: #fff7f8;
  color: #9d2633;
}

@media (max-width: 640px) {
  .dialog-overlay {
    align-items: end;
    padding: 0;
  }

  .dialog {
    width: 100%;
    max-height: 92vh;
    border-radius: 8px 8px 0 0;
  }

  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>

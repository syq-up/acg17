<template>
  <main class="account-page">
    <section class="account-panel">
      <header class="profile-header">
        <button class="profile-avatar" type="button" aria-label="更换头像" @click="openAvatarDialog">
          <img :src="user.avatarUrl" alt="用户头像">
          <span class="profile-avatar-overlay">更换头像</span>
        </button>
        <div class="profile-copy">
          <div class="profile-name">
            <h1>{{ displayName }}</h1>
            <button class="profile-edit" type="button" aria-label="修改昵称" title="修改昵称"
              @click="openNicknameDialog">
              <icon icon="#icon-edit"></icon>
            </button>
          </div>
          <p>@{{ user.username }}<span class="dot">·</span>加入于 {{ user.createTime || '—' }}</p>
        </div>
      </header>

      <section class="panel-section content-section">
        <div class="section-heading">
          <div>
            <h2>我的内容</h2>
            <p>内容数量及相关使用情况</p>
          </div>
        </div>

        <div class="stats-grid">
          <router-link v-for="stat in statCards" :key="stat.label" :to="stat.to" class="stat-card">
            <div class="stat-heading">
              <span class="stat-icon" :class="stat.tone"><icon :icon="stat.icon"></icon></span>
              <span class="stat-label">{{ stat.label }}</span>
              <icon class="stat-arrow" icon="#icon-right"></icon>
            </div>
            <div class="stat-value">
              <strong>{{ stat.value }}</strong>
              <span>{{ stat.unit }}</span>
            </div>
            <span class="stat-detail">{{ stat.detail }}</span>
          </router-link>
        </div>
      </section>

      <section class="panel-section security-section">
        <div class="section-heading">
          <div>
            <h2>账户安全</h2>
            <p>保护你的账户访问权限</p>
          </div>
        </div>
        <div class="security-row">
          <div>
            <span class="security-label">登录密码</span>
            <strong>••••••••</strong>
            <small>修改密码后，当前登录会话将失效</small>
          </div>
          <button class="setting-action" type="button" @click="openPasswordDialog">修改密码</button>
        </div>
      </section>
    </section>
  </main>

  <el-dialog v-model="dialogs.nickname" title="修改昵称" width="420px" @closed="resetNicknameForm">
    <form class="dialog-form" @submit.prevent="saveNickname">
      <label for="nickname-input">昵称</label>
      <input id="nickname-input" v-model="nicknameForm.value" type="text" maxlength="64"
        autocomplete="nickname" placeholder="请输入昵称" :disabled="nicknameForm.loading">
      <p v-if="nicknameForm.error" class="form-error">{{ nicknameForm.error }}</p>
      <div class="dialog-actions">
        <button type="button" class="dialog-button secondary" @click="dialogs.nickname = false">取消</button>
        <button type="submit" class="dialog-button primary" :disabled="nicknameForm.loading">
          {{ nicknameForm.loading ? '保存中…' : '保存' }}
        </button>
      </div>
    </form>
  </el-dialog>

  <el-dialog v-model="dialogs.avatar" title="更换头像" width="420px" @closed="resetAvatarForm">
    <div class="avatar-dialog">
      <div class="avatar-preview">
        <img :src="avatarForm.preview || user.avatarUrl" alt="头像预览">
      </div>
      <p class="dialog-hint">预览会以正方形显示，建议选择清晰的正面图片。</p>
      <input ref="avatarInput" class="hidden-file-input" type="file"
        accept="image/jpeg,image/png,image/gif,image/webp" @change="handleAvatarFile">
      <div class="avatar-file-actions">
        <button type="button" class="dialog-button secondary"
          :disabled="avatarForm.loading || avatarForm.processing" @click="chooseAvatar">选择图片</button>
        <button type="button" class="text-button" :disabled="avatarForm.loading || avatarForm.processing"
          @click="resetAvatar">恢复默认</button>
      </div>
      <p v-if="avatarForm.error" class="form-error">{{ avatarForm.error }}</p>
      <div class="dialog-actions">
        <button type="button" class="dialog-button secondary" @click="dialogs.avatar = false">取消</button>
        <button type="button" class="dialog-button primary"
          :disabled="avatarForm.loading || avatarForm.processing || !avatarForm.file"
          @click="saveAvatar">
          {{ avatarForm.processing ? '裁剪中…' : avatarForm.loading ? '上传中…' : '保存' }}
        </button>
      </div>
    </div>
  </el-dialog>

  <el-dialog v-model="dialogs.password" title="修改密码" width="420px" @closed="resetPasswordForm">
    <form class="dialog-form" @submit.prevent="savePassword">
      <label for="current-password-input">当前密码</label>
      <input id="current-password-input" v-model="passwordForm.current" type="password"
        autocomplete="current-password" maxlength="72" :disabled="passwordForm.loading">
      <label for="new-password-input">新密码</label>
      <input id="new-password-input" v-model="passwordForm.next" type="password"
        autocomplete="new-password" maxlength="72" placeholder="至少 8 个字符" :disabled="passwordForm.loading">
      <label for="confirm-password-input">确认新密码</label>
      <input id="confirm-password-input" v-model="passwordForm.confirm" type="password"
        autocomplete="new-password" maxlength="72" :disabled="passwordForm.loading">
      <p v-if="passwordForm.error" class="form-error">{{ passwordForm.error }}</p>
      <div class="dialog-actions">
        <button type="button" class="dialog-button secondary" @click="dialogs.password = false">取消</button>
        <button type="submit" class="dialog-button primary" :disabled="passwordForm.loading">
          {{ passwordForm.loading ? '提交中…' : '确认修改' }}
        </button>
      </div>
    </form>
  </el-dialog>
</template>

<script>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import server from '@/util/request'

const MAX_AVATAR_SIZE = 5 * 1024 * 1024

export default {
  name: 'Account',
  setup() {
    const store = useStore()
    const router = useRouter()
    const avatarInput = ref(null)
    const dialogs = reactive({
      nickname: false,
      avatar: false,
      password: false,
    })
    const nicknameForm = reactive({ value: '', loading: false, error: '' })
    const avatarForm = reactive({
      file: null,
      preview: '',
      objectUrl: '',
      processing: false,
      loading: false,
      error: '',
    })
    const passwordForm = reactive({ current: '', next: '', confirm: '', loading: false, error: '' })

    const user = computed(() => store.state.userInfo)
    const displayName = computed(() => user.value.nickname || user.value.username || '用户')
    const statCards = computed(() => [
      {
        label: '插画',
        value: formatNumber(user.value.illustrationCount),
        unit: '张',
        detail: `占用 ${formatBytes(user.value.illustrationStorageBytes)}`,
        icon: '#icon-illustration',
        tone: 'blue',
        to: { name: 'Illustration' },
      },
      {
        label: '漫画',
        value: formatNumber(user.value.mangaCount),
        unit: '部',
        detail: `占用 ${formatBytes(user.value.mangaStorageBytes)}`,
        icon: '#icon-manga',
        tone: 'purple',
        to: { name: 'Manga' },
      },
      {
        label: '游戏',
        value: formatNumber(user.value.gameCount),
        unit: '部',
        detail: '已收录游戏资料',
        icon: '#icon-game',
        tone: 'green',
        to: { name: 'Game' },
      },
      {
        label: '小说',
        value: formatNumber(user.value.novelCount),
        unit: '部',
        detail: `共 ${formatNumber(user.value.novelWords)} 字`,
        icon: '#icon-novel',
        tone: 'orange',
        to: { name: 'Novel' },
      },
    ])

    async function refreshUserInfo() {
      try {
        const res = await server.get('/user-info/getInfo', { timeout: 15000 })
        store.commit('updateUserInfo', res.data)
      } catch {
        // 请求层已经提示错误；保留现有缓存，避免个人中心内容被清空。
      }
    }

    onMounted(refreshUserInfo)

    function openNicknameDialog() {
      nicknameForm.value = user.value.nickname || user.value.username
      nicknameForm.error = ''
      dialogs.nickname = true
    }

    function resetNicknameForm() {
      nicknameForm.value = ''
      nicknameForm.error = ''
      nicknameForm.loading = false
    }

    async function saveNickname() {
      const nickname = nicknameForm.value.trim()
      if (!nickname) {
        nicknameForm.error = '昵称不能为空'
        return
      }
      nicknameForm.loading = true
      nicknameForm.error = ''
      try {
        const res = await server.patch('/user-info', { nickname })
        store.commit('updateUserInfo', res.data)
        dialogs.nickname = false
        ElMessage.success('昵称已更新')
      } catch (error) {
        nicknameForm.error = getErrorMessage(error)
      } finally {
        nicknameForm.loading = false
      }
    }

    function openAvatarDialog() {
      avatarForm.file = null
      avatarForm.error = ''
      avatarForm.preview = ''
      avatarForm.processing = false
      revokeAvatarObjectUrl()
      dialogs.avatar = true
    }

    function chooseAvatar() {
      avatarInput.value?.click()
    }

    function handleAvatarFile(event) {
      const file = event.target.files?.[0]
      event.target.value = ''
      avatarForm.error = ''
      if (!file) return
      if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) {
        avatarForm.error = '请选择 JPG、PNG、GIF 或 WEBP 图片'
        return
      }
      if (file.size > MAX_AVATAR_SIZE) {
        avatarForm.error = '头像文件大小不能超过 5MB'
        return
      }

      avatarForm.processing = true
      const sourceUrl = URL.createObjectURL(file)
      const image = new Image()
      image.onload = () => {
        const side = Math.min(image.naturalWidth, image.naturalHeight)
        const canvas = document.createElement('canvas')
        canvas.width = 512
        canvas.height = 512
        const context = canvas.getContext('2d')
        if (!context) {
          URL.revokeObjectURL(sourceUrl)
          avatarForm.processing = false
          avatarForm.error = '头像裁剪失败，请换一张图片重试'
          return
        }
        context.drawImage(
          image,
          (image.naturalWidth - side) / 2,
          (image.naturalHeight - side) / 2,
          side,
          side,
          0,
          0,
          canvas.width,
          canvas.height,
        )
        canvas.toBlob((blob) => {
          URL.revokeObjectURL(sourceUrl)
          avatarForm.processing = false
          if (!blob) {
            avatarForm.error = '头像裁剪失败，请换一张图片重试'
            return
          }
          revokeAvatarObjectUrl()
          avatarForm.objectUrl = URL.createObjectURL(blob)
          avatarForm.preview = avatarForm.objectUrl
          avatarForm.file = new File([blob], 'avatar.jpg', { type: 'image/jpeg' })
        }, 'image/jpeg', .92)
      }
      image.onerror = () => {
        URL.revokeObjectURL(sourceUrl)
        avatarForm.processing = false
        avatarForm.error = '无法读取这张图片，请换一张重试'
      }
      image.src = sourceUrl
    }

    function revokeAvatarObjectUrl() {
      if (avatarForm.objectUrl) {
        URL.revokeObjectURL(avatarForm.objectUrl)
        avatarForm.objectUrl = ''
      }
    }

    function resetAvatarForm() {
      revokeAvatarObjectUrl()
      avatarForm.file = null
      avatarForm.preview = ''
      avatarForm.error = ''
      avatarForm.processing = false
      avatarForm.loading = false
    }

    async function saveAvatar() {
      if (!avatarForm.file) {
        avatarForm.error = '请先选择图片'
        return
      }
      avatarForm.loading = true
      avatarForm.error = ''
      try {
        const formData = new FormData()
        formData.append('file', avatarForm.file)
        const res = await server.put('/user-info/avatar', formData)
        store.commit('updateUserInfo', res.data)
        dialogs.avatar = false
        ElMessage.success('头像已更新')
      } catch (error) {
        avatarForm.error = getErrorMessage(error)
      } finally {
        avatarForm.loading = false
      }
    }

    async function resetAvatar() {
      avatarForm.loading = true
      avatarForm.error = ''
      try {
        const res = await server.delete('/user-info/avatar')
        store.commit('updateUserInfo', res.data)
        dialogs.avatar = false
        ElMessage.success('已恢复默认头像')
      } catch (error) {
        avatarForm.error = getErrorMessage(error)
      } finally {
        avatarForm.loading = false
      }
    }

    function openPasswordDialog() {
      passwordForm.current = ''
      passwordForm.next = ''
      passwordForm.confirm = ''
      passwordForm.error = ''
      dialogs.password = true
    }

    function resetPasswordForm() {
      passwordForm.current = ''
      passwordForm.next = ''
      passwordForm.confirm = ''
      passwordForm.error = ''
      passwordForm.loading = false
    }

    async function savePassword() {
      if (!passwordForm.current) {
        passwordForm.error = '请输入当前密码'
        return
      }
      if (passwordForm.next.length < 8 || passwordForm.next.length > 72) {
        passwordForm.error = '新密码长度必须在8到72个字符之间'
        return
      }
      if (passwordForm.next !== passwordForm.confirm) {
        passwordForm.error = '两次输入的新密码不一致'
        return
      }

      passwordForm.loading = true
      passwordForm.error = ''
      try {
        await server.put('/user/password', {
          currentPassword: passwordForm.current,
          newPassword: passwordForm.next,
        })
        dialogs.password = false
        localStorage.removeItem('accessToken')
        store.commit('setAccessToken', '')
        store.commit('updateUserInfo', {})
        ElMessage.success('密码已修改，请重新登录')
        await router.replace({ name: 'Login' })
      } catch (error) {
        passwordForm.error = getErrorMessage(error)
      } finally {
        passwordForm.loading = false
      }
    }

    function getErrorMessage(error) {
      if (typeof error === 'string') return error
      return error?.message || '操作失败，请稍后重试'
    }

    onBeforeUnmount(() => {
      revokeAvatarObjectUrl()
    })

    return {
      user,
      displayName,
      statCards,
      dialogs,
      nicknameForm,
      avatarForm,
      passwordForm,
      avatarInput,
      openNicknameDialog,
      resetNicknameForm,
      saveNickname,
      openAvatarDialog,
      chooseAvatar,
      handleAvatarFile,
      resetAvatarForm,
      saveAvatar,
      resetAvatar,
      openPasswordDialog,
      resetPasswordForm,
      savePassword,
      formatNumber,
      formatBytes,
    }
  },
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatBytes(value) {
  const bytes = Number(value || 0)
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  const amount = bytes / (1024 ** index)
  const digits = index === 0 || amount >= 10 ? 0 : 1
  return `${amount.toFixed(digits)} ${units[index]}`
}
</script>

<style scoped>
.account-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 92px max(24px, calc((100vw - 1040px) / 2)) 48px;
  background: #f6f8fb;
  color: #1f2937;
  font-family: 'Blueaka', sans-serif;
}

.account-panel {
  overflow: hidden;
  border: 1px solid #e5ebf2;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(33, 74, 117, .07);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px 28px;
  border-bottom: 1px solid #edf1f5;
}

.profile-avatar {
  position: relative;
  flex: 0 0 84px;
  width: 84px;
  height: 84px;
  padding: 0;
  overflow: hidden;
  border: 3px solid #fff;
  border-radius: 22px;
  background: #eaf4ff;
  box-shadow: 0 6px 18px rgba(44, 93, 145, .15);
  cursor: pointer;
}

.profile-avatar img,
.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar-overlay {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 6px 4px;
  color: #fff;
  background: rgba(15, 23, 42, .68);
  font-size: 12px;
  opacity: 0;
  transition: opacity .2s ease;
}

.profile-avatar:hover .profile-avatar-overlay,
.profile-avatar:focus-visible .profile-avatar-overlay {
  opacity: 1;
}

.profile-copy {
  min-width: 0;
}

.profile-name {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 7px;
}

.profile-copy h1 {
  margin: 0;
  color: #172033;
  font-size: clamp(25px, 3vw, 32px);
  letter-spacing: .02em;
}

.profile-copy p,
.section-heading p,
.dialog-hint {
  margin: 0;
  color: #8290a5;
  font-size: 14px;
}

.dot {
  margin: 0 8px;
  color: #c3cedc;
}

.profile-edit {
  display: inline-flex;
  width: 32px;
  height: 32px;
  flex: none;
  align-items: center;
  justify-content: center;
  padding: 7px;
  border: 0;
  border-radius: 9px;
  color: #6b7b90;
  background: transparent;
  cursor: pointer;
}

.profile-edit:hover,
.profile-edit:focus-visible {
  color: #2587e8;
  background: #edf6ff;
}

.panel-section {
  padding: 24px 28px;
  border-bottom: 1px solid #edf1f5;
}

.panel-section:last-child {
  border-bottom: 0;
}

.section-heading {
  margin-bottom: 16px;
}

.section-heading h2 {
  margin: 0 0 5px;
  color: #1e293b;
  font-size: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  display: flex;
  min-height: 130px;
  box-sizing: border-box;
  flex-direction: column;
  padding: 16px;
  border: 1px solid #e6edf6;
  border-radius: 14px;
  background: #fbfcfe;
  transition: border-color .2s ease, background .2s ease, transform .2s ease;
}

.stat-card:hover {
  border-color: #cfe2f5;
  background: #fff;
  transform: translateY(-2px);
}

.stat-heading {
  display: flex;
  align-items: center;
  gap: 9px;
}

.stat-icon {
  display: inline-flex;
  width: 32px;
  height: 32px;
  flex: none;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 17px;
}

.stat-icon.blue { color: #2587e8; background: #eaf5ff; }
.stat-icon.purple { color: #8964db; background: #f2edff; }
.stat-icon.orange { color: #ec9945; background: #fff4e8; }
.stat-icon.green { color: #3ba889; background: #e8f9f2; }

.stat-label {
  color: #526176;
  font-size: 14px;
  font-weight: 600;
}

.stat-arrow {
  width: 12px;
  height: 12px;
  margin-left: auto;
  color: #b2bdca;
}

.stat-value {
  display: flex;
  align-items: baseline;
  gap: 5px;
  margin: 15px 0 5px;
}

.stat-value strong {
  color: #1f2937;
  font-size: 25px;
  line-height: 1;
}

.stat-value span,
.stat-detail {
  color: #8a97a8;
  font-size: 12px;
}

.security-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 18px;
  border: 1px solid #e9eef4;
  border-radius: 12px;
  background: #f8fafc;
}

.security-label { display: block; margin-bottom: 4px; color: #8b98aa; font-size: 12px; }
.security-row strong { display: block; color: #334155; font-size: 15px; }
.security-row small { display: block; margin-top: 4px; color: #98a5b5; font-size: 12px; }
.setting-action { flex: none; padding: 8px 14px; border: 1px solid #cfe5fb; border-radius: 9px; color: #2587e8; background: #f3f9ff; cursor: pointer; font-family: inherit; }
.setting-action:hover { border-color: #409eff; background: #eaf5ff; }

.dialog-form { display: flex; flex-direction: column; gap: 9px; }
.dialog-form label { color: #526176; font-size: 13px; }
.dialog-form input { box-sizing: border-box; width: 100%; padding: 11px 13px; border: 1px solid #dbe5ef; border-radius: 10px; outline: none; color: #253247; background: #fbfdff; font: inherit; }
.dialog-form input:focus { border-color: #7abcf4; box-shadow: 0 0 0 3px rgba(64, 158, 255, .12); }
.form-error { margin: 2px 0 0; color: #e05252; font-size: 13px; }
.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }
.dialog-button { min-width: 78px; padding: 9px 16px; border: 1px solid transparent; border-radius: 9px; cursor: pointer; font-family: inherit; }
.dialog-button:disabled { cursor: wait; opacity: .6; }
.dialog-button.secondary { border-color: #dbe5ef; color: #64748b; background: #fff; }
.dialog-button.primary { color: #fff; background: #409eff; }
.dialog-button.primary:hover:not(:disabled) { background: #2587e8; }
.avatar-dialog { text-align: center; }
.avatar-preview { width: 160px; height: 160px; margin: 0 auto 16px; overflow: hidden; border-radius: 28px; background: #eef6ff; box-shadow: inset 0 0 0 1px #e1edf8; }
.dialog-hint { line-height: 1.6; }
.hidden-file-input { display: none; }
.avatar-file-actions { display: flex; align-items: center; justify-content: center; gap: 14px; margin-top: 18px; }
.text-button { padding: 0; border: 0; color: #8491a5; background: transparent; cursor: pointer; font: inherit; }
.text-button:hover { color: #409eff; }

@media screen and (max-width: 900px) {
  .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media screen and (max-width: 600px) {
  .account-page { padding: 80px 12px 32px; }
  .account-panel { border-radius: 16px; }
  .profile-header { gap: 14px; padding: 18px; }
  .profile-avatar { flex-basis: 70px; width: 70px; height: 70px; border-radius: 18px; }
  .profile-copy h1 { font-size: 24px; }
  .profile-copy p { font-size: 12px; }
  .panel-section { padding: 20px 18px; }
  .stats-grid { gap: 10px; }
  .stat-card { min-height: 120px; padding: 14px; }
  .security-row { align-items: flex-start; padding: 14px; }
}
</style>

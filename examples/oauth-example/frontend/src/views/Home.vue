<template>
  <div class="page-container home-page">
    <div class="home-wrapper">
      <div class="arco-card auth-card fade-in">
        <!-- Logo 区域 -->
        <div class="logo-section stagger-fade-in stagger-1">
          <div class="app-logo">
            <el-icon :size="32"><Connection /></el-icon>
          </div>
          <h1 class="app-title">OAuth Client Demo</h1>
          <p class="app-desc">Vocoor 第三方登录示例应用</p>
        </div>

        <!-- 已登录状态 -->
        <div v-if="userStore.isLoggedIn" class="logged-in-section">
          <div class="user-card stagger-fade-in stagger-2">
            <img 
              :src="userStore.user?.avatar || defaultAvatar" 
              :alt="userStore.user?.nickname"
              class="user-avatar"
            />
            <div class="user-info">
              <div class="user-name">{{ userStore.user?.nickname }}</div>
              <div class="user-detail">{{ userStore.user?.mobile }}</div>
              <div v-if="userStore.user?.companyName" class="user-detail">
                {{ userStore.user?.companyName }}
              </div>
            </div>
          </div>

          <div class="action-buttons stagger-fade-in stagger-3">
            <el-button type="primary" size="large" @click="goToDashboard">
              <el-icon><User /></el-icon>
              进入用户中心
            </el-button>
            <el-button size="large" @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-button>
          </div>
        </div>

        <!-- 未登录状态 -->
        <div v-else class="login-section">
          <div class="features stagger-fade-in stagger-2">
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon :size="20"><Lock /></el-icon>
              </div>
              <div class="feature-text">
                <strong>安全授权</strong>
                <span>OAuth 2.0 标准协议</span>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon secondary">
                <el-icon :size="20"><User /></el-icon>
              </div>
              <div class="feature-text">
                <strong>一键登录</strong>
                <span>使用 Vocoor 账号登录</span>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon success">
                <el-icon :size="20"><CircleCheck /></el-icon>
              </div>
              <div class="feature-text">
                <strong>用户绑定</strong>
                <span>自动创建本地账号</span>
              </div>
            </div>
          </div>

          <div class="scope-section stagger-fade-in stagger-3">
            <p class="scope-label">授权范围</p>
            <div class="scope-tags">
              <el-tag v-for="scope in scopes" :key="scope.key" :type="scope.type" size="large">
                {{ scope.label }}
              </el-tag>
            </div>
          </div>

          <button 
            class="vocoor-btn stagger-fade-in stagger-4" 
            :disabled="loading"
            @click="handleLogin"
          >
            <div class="vocoor-logo">V</div>
            <span>{{ loading ? '跳转中...' : '使用 Vocoor 账号登录' }}</span>
          </button>

          <p class="login-tip stagger-fade-in stagger-5">
            点击登录即表示您同意授权本应用获取您的 Vocoor 账号信息
          </p>
        </div>

        <!-- 健康状态 -->
        <div class="health-status stagger-fade-in stagger-6">
          <div :class="['status-tag', healthStatus]">
            <el-icon v-if="healthStatus === 'success'"><CircleCheck /></el-icon>
            <el-icon v-else-if="healthStatus === 'error'"><CircleClose /></el-icon>
            <el-icon v-else class="spin"><Loading /></el-icon>
            <span>{{ healthMessage }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Connection, Lock, User, CircleCheck, CircleClose, 
  Loading, SwitchButton 
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getAuthUrl, healthCheck } from '@/api/oauth'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const healthStatus = ref<'success' | 'error' | 'loading'>('loading')
const healthMessage = ref('检查服务状态...')

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const scopes = [
  { key: 'profile', label: '基本信息', type: '' as const },
  { key: 'phone', label: '手机号', type: 'info' as const },
  { key: 'company', label: '企业信息', type: 'warning' as const }
]

// 发起登录
async function handleLogin() {
  loading.value = true
  try {
    const authUrl = await getAuthUrl('profile phone company')
    window.location.href = authUrl
  } catch (error: any) {
    ElMessage.error(error.message || '获取授权地址失败')
    loading.value = false
  }
}

// 退出登录
async function handleLogout() {
  await userStore.logout()
  ElMessage.success('已退出登录')
}

// 进入用户中心
function goToDashboard() {
  router.push('/dashboard')
}

// 检查健康状态
async function checkHealth() {
  try {
    const result = await healthCheck()
    if (result.status === 'UP') {
      healthStatus.value = 'success'
      healthMessage.value = '服务正常'
    } else {
      healthStatus.value = 'error'
      healthMessage.value = '服务异常'
    }
  } catch {
    healthStatus.value = 'error'
    healthMessage.value = '无法连接后端服务'
  }
}

onMounted(async () => {
  await checkHealth()
  if (localStorage.getItem('access_token')) {
    await userStore.fetchUser()
  }
})
</script>

<style lang="scss" scoped>
.home-page {
  background: linear-gradient(135deg, #E8F3FF 0%, #F7F8FA 50%, #E8F0FF 100%);
}

.home-wrapper {
  width: 100%;
  max-width: 420px;
}

.auth-card {
  padding: 48px 40px;
}

.logo-section {
  text-align: center;
  margin-bottom: 32px;

  .app-logo {
    width: 72px;
    height: 72px;
    background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
    border-radius: var(--border-radius-xlarge);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    margin: 0 auto 16px;
    box-shadow: 0 8px 24px rgba(22, 93, 255, 0.25);
  }

  .app-title {
    font-size: 24px;
    font-weight: 600;
    color: var(--color-text-1);
    margin: 0 0 8px;
  }

  .app-desc {
    font-size: 14px;
    color: var(--color-text-3);
    margin: 0;
  }
}

.features {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 24px;
  padding: 20px;
  background: var(--color-fill-1);
  border-radius: var(--border-radius-large);
  border: 1px solid var(--color-border-1);

  .feature-item {
    display: flex;
    align-items: center;
    gap: 14px;

    .feature-icon {
      width: 40px;
      height: 40px;
      background: var(--color-primary-lightest);
      border-radius: var(--border-radius-medium);
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--color-primary);
      flex-shrink: 0;
      
      &.secondary {
        background: #FFF0E5;
        color: var(--color-warning);
      }
      
      &.success {
        background: var(--color-success-lightest);
        color: var(--color-success);
      }
    }

    .feature-text {
      display: flex;
      flex-direction: column;

      strong {
        font-size: 14px;
        font-weight: 600;
        color: var(--color-text-1);
      }

      span {
        font-size: 13px;
        color: var(--color-text-3);
        margin-top: 2px;
      }
    }
  }
}

.scope-section {
  margin-bottom: 24px;

  .scope-label {
    font-size: 13px;
    color: var(--color-text-3);
    margin-bottom: 10px;
  }

  .scope-tags {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }
}

.login-tip {
  text-align: center;
  font-size: 12px;
  color: var(--color-text-4);
  margin-top: 16px;
}

.logged-in-section {
  .user-card {
    margin-top: 0;
  }
  
  .action-buttons {
    display: flex;
    flex-direction: column;
    gap: 12px;
    margin-top: 24px;
  }
}

.health-status {
  margin-top: 32px;
  text-align: center;
}
</style>

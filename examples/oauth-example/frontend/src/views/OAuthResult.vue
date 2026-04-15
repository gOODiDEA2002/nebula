<template>
  <div class="page-container result-page">
    <div class="result-wrapper">
      <div class="arco-card result-card fade-in">
        <!-- 成功状态 -->
        <template v-if="success">
          <div class="result-icon success stagger-fade-in stagger-1">
            <el-icon :size="48"><CircleCheck /></el-icon>
          </div>
          <h2 class="result-title stagger-fade-in stagger-2">授权成功</h2>
          <p class="result-desc stagger-fade-in stagger-3">
            欢迎回来，<span class="highlight">{{ nickname }}</span>
          </p>
          
          <div class="result-info stagger-fade-in stagger-4">
            <div class="info-row">
              <span class="label">用户 ID</span>
              <span class="value">{{ userId }}</span>
            </div>
          </div>

          <div class="action-buttons stagger-fade-in stagger-5">
            <button class="arco-btn arco-btn--primary arco-btn--large arco-btn--block" @click="goToDashboard">
              进入用户中心
            </button>
            <button class="arco-btn arco-btn--secondary arco-btn--large arco-btn--block" @click="goToHome">
              返回首页
            </button>
          </div>
        </template>

        <!-- 失败状态 -->
        <template v-else>
          <div class="result-icon error stagger-fade-in stagger-1">
            <el-icon :size="48"><CircleClose /></el-icon>
          </div>
          <h2 class="result-title stagger-fade-in stagger-2">授权失败</h2>
          <p class="result-desc error-text stagger-fade-in stagger-3">{{ errorMessage }}</p>

          <div class="action-buttons stagger-fade-in stagger-4">
            <button class="arco-btn arco-btn--primary arco-btn--large arco-btn--block" @click="retryLogin">
              重新登录
            </button>
            <button class="arco-btn arco-btn--secondary arco-btn--large arco-btn--block" @click="goToHome">
              返回首页
            </button>
          </div>
        </template>

        <!-- 倒计时跳转 -->
        <div class="countdown-section stagger-fade-in stagger-6" v-if="countdown > 0">
          <div class="countdown-ring">
            <svg viewBox="0 0 36 36">
              <circle
                class="countdown-bg"
                cx="18" cy="18" r="16"
                fill="none"
                stroke-width="2"
              />
              <circle
                class="countdown-progress"
                :class="{ success: success, error: !success }"
                cx="18" cy="18" r="16"
                fill="none"
                stroke-width="2"
                :stroke-dasharray="circumference"
                :stroke-dashoffset="progressOffset"
              />
            </svg>
            <span class="countdown-number">{{ countdown }}</span>
          </div>
          <p class="countdown-text">秒后自动跳转</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const success = ref(false)
const nickname = ref('')
const userId = ref('')
const errorMessage = ref('未知错误')
const countdown = ref(5)
const initialCountdown = 5

let timer: number | null = null

// 圆环进度计算
const circumference = 2 * Math.PI * 16
const progressOffset = computed(() => {
  return circumference * (1 - countdown.value / initialCountdown)
})

function goToHome() {
  router.push('/')
}

function goToDashboard() {
  router.push('/dashboard')
}

async function retryLogin() {
  router.push('/')
}

onMounted(() => {
  // 解析 URL 参数
  const query = route.query
  success.value = query.success === 'true'
  
  if (success.value) {
    const token = query.token as string
    userId.value = query.userId as string || ''
    nickname.value = decodeURIComponent(query.nickname as string || '用户')
    
    // 保存 token
    if (token) {
      userStore.setToken(token)
      // 获取用户信息
      userStore.fetchUser()
    }
    
    // 开始倒计时
    timer = window.setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        if (timer) clearInterval(timer)
        goToDashboard()
      }
    }, 1000)
  } else {
    errorMessage.value = decodeURIComponent(query.error as string || '授权失败')
    
    // 开始倒计时
    timer = window.setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        if (timer) clearInterval(timer)
        goToHome()
      }
    }, 1000)
  }
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style lang="scss" scoped>
.result-page {
  background: linear-gradient(135deg, #E8F3FF 0%, #F7F8FA 50%, #E8F0FF 100%);
}

.result-wrapper {
  width: 100%;
  max-width: 420px;
}

.result-card {
  text-align: center;
  padding: 48px 40px;
}

.result-icon {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  
  &.success {
    background: var(--color-success-lightest);
    color: var(--color-success);
    box-shadow: 0 8px 24px rgba(0, 180, 42, 0.2);
  }
  
  &.error {
    background: var(--color-error-lightest);
    color: var(--color-error);
    box-shadow: 0 8px 24px rgba(245, 63, 63, 0.2);
  }
}

.result-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-1);
  margin: 0 0 12px;
}

.result-desc {
  font-size: 16px;
  color: var(--color-text-2);
  margin: 0 0 28px;
  
  .highlight {
    color: var(--color-primary);
    font-weight: 600;
  }
  
  &.error-text {
    color: var(--color-error);
  }
}

.result-info {
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-1);
  border-radius: var(--border-radius-medium);
  padding: 16px 20px;
  margin-bottom: 28px;
  
  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .label {
      font-size: 14px;
      color: var(--color-text-3);
    }
    
    .value {
      font-size: 14px;
      color: var(--color-text-1);
      font-weight: 500;
    }
  }
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.countdown-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 32px;
  
  .countdown-ring {
    position: relative;
    width: 36px;
    height: 36px;
    
    svg {
      transform: rotate(-90deg);
      width: 100%;
      height: 100%;
    }
    
    .countdown-bg {
      stroke: var(--color-border-2);
    }
    
    .countdown-progress {
      transition: stroke-dashoffset 0.3s ease;
      stroke-linecap: round;
      
      &.success {
        stroke: var(--color-success);
      }
      
      &.error {
        stroke: var(--color-error);
      }
    }
    
    .countdown-number {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 600;
      color: var(--color-text-1);
    }
  }
  
  .countdown-text {
    font-size: 14px;
    color: var(--color-text-3);
    margin: 0;
  }
}
</style>

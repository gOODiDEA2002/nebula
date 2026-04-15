<template>
  <div class="page-container dashboard-page">
    <div class="dashboard-wrapper">
      <div class="arco-card dashboard-card fade-in">
        <!-- 头部 -->
        <div class="dashboard-header stagger-fade-in stagger-1">
          <div class="header-title">
            <div class="title-icon">
              <el-icon :size="18"><User /></el-icon>
            </div>
            <h1>用户中心</h1>
          </div>
          <button class="logout-btn" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出</span>
          </button>
        </div>

        <!-- 加载状态 -->
        <div v-if="userStore.loading" class="loading-state">
          <div class="loading-spinner">
            <el-icon class="spin" :size="32"><Loading /></el-icon>
          </div>
          <span>加载中...</span>
        </div>

        <!-- 用户信息 -->
        <template v-else-if="userStore.user">
          <div class="user-profile stagger-fade-in stagger-2">
            <div class="profile-avatar-wrapper">
              <img 
                :src="userStore.user.avatar || defaultAvatar" 
                :alt="userStore.user.nickname"
                class="profile-avatar"
              />
            </div>
            <div class="profile-info">
              <h2 class="profile-name">{{ userStore.user.nickname }}</h2>
              <p class="profile-username">@{{ userStore.user.username }}</p>
            </div>
            <div class="profile-badge">
              <el-icon><CircleCheck /></el-icon>
              <span>已认证</span>
            </div>
          </div>

          <div class="info-section stagger-fade-in stagger-3">
            <div class="section-header">
              <span class="section-title">用户信息</span>
            </div>
            
            <div class="info-grid">
              <div class="info-item">
                <div class="info-icon">
                  <el-icon><User /></el-icon>
                </div>
                <div class="info-content">
                  <span class="info-label">用户ID</span>
                  <span class="info-value">{{ userStore.user.id }}</span>
                </div>
              </div>

              <div class="info-item" v-if="userStore.user.mobile">
                <div class="info-icon">
                  <el-icon><Iphone /></el-icon>
                </div>
                <div class="info-content">
                  <span class="info-label">手机号</span>
                  <span class="info-value">{{ userStore.user.mobile }}</span>
                </div>
              </div>

              <div class="info-item" v-if="userStore.user.email">
                <div class="info-icon">
                  <el-icon><Message /></el-icon>
                </div>
                <div class="info-content">
                  <span class="info-label">邮箱</span>
                  <span class="info-value">{{ userStore.user.email }}</span>
                </div>
              </div>

              <div class="info-item" v-if="userStore.user.companyName">
                <div class="info-icon warning">
                  <el-icon><OfficeBuilding /></el-icon>
                </div>
                <div class="info-content">
                  <span class="info-label">企业</span>
                  <span class="info-value">{{ userStore.user.companyName }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="tech-section stagger-fade-in stagger-4">
            <div class="section-header">
              <span class="section-title">技术信息</span>
            </div>
            
            <div class="tech-grid">
              <div class="tech-item">
                <span class="tech-label">登录方式</span>
                <div class="tech-value">
                  <el-tag type="success" size="small">Vocoor OAuth 2.0</el-tag>
                </div>
              </div>
              <div class="tech-item">
                <span class="tech-label">授权范围</span>
                <div class="scope-tags">
                  <el-tag size="small">profile</el-tag>
                  <el-tag type="info" size="small">phone</el-tag>
                  <el-tag type="warning" size="small">company</el-tag>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- 未登录 -->
        <template v-else>
          <div class="empty-state">
            <div class="empty-icon">
              <el-icon :size="48"><UserFilled /></el-icon>
            </div>
            <p>请先登录</p>
            <el-button type="primary" @click="goToHome">去登录</el-button>
          </div>
        </template>

        <!-- 底部 -->
        <div class="dashboard-footer stagger-fade-in stagger-5">
          <button class="back-btn" @click="goToHome">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回首页</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  SwitchButton, Loading, User, Iphone, Message, 
  OfficeBuilding, UserFilled, ArrowLeft, CircleCheck 
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

function goToHome() {
  router.push('/')
}

async function handleLogout() {
  await userStore.logout()
  ElMessage.success('已退出登录')
  goToHome()
}

onMounted(async () => {
  if (!userStore.user && localStorage.getItem('access_token')) {
    await userStore.fetchUser()
  }
})
</script>

<style lang="scss" scoped>
.dashboard-page {
  background: linear-gradient(135deg, #E8F3FF 0%, #F7F8FA 50%, #E8F0FF 100%);
}

.dashboard-wrapper {
  width: 100%;
  max-width: 480px;
}

.dashboard-card {
  padding: 32px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  .header-title {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .title-icon {
      width: 36px;
      height: 36px;
      background: var(--color-primary-lightest);
      border-radius: var(--border-radius-medium);
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--color-primary);
    }
    
    h1 {
      font-size: 18px;
      font-weight: 600;
      color: var(--color-text-1);
      margin: 0;
    }
  }
  
  .logout-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 14px;
    background: var(--color-bg-1);
    border: 1px solid var(--color-border-2);
    border-radius: var(--border-radius-small);
    color: var(--color-text-3);
    font-size: 13px;
    cursor: pointer;
    transition: all var(--transition-duration) ease;
    
    &:hover {
      border-color: var(--color-error);
      color: var(--color-error);
    }
  }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 60px 0;
  color: var(--color-text-3);
  
  .loading-spinner {
    color: var(--color-primary);
  }
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  border-radius: var(--border-radius-large);
  margin-bottom: 24px;
  
  .profile-avatar-wrapper {
    .profile-avatar {
      width: 64px;
      height: 64px;
      border-radius: 50%;
      object-fit: cover;
      border: 3px solid rgba(255, 255, 255, 0.3);
    }
  }
  
  .profile-info {
    flex: 1;
    
    .profile-name {
      font-size: 20px;
      font-weight: 600;
      color: #fff;
      margin: 0 0 4px;
    }
    
    .profile-username {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.8);
      margin: 0;
    }
  }
  
  .profile-badge {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 6px 12px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 20px;
    color: #fff;
    font-size: 12px;
    font-weight: 500;
  }
}

.section-header {
  margin-bottom: 14px;
  
  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--color-text-2);
  }
}

.info-section {
  margin-bottom: 24px;
}

.info-grid {
  display: grid;
  gap: 10px;
  
  .info-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    background: var(--color-fill-1);
    border: 1px solid var(--color-border-1);
    border-radius: var(--border-radius-medium);
    transition: all var(--transition-duration) ease;
    
    &:hover {
      border-color: var(--color-primary-lighter);
      background: var(--color-primary-lightest);
    }
    
    .info-icon {
      width: 36px;
      height: 36px;
      background: var(--color-primary-lightest);
      border-radius: var(--border-radius-small);
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--color-primary);
      flex-shrink: 0;
      
      &.warning {
        background: var(--color-warning-lightest);
        color: var(--color-warning);
      }
    }
    
    .info-content {
      flex: 1;
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .info-label {
        font-size: 14px;
        color: var(--color-text-3);
      }
      
      .info-value {
        font-size: 14px;
        color: var(--color-text-1);
        font-weight: 500;
      }
    }
  }
}

.tech-section {
  margin-bottom: 24px;
}

.tech-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-1);
  border-radius: var(--border-radius-medium);
  
  .tech-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .tech-label {
      font-size: 14px;
      color: var(--color-text-3);
    }
    
    .scope-tags {
      display: flex;
      gap: 6px;
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 60px 0;
  
  .empty-icon {
    width: 80px;
    height: 80px;
    background: var(--color-fill-1);
    border: 1px solid var(--color-border-1);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--color-text-4);
  }
  
  p {
    font-size: 16px;
    color: var(--color-text-3);
    margin: 0;
  }
}

.dashboard-footer {
  text-align: center;
  margin-top: 8px;
  
  .back-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 10px 20px;
    background: var(--color-bg-1);
    border: 1px solid var(--color-border-2);
    border-radius: var(--border-radius-medium);
    color: var(--color-text-2);
    font-size: 14px;
    cursor: pointer;
    transition: all var(--transition-duration) ease;
    
    &:hover {
      border-color: var(--color-primary);
      color: var(--color-primary);
    }
  }
}
</style>

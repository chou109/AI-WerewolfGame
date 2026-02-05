<template>
  <div id="app">
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
// App.vue - 应用根组件
</script>

<style scoped>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  min-height: 100vh;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  background-color: #f5f7fa;
}

.el-container {
  min-height: 100vh;
}

.el-header {
  background-color: #409eff;
  color: #fff;
  line-height: 60px;
}

.el-footer {
  background-color: #f5f7fa;
  color: #666;
  line-height: 60px;
  text-align: center;
  border-top: 1px solid #e4e7ed;
}

.el-main {
  padding: 20px;
}

.el-card {
  margin-bottom: 20px;
}

.el-button--primary {
  background-color: #409eff;
  border-color: #409eff;
}

.el-button--success {
  background-color: #67c23a;
  border-color: #67c23a;
}

.el-button--danger {
  background-color: #f56c6c;
  border-color: #f56c6c;
}

.el-button--warning {
  background-color: #e6a23c;
  border-color: #e6a23c;
}

/* 游戏相关样式 */
.game-room-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.game-room-card {
  transition: all 0.3s ease;
}

.game-room-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.player-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.player-item {
  width: 80px;
  text-align: center;
}

.player-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  margin: 0 auto 5px;
  background-color: #ecf5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #409eff;
  border: 2px solid #409eff;
}

.player-avatar.sheriff {
  border-color: #e6a23c;
  background-color: #fdf6ec;
  color: #e6a23c;
}

.player-avatar.dead {
  opacity: 0.5;
  background-color: #fef0f0;
  color: #f56c6c;
  border-color: #f56c6c;
}

.speech-container {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
  background-color: #fff;
}

.speech-item {
  margin-bottom: 10px;
  padding: 10px;
  border-radius: 4px;
  background-color: #f9f9f9;
}

.speech-item .speaker {
  font-weight: bold;
  margin-bottom: 5px;
  color: #409eff;
}

.speech-item .content {
  line-height: 1.4;
}

.speech-item .time {
  font-size: 12px;
  color: #999;
  text-align: right;
  margin-top: 5px;
}

/* 投票区域样式 */
.vote-container {
  margin-top: 20px;
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background-color: #fff;
}

.vote-result {
  margin-top: 10px;
  padding: 10px;
  background-color: #f0f9eb;
  border-radius: 4px;
  border-left: 4px solid #67c23a;
}

/* 游戏状态样式 */
.game-status {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #ecf5ff;
  border-radius: 4px;
  border-left: 4px solid #409eff;
}

.game-status h3 {
  margin-bottom: 10px;
  color: #409eff;
}

.game-status .status-item {
  margin-bottom: 5px;
  display: flex;
  justify-content: space-between;
}

.game-status .status-label {
  color: #606266;
}

.game-status .status-value {
  font-weight: bold;
  color: #303133;
}
</style>

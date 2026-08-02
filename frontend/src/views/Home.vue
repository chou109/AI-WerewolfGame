<template>
  <div class="lobby-page">
    <section class="lobby-hero" ref="heroRef">
      <canvas ref="fireflyCanvas" class="firefly-canvas" aria-hidden="true"></canvas>
      <div class="hero-vignette" aria-hidden="true"></div>

      <div class="hero-layout">
        <div class="hero-copy">
          <div class="eyebrow"><span></span>{{ $locale === 'zh-CN' ? 'THE MOONLIT TABLE' : 'THE MOONLIT TABLE' }}</div>
          <h1>{{ $t('home.title') }}</h1>
          <p class="hero-lead">{{ $t('home.subtitle') }}</p>

          <div class="hero-actions">
            <button class="hero-primary" @click="goToRoomList">
              <span class="button-mark">✦</span>
              {{ $t('home.enterGame') }}
            </button>
            <button class="hero-secondary" @click="goToApiConfig">
              {{ $t('home.aiPlayerManagement') }}
              <span aria-hidden="true">→</span>
            </button>
          </div>

          <div class="hero-status" aria-label="平台状态">
            <div class="status-item">
              <span class="status-label">{{ $locale === 'zh-CN' ? '推理引擎' : 'AI ENGINE' }}</span>
              <span class="status-value"><i></i>{{ $locale === 'zh-CN' ? '已就绪' : 'ONLINE' }}</span>
            </div>
            <div class="status-divider"></div>
            <div class="status-item">
              <span class="status-label">{{ $locale === 'zh-CN' ? '游戏规则' : 'RULE SET' }}</span>
              <span class="status-value">{{ $locale === 'zh-CN' ? '标准与进阶' : 'STANDARD & ADVANCED' }}</span>
            </div>
          </div>
        </div>

        <aside class="hero-side-card">
          <div class="side-card-top">
            <span>{{ $locale === 'zh-CN' ? '今夜的圆桌' : 'TONIGHT AT THE TABLE' }}</span>
            <span class="side-orbit">◌</span>
          </div>
          <div class="side-card-mark">☾</div>
          <p>{{ $locale === 'zh-CN' ? '选择一张板子，召集玩家，在谎言与直觉之间找到真相。' : 'Choose a board, gather players, and find truth between intuition and deceit.' }}</p>
          <button @click="scrollDown">{{ $locale === 'zh-CN' ? '浏览游戏模式' : 'EXPLORE MODES' }} <span>↓</span></button>
        </aside>
      </div>

      <button class="hero-scroll" @click="scrollDown" :aria-label="$locale === 'zh-CN' ? '浏览下方内容' : 'Explore content below'">
        <span>{{ $locale === 'zh-CN' ? '向下探索' : 'EXPLORE' }}</span>
        <i></i>
      </button>
    </section>

    <main class="lobby-content" id="features">
      <section class="content-section feature-section">
        <div class="section-heading">
          <div>
            <span class="section-kicker">01 / {{ $locale === 'zh-CN' ? '系统能力' : 'SYSTEM' }}</span>
            <h2>{{ $locale === 'zh-CN' ? '不只是旁观一场游戏' : 'MORE THAN A SPECTATOR GAME' }}</h2>
          </div>
          <p>{{ $locale === 'zh-CN' ? '为每一次发言保留动机、策略与局势变化。' : 'Every speech carries motive, strategy, and shifting context.' }}</p>
        </div>

        <div class="feature-grid">
          <article class="feature-panel">
            <span class="feature-number">01</span>
            <svg viewBox="0 0 48 48" aria-hidden="true"><path d="M13 25a11 11 0 0 1 8-10.6A10 10 0 0 1 39 21c0 .8-.1 1.5-.3 2.2A8 8 0 0 1 34 38H18a8 8 0 0 1-5-13Z"/><path d="M19 23h.1M30 23h.1M20 31c2.5 2 5.5 2 8 0"/></svg>
            <h3>{{ $t('home.features.ai.title') }}</h3>
            <p>{{ $t('home.features.ai.desc') }}</p>
            <span class="feature-line"></span>
          </article>
          <article class="feature-panel">
            <span class="feature-number">02</span>
            <svg viewBox="0 0 48 48" aria-hidden="true"><circle cx="24" cy="24" r="7"/><path d="M24 7v6m0 22v6M7 24h6m22 0h6M12 12l4.2 4.2m15.6 15.6L36 36m0-24-4.2 4.2M16.2 31.8 12 36"/></svg>
            <h3>{{ $t('home.features.config.title') }}</h3>
            <p>{{ $t('home.features.config.desc') }}</p>
            <span class="feature-line"></span>
          </article>
          <article class="feature-panel">
            <span class="feature-number">03</span>
            <svg viewBox="0 0 48 48" aria-hidden="true"><rect x="18" y="7" width="12" height="23" rx="6"/><path d="M12 23a12 12 0 0 0 24 0M24 35v6m-7 0h14"/></svg>
            <h3>{{ $t('home.features.voice.title') }}</h3>
            <p>{{ $t('home.features.voice.desc') }}</p>
            <span class="feature-line"></span>
          </article>
        </div>
      </section>

      <section class="content-section mode-section" id="modes">
        <div class="section-heading">
          <div>
            <span class="section-kicker">02 / {{ $locale === 'zh-CN' ? '选择板子' : 'GAME BOARDS' }}</span>
            <h2>{{ $t('home.gameModes') }}</h2>
          </div>
          <p>{{ $locale === 'zh-CN' ? '每一种配置，都有不同的谎言、信息与胜利路径。' : 'Each configuration changes the information, deception, and road to victory.' }}</p>
        </div>

        <div class="mode-grid">
          <button v-for="(mode, index) in gameModes" :key="mode.key" class="mode-panel" @click="createRoom(mode.players, mode.key)">
            <span class="mode-index">0{{ index + 1 }}</span>
            <div class="mode-panel-head">
              <span class="mode-icon">{{ mode.icon }}</span>
              <span class="mode-count">{{ mode.players }}{{ $locale === 'zh-CN' ? ' 人局' : ' PLAYERS' }}</span>
            </div>
            <h3>{{ mode.title }}</h3>
            <p>{{ mode.config }}</p>
            <span class="mode-action">{{ $locale === 'zh-CN' ? '选择此板子' : 'CHOOSE BOARD' }} <b>→</b></span>
          </button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useGameStore } from '../stores/game'
import { ElMessage } from 'element-plus'

const { proxy } = getCurrentInstance()
const $t = proxy.$t
const $locale = proxy.$locale
const router = useRouter()
const userStore = useUserStore()
const gameStore = useGameStore()
const heroRef = ref(null)
const fireflyCanvas = ref(null)
let animationId = null
let resizeHandler = null

const gameModes = computed(() => [
  { key: 'standard', players: 9, icon: '◈', title: $t('home.modeList.standard9.title'), config: $t('home.modeList.standard9.config') },
  { key: 'wolfking_guard', players: 12, icon: '♛', title: $t('home.modeList.wolfkingGuard.title'), config: $t('home.modeList.wolfkingGuard.config') },
  { key: 'miracle_merchant', players: 12, icon: '✦', title: $t('home.modeList.miracleMerchant.title'), config: $t('home.modeList.miracleMerchant.config') },
  { key: 'wolf_beauty_knight', players: 12, icon: '✧', title: $t('home.modeList.wolfBeauty.title'), config: $t('home.modeList.wolfBeauty.config') },
  { key: 'white_wolf_knight', players: 12, icon: '⚜', title: $t('home.modeList.whiteWolf.title'), config: $t('home.modeList.whiteWolf.config') },
  { key: 'gargoyle_gravedigger', players: 12, icon: '◇', title: $t('home.modeList.gargoyle.title'), config: $t('home.modeList.gargoyle.config') }
])

class Firefly {
  constructor(width, height) { this.reset(width, height, true) }
  reset(width, height, initial = false) {
    this.x = Math.random() * width
    this.y = initial ? Math.random() * height : height + 20
    this.size = 1 + Math.random() * 2
    this.speedX = (Math.random() - 0.5) * 0.28
    this.speedY = -(0.2 + Math.random() * 0.45)
    this.phase = Math.random() * Math.PI * 2
    this.phaseSpeed = 0.01 + Math.random() * 0.02
    this.opacity = 0.18 + Math.random() * 0.52
    this.direction = 1
  }
  update(width, height) {
    this.phase += this.phaseSpeed
    this.x += this.speedX + Math.sin(this.phase) * 0.22
    this.y += this.speedY
    this.opacity += this.direction * 0.007
    if (this.opacity > 0.78) this.direction = -1
    if (this.opacity < 0.12) this.direction = 1
    if (this.y < -20 || this.x < -20 || this.x > width + 20) this.reset(width, height)
  }
  draw(context) {
    context.save()
    context.globalAlpha = this.opacity
    const glow = context.createRadialGradient(this.x, this.y, 0, this.x, this.y, this.size * 7)
    glow.addColorStop(0, 'rgba(226, 196, 102, .95)')
    glow.addColorStop(.2, 'rgba(218, 188, 94, .4)')
    glow.addColorStop(1, 'rgba(218, 188, 94, 0)')
    context.fillStyle = glow
    context.beginPath()
    context.arc(this.x, this.y, this.size * 7, 0, Math.PI * 2)
    context.fill()
    context.restore()
  }
}

function initFireflies() {
  const canvas = fireflyCanvas.value
  const hero = heroRef.value
  if (!canvas || !hero) return
  const context = canvas.getContext('2d')
  const resize = () => {
    canvas.width = hero.offsetWidth
    canvas.height = hero.offsetHeight
  }
  resizeHandler = resize
  resize()
  window.addEventListener('resize', resize)
  const particles = Array.from({ length: 28 }, () => new Firefly(canvas.width, canvas.height))
  const frame = () => {
    context.clearRect(0, 0, canvas.width, canvas.height)
    particles.forEach(particle => { particle.update(canvas.width, canvas.height); particle.draw(context) })
    animationId = requestAnimationFrame(frame)
  }
  frame()
}

onMounted(initFireflies)
onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
})

const goToRoomList = () => router.push('/game/room/list')
const goToApiConfig = () => router.push('/ai-players')
const scrollDown = () => document.getElementById('modes')?.scrollIntoView({ behavior: 'smooth' })

const createRoom = async (playerCount, gameBoard) => {
  if (!userStore.getIsLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const success = await gameStore.createRoom({
      roomName: `GameRoom-${Date.now()}`,
      playerCount,
      gameBoard,
      roomCode: '',
      status: 1,
      creatorId: 1
    })
    if (success) {
      ElMessage.success($t('createRoom.createSuccess'))
      router.push(`/game/play/${gameStore.getCurrentRoom.id}?playerCount=${playerCount}&gameBoard=${gameBoard}`)
    } else {
      ElMessage.error($t('createRoom.createFailed'))
    }
  } catch {
    ElMessage.error($t('createRoom.createFailed'))
  }
}
</script>

<style scoped>
.lobby-page {
  margin: -20px;
  min-height: 100vh;
  color: #edf1f7;
  background: #08101a;
}

.lobby-hero {
  position: relative;
  isolation: isolate;
  min-height: min(760px, calc(100vh - 60px));
  overflow: hidden;
  display: flex;
  align-items: center;
  background: #07101a url('/assets/hero-moonlit-forest.png') center 58% / cover no-repeat;
}

.lobby-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    linear-gradient(90deg, rgba(4, 10, 18, .94) 0%, rgba(4, 10, 18, .78) 37%, rgba(4, 10, 18, .22) 68%, rgba(4, 10, 18, .46) 100%),
    linear-gradient(180deg, rgba(5, 9, 17, .20) 0%, rgba(5, 10, 16, .04) 52%, rgba(3, 8, 13, .90) 100%);
}

.hero-vignette {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: radial-gradient(circle at 52% 42%, transparent 0 26%, rgba(0, 0, 0, .18) 70%, rgba(0, 0, 0, .55) 100%);
}

.firefly-canvas { position: absolute; inset: 0; z-index: 1; pointer-events: none; }

.hero-layout {
  position: relative;
  z-index: 2;
  width: min(1220px, calc(100% - 80px));
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) 272px;
  align-items: end;
  gap: clamp(40px, 9vw, 150px);
  padding: 86px 0 98px;
}

.hero-copy { max-width: 680px; }
.eyebrow,
.section-kicker {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #d9b55d;
  font: 700 11px/1.2 var(--font-heading);
  letter-spacing: .2em;
}
.eyebrow span { width: 34px; height: 1px; background: #d9b55d; }

.hero-copy h1 {
  margin: 18px 0 20px;
  max-width: 670px;
  color: #ffffff;
  font-size: clamp(42px, 5.2vw, 76px);
  line-height: 1.04;
  letter-spacing: -.045em;
  text-shadow: 0 6px 24px rgba(0, 0, 0, .42);
}

.hero-lead {
  max-width: 530px;
  margin: 0;
  color: #d7e0e9;
  font-size: clamp(18px, 1.7vw, 21px);
  line-height: 1.65;
  text-shadow: 0 2px 14px rgba(0, 0, 0, .62);
}

.hero-actions { display: flex; gap: 12px; margin-top: 34px; flex-wrap: wrap; }
.hero-primary,
.hero-secondary {
  min-height: 48px;
  padding: 0 20px;
  border-radius: 8px;
  cursor: pointer;
  font: 700 12px/1 var(--font-heading);
  letter-spacing: .08em;
  transition: transform .2s ease, border-color .2s ease, background .2s ease, box-shadow .2s ease;
}
.hero-primary {
  border: 1px solid #efca72;
  color: #12100b;
  background: linear-gradient(135deg, #f2d47d, #c99a3c);
  box-shadow: 0 10px 30px rgba(206, 158, 61, .23);
}
.hero-primary:hover { transform: translateY(-2px); box-shadow: 0 14px 34px rgba(206, 158, 61, .38); }
.hero-secondary {
  border: 1px solid rgba(238, 244, 249, .38);
  color: #eef4fa;
  background: rgba(7, 15, 24, .44);
  backdrop-filter: blur(10px);
}
.hero-secondary:hover { transform: translateY(-2px); border-color: #d9b55d; background: rgba(7, 15, 24, .68); }
.button-mark { margin-right: 7px; }

.hero-status { display: flex; align-items: center; gap: 18px; margin-top: 44px; color: #c1cbd5; }
.status-item { display: grid; gap: 5px; }
.status-label { color: rgba(215, 225, 234, .6); font: 700 9px/1 var(--font-heading); letter-spacing: .14em; }
.status-value { display: flex; align-items: center; gap: 6px; color: #e2e9ef; font: 600 12px/1.2 var(--font-heading); letter-spacing: .04em; }
.status-value i { width: 7px; height: 7px; border-radius: 50%; background: #90d89e; box-shadow: 0 0 0 4px rgba(144, 216, 158, .12); }
.status-divider { width: 1px; height: 32px; background: rgba(221, 229, 236, .22); }

.hero-side-card {
  padding: 22px;
  color: #d8e1e8;
  border: 1px solid rgba(216, 226, 235, .22);
  border-radius: 12px;
  background: linear-gradient(145deg, rgba(15, 28, 42, .78), rgba(8, 16, 25, .45));
  box-shadow: 0 20px 48px rgba(0, 0, 0, .22);
  backdrop-filter: blur(14px);
}
.side-card-top { display: flex; justify-content: space-between; color: #cfaa52; font: 700 9px/1 var(--font-heading); letter-spacing: .15em; }
.side-orbit { font-size: 18px; line-height: 8px; }
.side-card-mark { margin: 28px 0 16px; color: #f2cf79; font: 500 68px/.7 Georgia, serif; }
.hero-side-card p { margin: 0; color: #c2ced8; font-size: 15px; line-height: 1.65; }
.hero-side-card button { display: flex; gap: 10px; align-items: center; margin-top: 24px; padding: 0; border: 0; color: #f0cb72; background: transparent; cursor: pointer; font: 700 10px/1 var(--font-heading); letter-spacing: .12em; }
.hero-side-card button:hover { color: #fff0b5; }

.hero-scroll { position: absolute; z-index: 3; right: max(40px, calc((100% - 1220px) / 2)); bottom: 30px; display: flex; align-items: center; gap: 10px; border: 0; color: rgba(230, 238, 244, .72); background: transparent; cursor: pointer; font: 700 9px/1 var(--font-heading); letter-spacing: .15em; }
.hero-scroll i { width: 32px; height: 1px; background: currentColor; }
.hero-scroll:hover { color: #e3bb60; }

.lobby-content { background: #08101a; }
.content-section { width: min(1220px, calc(100% - 80px)); margin: 0 auto; padding: 106px 0; }
.feature-section { border-bottom: 1px solid rgba(190, 209, 224, .11); }

.section-heading { display: flex; justify-content: space-between; gap: 32px; align-items: end; margin-bottom: 42px; }
.section-heading h2 { margin: 12px 0 0; color: #f0f4f8; font-size: clamp(28px, 3vw, 42px); letter-spacing: -.035em; }
.section-heading p { max-width: 325px; margin: 0; color: #a8b6c4; font-size: 16px; line-height: 1.6; }

.feature-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.feature-panel { position: relative; min-height: 290px; overflow: hidden; padding: 28px; border: 1px solid rgba(185, 202, 216, .14); border-radius: 12px; background: linear-gradient(145deg, #101c29, #0c1520); transition: transform .25s ease, border-color .25s ease, background .25s ease; }
.feature-panel:hover { transform: translateY(-5px); border-color: rgba(218, 181, 93, .55); background: linear-gradient(145deg, #142434, #0d1824); }
.feature-number { display: block; color: #7f8e9d; font: 700 10px/1 var(--font-heading); letter-spacing: .16em; }
.feature-panel svg { width: 42px; height: 42px; margin: 52px 0 30px; fill: none; stroke: #e2bd68; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.6; }
.feature-panel h3 { margin: 0 0 12px; color: #eff5fa; font-size: 22px; letter-spacing: -.025em; }
.feature-panel p { max-width: 290px; margin: 0; color: #aebbc7; font-size: 15px; line-height: 1.65; }
.feature-line { position: absolute; right: 26px; bottom: 26px; width: 52px; height: 1px; background: #d9b55d; opacity: .8; }

.mode-section { padding-bottom: 124px; }
.mode-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.mode-panel { min-height: 228px; padding: 24px; overflow: hidden; text-align: left; border: 1px solid rgba(185, 202, 216, .14); border-radius: 12px; color: inherit; background: #0e1823; cursor: pointer; transition: transform .25s ease, border-color .25s ease, background .25s ease; }
.mode-panel:hover { transform: translateY(-4px); border-color: rgba(218, 181, 93, .55); background: #132130; }
.mode-index { color: #778696; font: 700 10px/1 var(--font-heading); letter-spacing: .16em; }
.mode-panel-head { display: flex; justify-content: space-between; align-items: center; margin: 28px 0 24px; }
.mode-icon { color: #e3bd66; font: 400 34px/1 Georgia, serif; }
.mode-count { padding: 6px 8px; border: 1px solid rgba(204, 217, 229, .2); border-radius: 4px; color: #c7d1da; font: 700 9px/1 var(--font-heading); letter-spacing: .08em; }
.mode-panel h3 { margin: 0 0 10px; color: #eff5fa; font-size: 19px; letter-spacing: -.02em; }
.mode-panel p { min-height: 45px; margin: 0; color: #96a7b7; font-size: 14px; line-height: 1.55; }
.mode-action { display: inline-flex; gap: 10px; align-items: center; margin-top: 18px; color: #dfba62; font: 700 10px/1 var(--font-heading); letter-spacing: .1em; }
.mode-action b { font-size: 15px; font-weight: 400; }

@media (max-width: 980px) {
  .hero-layout { grid-template-columns: 1fr; padding: 78px 0 104px; }
  .hero-side-card { display: none; }
  .feature-grid, .mode-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 680px) {
  .lobby-page { margin: -20px; }
  .lobby-hero { min-height: 680px; background-position: 62% center; }
  .hero-layout, .content-section { width: min(100% - 40px, 1220px); }
  .hero-layout { padding-top: 90px; }
  .hero-copy h1 { font-size: 43px; }
  .hero-status { gap: 12px; margin-top: 34px; }
  .status-divider { display: none; }
  .hero-scroll { right: 20px; }
  .section-heading { display: block; }
  .section-heading p { margin-top: 18px; }
  .feature-grid, .mode-grid { grid-template-columns: 1fr; }
  .content-section { padding: 72px 0; }
}
</style>

<template>
  <div class="create-room-page">
    <section class="create-intro">
      <div>
        <span class="create-kicker">01 / {{ $locale === 'zh-CN' ? '开启圆桌' : 'OPEN A TABLE' }}</span>
        <h2>{{ $t('createRoom.title') }}</h2>
        <p>{{ $locale === 'zh-CN' ? '设定玩家数量与游戏板子，系统将为这一夜创建专属的推理舞台。' : 'Set the player count and board. The system will prepare a deduction stage for tonight.' }}</p>
      </div>
      <button class="back-link" @click="router.push('/game/room/list')">← {{ $t('common.back') }}</button>
    </section>

    <section class="creation-layout">
      <el-form :model="form" :rules="rules" ref="formRef" class="room-form" label-position="top">
        <div class="form-shell">
          <div class="form-section-title"><span>01</span><div><h3>{{ $locale === 'zh-CN' ? '房间信息' : 'ROOM DETAILS' }}</h3><p>{{ $locale === 'zh-CN' ? '给这一张圆桌一个名字。' : 'Give this table a name.' }}</p></div></div>
          <el-form-item :label="$t('createRoom.roomName')" prop="roomName">
            <el-input v-model="form.roomName" :placeholder="$t('createRoom.roomNameRequired')" maxlength="20" show-word-limit />
          </el-form-item>
          <el-form-item :label="$t('createRoom.password')" prop="password">
            <el-input v-model="form.password" type="password" show-password :placeholder="$t('createRoom.passwordOptional')" />
            <p class="field-hint">{{ $locale === 'zh-CN' ? '留空则任何受邀玩家都可以加入。' : 'Leave empty to allow any invited player to join.' }}</p>
          </el-form-item>
        </div>

        <div class="form-shell board-shell">
          <div class="form-section-title"><span>02</span><div><h3>{{ $locale === 'zh-CN' ? '游戏配置' : 'GAME SETUP' }}</h3><p>{{ $locale === 'zh-CN' ? '选择今晚的玩家数量与角色板子。' : 'Choose player count and a role board for tonight.' }}</p></div></div>
          <el-form-item :label="$t('createRoom.playerCount')" prop="playerCount">
            <div class="count-options">
              <button v-for="count in ['9', '12']" :key="count" type="button" :class="{ active: form.playerCount === count }" @click="form.playerCount = count">
                <b>{{ count }}</b><span>{{ $locale === 'zh-CN' ? '人局' : 'PLAYERS' }}</span>
              </button>
            </div>
          </el-form-item>
          <el-form-item :label="$t('createRoom.gameBoard')" prop="gameBoard">
            <div class="board-options">
              <button v-for="board in boards" :key="board.value" type="button" :class="{ active: form.gameBoard === board.value }" @click="selectBoard(board.value, board.count)">
                <span class="board-mark">{{ board.mark }}</span>
                <span class="board-copy"><b>{{ board.label }}</b><small>{{ board.desc }}</small></span>
                <span class="board-select">{{ form.gameBoard === board.value ? '✓' : '' }}</span>
              </button>
            </div>
          </el-form-item>
        </div>
      </el-form>

      <aside class="creation-summary">
        <span class="summary-kicker">TABLE PREVIEW</span>
        <div class="summary-symbol">{{ selectedBoard.mark }}</div>
        <h3>{{ form.roomName || ($locale === 'zh-CN' ? '未命名圆桌' : 'UNTITLED TABLE') }}</h3>
        <p>{{ selectedBoard.label }}</p>
        <div class="summary-rule"></div>
        <div class="summary-stats">
          <div><span>{{ $locale === 'zh-CN' ? '参与人数' : 'PLAYERS' }}</span><b>{{ form.playerCount }}</b></div>
          <div><span>{{ $locale === 'zh-CN' ? '访问方式' : 'ACCESS' }}</span><b>{{ form.password ? ($locale === 'zh-CN' ? '私密' : 'PRIVATE') : ($locale === 'zh-CN' ? '公开' : 'OPEN') }}</b></div>
        </div>
        <button class="create-submit" :disabled="loading" @click="createRoom"><span>{{ loading ? '⋯' : '✦' }}</span>{{ loading ? ($locale === 'zh-CN' ? '正在创建' : 'CREATING') : $t('createRoom.createRoom') }}</button>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useGameStore } from '../../stores/game'
import { BOARD_CONFIG_SCHEMA_VERSION } from '../../game/rules.js'
import { ElMessage } from 'element-plus'

const { proxy } = getCurrentInstance()
const $t = proxy.$t
const $locale = proxy.$locale
const router = useRouter()
const route = useRoute()
const gameStore = useGameStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  roomName: '',
  playerCount: route.query.playerCount || '9',
  gameBoard: route.query.gameBoard || 'standard',
  password: ''
})

const boards = computed(() => [
  { value: 'standard', count: '9', mark: '◈', label: $t('gameBoard.standard'), desc: $locale === 'zh-CN' ? '经典预言家、女巫与猎人配置' : 'Classic Seer, Witch and Hunter setup' },
  { value: 'wolfking_guard', count: '12', mark: '♛', label: $t('gameBoard.wolfking_guard'), desc: $locale === 'zh-CN' ? '狼王与守卫的攻守博弈' : 'A duel of Wolf King and Guard' },
  { value: 'miracle_merchant', count: '12', mark: '✦', label: $t('gameBoard.miracle_merchant'), desc: $locale === 'zh-CN' ? '奇迹商人带来的变量' : 'Unpredictability from the Merchant' },
  { value: 'wolf_beauty_knight', count: '12', mark: '✧', label: $t('gameBoard.wolf_beauty_knight'), desc: $locale === 'zh-CN' ? '狼美人与骑士的抉择' : 'Choices of Beauty and Knight' },
  { value: 'white_wolf_knight', count: '12', mark: '⚜', label: $t('gameBoard.white_wolf_knight'), desc: $locale === 'zh-CN' ? '白狼王的终局威胁' : 'The White Wolf endgame threat' },
  { value: 'gargoyle_gravedigger', count: '12', mark: '◇', label: $t('gameBoard.gargoyle_gravedigger'), desc: $locale === 'zh-CN' ? '石像鬼与守墓人的信息战' : 'Gargoyle and Gravedigger information war' },
  { value: 'seer_witch_hunter_idiot', count: '12', mark: '♙', label: $t('gameBoard.seer_witch_hunter_idiot'), desc: $locale === 'zh-CN' ? '预女猎白经典平衡场' : 'Classic Seer Witch Hunter Idiot' },
  { value: 'stalker_silencer', count: '12', mark: '⌁', label: $t('gameBoard.stalker_silencer'), desc: $locale === 'zh-CN' ? '潜行刺杀与禁言控制' : 'Stalker strikes and silencer control' },
  { value: 'bear_hunter_idiot', count: '12', mark: '◉', label: $t('gameBoard.bear_hunter_idiot'), desc: $locale === 'zh-CN' ? '熊咆哮的邻座推理' : 'Bear roar adjacency reads' },
  { value: 'nightmare_guard', count: '12', mark: '☾', label: $t('gameBoard.nightmare_guard'), desc: $locale === 'zh-CN' ? '梦魇封锁夜间技能' : 'Nightmare ability lock' },
  { value: 'evil_knight', count: '12', mark: '☠', label: $t('gameBoard.evil_knight'), desc: $locale === 'zh-CN' ? '恶灵骑士反伤地雷' : 'Evil Knight retaliation trap' },
  { value: 'magician_wolfking', count: '12', mark: '🃏', label: $t('gameBoard.magician_wolfking'), desc: $locale === 'zh-CN' ? '魔术师交换目标' : 'Magician target swaps' },
  { value: 'medium_mechanical_wolf', count: '12', mark: '⌘', label: $t('gameBoard.medium_mechanical_wolf'), desc: $locale === 'zh-CN' ? '通灵师与机械狼复制' : 'Medium and Mechanical Wolf' },
  { value: 'black_death', count: '12', mark: '✖', label: $t('gameBoard.black_death'), desc: $locale === 'zh-CN' ? '没有狼人的黑死病谜局' : 'The no-wolf Black Death' },
  { value: 'bomber', count: '12', mark: '▣', label: $t('gameBoard.bomber'), desc: $locale === 'zh-CN' ? '炸弹人反制冲票' : 'Bomber vote retaliation' },
  { value: 'sun_moon', count: '12', mark: '☼', label: $t('gameBoard.sun_moon'), desc: $locale === 'zh-CN' ? '日月改变轮次节奏' : 'Sun and Moon tempo shifts' },
  { value: 'cupid', count: '12', mark: '♡', label: $t('gameBoard.cupid'), desc: $locale === 'zh-CN' ? '丘比特情侣与人狼恋' : 'Cupid lovers and mixed love' },
  { value: 'wolf_brothers', count: '12', mark: '♞', label: $t('gameBoard.wolf_brothers'), desc: $locale === 'zh-CN' ? '狼兄狼弟影子刺客' : 'Wolf Brothers shadow assassin' },
  { value: 'janus', count: '12', mark: '⧉', label: $t('gameBoard.janus'), desc: $locale === 'zh-CN' ? '千面人首夜选身份' : 'Janus first-night choice' },
  { value: 'cursed_fox', count: '12', mark: '🦊', label: $t('gameBoard.cursed_fox'), desc: $locale === 'zh-CN' ? '咒狐独立存活' : 'Cursed Fox solo survival' },
  { value: 'shapeshifter_wolfking', count: '12', mark: '⟲', label: $t('gameBoard.shapeshifter_wolfking'), desc: $locale === 'zh-CN' ? '百变狼王随机变异' : 'Random Shapeshifter Wolf King' }
])

const selectedBoard = computed(() => boards.value.find(board => board.value === form.gameBoard) || boards.value[0])

const rules = {
  roomName: [{ required: true, message: () => $t('createRoom.roomNameRequired'), trigger: 'blur' }, { min: 2, max: 20, message: () => $t('createRoom.roomNameLength'), trigger: 'blur' }],
  playerCount: [{ required: true, message: () => $t('createRoom.playerCountRequired'), trigger: 'change' }],
  gameBoard: [{ required: true, message: () => $t('createRoom.gameBoardRequired'), trigger: 'change' }]
}

const selectBoard = (board, count) => {
  form.gameBoard = board
  form.playerCount = count
}

const createRoom = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async valid => {
    if (!valid) return
    loading.value = true
    const success = await gameStore.createRoom({
      roomName: form.roomName,
      playerCount: Number(form.playerCount),
      gameBoard: form.gameBoard,
      password: form.password,
      boardVersion: String(BOARD_CONFIG_SCHEMA_VERSION || 1),
      status: 1
    })
    loading.value = false
    if (success) {
      ElMessage.success($t('createRoom.createSuccess'))
      router.push(`/game/room/${gameStore.getCurrentRoom.id}?playerCount=${form.playerCount}&gameBoard=${form.gameBoard}`)
    } else {
      ElMessage.error($t('createRoom.createFailed'))
    }
  })
}
</script>

<style scoped>
.create-room-page { width: min(1240px, 100%); margin: 0 auto; padding: 26px 0 84px; }
.create-intro { display: flex; align-items: end; justify-content: space-between; gap: 30px; padding: 30px 0 38px; }.create-kicker, .summary-kicker { color: #d9b55d; font: 700 10px/1 var(--font-heading); letter-spacing: .18em; }.create-intro h2 { margin: 14px 0 12px; color: #eef4f8; font-size: clamp(34px, 4vw, 50px); letter-spacing: -.045em; }.create-intro p { max-width: 540px; margin: 0; color: #9dadbc; font-size: 16px; line-height: 1.65; }.back-link { border: 0; color: #b5c1cc; background: transparent; cursor: pointer; font: 700 10px/1 var(--font-heading); letter-spacing: .1em; }.back-link:hover { color: #e3bd66; }
.creation-layout { display: grid; grid-template-columns: minmax(0, 1fr) 330px; gap: 18px; align-items: start; }.room-form { display: grid; gap: 18px; }.form-shell, .creation-summary { border: 1px solid rgba(180, 204, 222, .18); border-radius: 12px; background: linear-gradient(155deg, #101d2a, #0b141f); }.form-shell { padding: 28px; }.form-section-title { display: flex; gap: 15px; margin-bottom: 28px; }.form-section-title > span { color: #d9b55d; font: 700 11px/1 var(--font-heading); letter-spacing: .16em; }.form-section-title h3 { margin: -4px 0 5px; color: #edf4f8; font-size: 20px; }.form-section-title p { margin: 0; color: #8fa1b1; font-size: 14px; }.field-hint { margin: 8px 0 0; color: #8192a2; font-size: 12px; }
.count-options { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }.count-options button { min-height: 76px; border: 1px solid rgba(180, 204, 222, .18); border-radius: 8px; color: #b8c5cf; background: #0b1520; cursor: pointer; transition: .2s; }.count-options button.active { border-color: #d9b55d; color: #f2cd75; background: rgba(205, 160, 62, .11); }.count-options b { display: block; margin-bottom: 5px; font: 700 28px/1 var(--font-heading); }.count-options span { font: 700 9px/1 var(--font-heading); letter-spacing: .12em; }
.board-options { display: grid; grid-template-columns: repeat(2, 1fr); gap: 9px; }.board-options button { display: flex; align-items: center; gap: 11px; min-height: 74px; padding: 11px; text-align: left; border: 1px solid rgba(180, 204, 222, .16); border-radius: 8px; color: #c8d3dc; background: #0b1520; cursor: pointer; transition: .2s; }.board-options button:hover, .board-options button.active { border-color: rgba(217, 181, 93, .7); background: #132333; }.board-mark { width: 27px; color: #dfb95e; font: 400 25px/1 Georgia, serif; }.board-copy { min-width: 0; flex: 1; }.board-copy b { display: block; overflow: hidden; color: #e8eff4; font: 700 12px/1.3 var(--font-heading); text-overflow: ellipsis; white-space: nowrap; }.board-copy small { display: block; overflow: hidden; margin-top: 5px; color: #8799a9; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.board-select { color: #f1c86e; font-size: 16px; }
.creation-summary { position: sticky; top: 80px; padding: 28px; }.summary-symbol { margin: 55px 0 26px; color: #e3bd66; font: 400 70px/.7 Georgia, serif; }.creation-summary h3 { margin: 0 0 8px; overflow: hidden; color: #f0f5f8; font-size: 24px; text-overflow: ellipsis; white-space: nowrap; }.creation-summary > p { min-height: 42px; margin: 0; color: #99aaba; font-size: 15px; line-height: 1.55; }.summary-rule { height: 1px; margin: 26px 0; background: rgba(180, 204, 222, .15); }.summary-stats { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }.summary-stats span { display: block; margin-bottom: 7px; color: #8495a4; font: 700 9px/1 var(--font-heading); letter-spacing: .12em; }.summary-stats b { color: #dde6ed; font: 700 13px/1.3 var(--font-heading); }.create-submit { width: 100%; min-height: 48px; margin-top: 32px; border: 1px solid #e6c76c; border-radius: 7px; color: #100f0b; background: linear-gradient(135deg, #e8ca70, #bd8d31); cursor: pointer; font: 700 11px/1 var(--font-heading); letter-spacing: .08em; }.create-submit span { margin-right: 7px; }.create-submit:disabled { opacity: .55; cursor: wait; }.create-submit:hover:not(:disabled) { filter: brightness(1.08); }
@media (max-width: 900px) { .creation-layout { grid-template-columns: 1fr; }.creation-summary { position: static; }.board-options { grid-template-columns: 1fr; } }
@media (max-width: 620px) { .create-intro { display: block; }.back-link { margin-top: 22px; }.form-shell { padding: 20px; }.board-options { grid-template-columns: 1fr; } }
</style>

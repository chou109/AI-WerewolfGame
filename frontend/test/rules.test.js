import test from 'node:test'
import assert from 'node:assert/strict'
import {
  BOARD_RULES,
  WOLF_TEAM_ROLES,
  getBoardRules,
  getRoleSummary,
  isPackWolfRole,
  isWolfTeamRole
} from '../src/game/rules.js'

const roleTotal = board => board.roles.reduce((total, item) => total + item.count, 0)

test('每个板子的角色数量与配置人数一致', () => {
  for (const board of Object.values(BOARD_RULES)) {
    assert.equal(roleTotal(board), board.players, `${board.key} 角色总数不匹配`)
    assert.ok(board.roles.length > 0, `${board.key} 至少要配置一个角色`)
    assert.ok(Array.isArray(board.nightOrder), `${board.key} 缺少夜间行动顺序`)
  }
})

test('9人标准局无警长，12人板子默认有警长', () => {
  assert.equal(getBoardRules('standard').players, 9)
  assert.equal(getBoardRules('standard').sheriff, false)
  assert.equal(getBoardRules('wolfking_guard').players, 12)
  assert.equal(getBoardRules('wolfking_guard').sheriff, true)
})

test('未知板子按人数回退到合理规则', () => {
  assert.equal(getBoardRules('missing-board', 9).key, 'standard')
  assert.equal(getBoardRules('missing-board', 12).key, 'wolfking_guard')
  assert.equal(getBoardRules(null, 8).key, 'standard')
})

test('狼人阵营识别区分普通狼队与独立行动角色', () => {
  assert.ok(WOLF_TEAM_ROLES.includes('石像鬼'))
  assert.ok(isWolfTeamRole('石像鬼'))
  assert.ok(isWolfTeamRole('梦魇'))
  assert.ok(isPackWolfRole('狼王'))
  assert.equal(isPackWolfRole('石像鬼'), false)
  assert.equal(isWolfTeamRole('预言家'), false)
})

test('规则摘要包含角色数量，便于主持人提示词复用', () => {
  const summary = getRoleSummary(getBoardRules('miracle_merchant'))
  assert.match(summary, /狼人×3/)
  assert.match(summary, /奇迹商人×1/)
})

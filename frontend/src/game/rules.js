export const WOLF_TEAM_ROLES = ['狼人', '狼王', '狼美人', '白狼王', '石像鬼']
export const PACK_WOLF_ROLES = ['狼人', '狼王', '狼美人', '白狼王']

export const isWolfTeamRole = role => WOLF_TEAM_ROLES.includes(role)
export const isPackWolfRole = role => PACK_WOLF_ROLES.includes(role)

const roles = items => Object.freeze(items.map(([role, count]) => ({ role, count })))

export const BOARD_RULES = Object.freeze({
  standard: Object.freeze({
    key: 'standard',
    players: 9,
    sheriff: false,
    witchCanSelfSave: 'first-night',
    roles: roles([['狼人', 3], ['平民', 3], ['预言家', 1], ['女巫', 1], ['猎人', 1]]),
    nightOrder: ['wolves', 'witch', 'seer'],
    special: '暗牌、无警长、屠边；首夜女巫可以自救；仅首个白天放逐者留遗言。'
  }),
  wolfking_guard: Object.freeze({
    key: 'wolfking_guard',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 3], ['狼王', 1], ['平民', 4], ['预言家', 1], ['女巫', 1], ['猎人', 1], ['守卫', 1]]),
    nightOrder: ['guard', 'wolves', 'witch', 'seer'],
    special: '暗牌屠边、有警长、双爆吞警徽；狼刀在先，同守同救奶穿；狼王被放逐、猎杀或自刀死亡可开枪。'
  }),
  miracle_merchant: Object.freeze({
    key: 'miracle_merchant',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 3], ['狼王', 1], ['平民', 4], ['预言家', 1], ['女巫', 1], ['守卫', 1], ['奇迹商人', 1]]),
    nightOrder: ['miracle', 'guard', 'wolves', 'witch', 'seer'],
    special: '奇迹商人每局首夜先行动，给一名幸运儿一次查验、毒药或守护；幸运儿为狼人时商人次日出局且不给技能。'
  }),
  wolf_beauty_knight: Object.freeze({
    key: 'wolf_beauty_knight',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 3], ['狼美人', 1], ['平民', 4], ['预言家', 1], ['女巫', 1], ['骑士', 1], ['守卫', 1]]),
    nightOrder: ['guard', 'wolves', 'wolfBeauty', 'witch', 'seer'],
    special: '狼美人每夜魅惑一人且不可连续同人，狼美人被放逐或猎杀时魅惑目标殉情；被骑士决斗则不触发。骑士每局一次。'
  }),
  white_wolf_knight: Object.freeze({
    key: 'white_wolf_knight',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 3], ['白狼王', 1], ['平民', 4], ['预言家', 1], ['女巫', 1], ['骑士', 1], ['守卫', 1]]),
    nightOrder: ['guard', 'wolves', 'witch', 'seer'],
    special: '白狼王只能在白天公开发言时自爆并带走一人，立即入夜；投票、遗言、毒杀或骑士决斗不能发动。'
  }),
  gargoyle_gravedigger: Object.freeze({
    key: 'gargoyle_gravedigger',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 3], ['石像鬼', 1], ['平民', 4], ['预言家', 1], ['女巫', 1], ['守墓人', 1], ['猎人', 1]]),
    nightOrder: ['gravedigger', 'gargoyle', 'seer', 'witch', 'wolves'],
    special: '石像鬼不与狼人互知且不参与袭击；首夜先验具体身份，其他狼人出局后才可刀人。守墓人从第二夜起强制获知上个白天放逐者阵营。'
  })
})

export const WEREWOLF_KNOWLEDGE = [
  '金水：预言家查验为好人的结果；查杀：预言家查验为狼人的结果。',
  '银水：女巫用解药救起的人；铜水：守卫守中且恰好挡住狼刀的平安夜。',
  '悍跳：狼人冒充预言家等神职；倒钩狼：站真预言家、卖队友做高身份；冲锋狼：为悍跳狼强势冲票。',
  '上警是参与警长竞选，警下是不竞选，退水是竞选中退出；站边是选择相信某个对跳预言家。',
  '归票是警长在末置位号召集中投票；抗推是把缺乏硬证据的好人推出去；生推是没有预言家信息时靠发言票型找狼。',
  '表水必须解释行为、投票和怀疑链，不是重复“我是好人”；踩是指出疑点，捞是为他人辩护，对话是要求具体回应。',
  '自刀是狼人击杀狼队友骗解药，空刀是狼人夜晚不杀人，绑票是狼队利用票数冲出好人，拍刀是优势狼队直接摊牌。',
  '贴脸和场外信息是禁忌；黑话只改变公开表达，底层推理仍必须转换成“谁因何行为有嫌疑”的结构化证据。'
].join('\n')

export const getBoardRules = (boardKey, playerCount = 12) => {
  if (boardKey && BOARD_RULES[boardKey]) return BOARD_RULES[boardKey]
  return Number(playerCount) <= 9 ? BOARD_RULES.standard : BOARD_RULES.wolfking_guard
}

export const getRoleSummary = board => board.roles.map(item => `${item.role}×${item.count}`).join('、')

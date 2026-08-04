// 板子规则唯一数据源：board_configs.json
// 注意：frontend/src/game/board_configs.json 与 backend/src/main/resources/configs/board_configs.json 必须保持同步。
// 修改任何板子配置时，请同步更新两份文件（或从后端 /game/board/configs 导出覆盖）。
import boardConfigs from './board_configs.json'

export const BOARD_CONFIG_SCHEMA_VERSION = boardConfigs.schemaVersion || 1

export const WOLF_TEAM_ROLES = ['狼人', '狼王', '狼美人', '白狼王', '石像鬼', '梦魇', '恶灵骑士', '机械狼', '狼兄', '狼弟', '百变狼王']
export const PACK_WOLF_ROLES = ['狼人', '狼王', '狼美人', '白狼王', '狼兄', '百变狼王']

export const isWolfTeamRole = role => WOLF_TEAM_ROLES.includes(role)
export const isPackWolfRole = role => PACK_WOLF_ROLES.includes(role)

const freezeBoard = board => Object.freeze({
  ...board,
  roles: Object.freeze((board.roles || []).map(role => Object.freeze({ ...role }))),
  nightOrder: Object.freeze([...(board.nightOrder || [])])
})

export const BOARD_RULES = Object.freeze(
  Object.fromEntries(Object.entries(boardConfigs.boards || {}).map(([key, board]) => [key, freezeBoard(board)]))
)

export const WEREWOLF_KNOWLEDGE = [
  '金水：预言家查验为好人的结果；查杀：预言家查验为狼人的结果。',
  '银水：女巫用解药救起的人；铜水：守卫守中且恰好挡住狼刀的平安夜。',
  '悍跳：狼人冒充预言家等神职；倒钩狼：站真预言家、卖队友做高身份；冲锋狼：为悍跳狼强势冲票。',
  '上警是参与警长竞选，警下是不竞选，退水是竞选中退出；站边是选择相信某个对跳预言家。',
  '归票是警长在末置位号召集中投票；抗推是把缺乏硬证据的好人推出去；生推是没有预言家信息时靠发言票型找狼。',
  '第一次警徽投票允许弃票，俗称“压手”；常规放逐投票也允许弃票，但弃票会留下需要解释的票型。平票后的警徽PK重投或放逐PK重投必须从PK台中选择。',
  '奇迹商人倒牌时，系统不会私下告知商人死因；公开视角应区分狼刀与交易失败反噬两种可能。遗言中交代赠予对象和技能是常见打法，但不是系统强制要求。',
  '奇迹商人若把技能给狼人，狼人不会获得技能且商人因反噬出局；若幸运儿获得查验，应公开报出金水或查杀，但不知道商人是谁。',
  '守卫或幸运儿的一次性守护只能挡住狼人夜刀，不能抵挡女巫或幸运儿的毒药。',
  '夜间死亡与白天放逐是不同出局场景；遗言只能复盘已发生的信息，不能声称自己之后还会听发言、投票或调整判断。',
  '表水必须解释行为、投票和怀疑链，不是重复“我是好人”；踩是指出疑点，捞是为他人辩护，对话是要求具体回应。',
  '自刀是狼人击杀狼队友骗解药，空刀是狼人夜晚不杀人，绑票是狼队利用票数冲出好人，拍刀是优势狼队直接摊牌。',
  '贴脸和场外信息是禁忌；黑话只改变公开表达，底层推理仍必须转换成“谁因何行为有嫌疑”的结构化证据。'
].join('\n')

export const getBoardRules = (boardKey, playerCount = 12) => {
  if (boardKey && BOARD_RULES[boardKey]) return BOARD_RULES[boardKey]
  return Number(playerCount) <= 9 ? BOARD_RULES.standard : BOARD_RULES.wolfking_guard
}

export const getRoleSummary = board => board.roles.map(item => `${item.role}×${item.count}`).join('、')
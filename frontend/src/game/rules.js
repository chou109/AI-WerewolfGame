export const WOLF_TEAM_ROLES = ['狼人', '狼王', '狼美人', '白狼王', '石像鬼', '梦魇', '恶灵骑士', '机械狼', '狼兄', '狼弟', '百变狼王']
export const PACK_WOLF_ROLES = ['狼人', '狼王', '狼美人', '白狼王', '狼兄', '百变狼王']

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
  }),
  seer_witch_hunter_idiot: Object.freeze({
    key: 'seer_witch_hunter_idiot',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 4], ['平民', 4], ['预言家', 1], ['女巫', 1], ['猎人', 1], ['白痴', 1]]),
    nightOrder: ['wolves', 'witch', 'seer'],
    special: '暗牌屠边、有警长；女巫不可自救。白痴被投票放逐时翻牌免死，之后不能投票但仍可发言，狼人需再次夜刀。'
  }),
  stalker_silencer: Object.freeze({
    key: 'stalker_silencer',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 4], ['平民', 4], ['预言家', 1], ['女巫', 1], ['潜行者', 1], ['禁言长老', 1]]),
    nightOrder: ['wolves', 'silencer', 'witch', 'seer'],
    special: '潜行者每局一次：白天投票给某人且该人未被放逐时，潜行者当晚可刺杀该目标。禁言长老每晚禁言一人，不能连续禁言同一目标。'
  }),
  bear_hunter_idiot: Object.freeze({
    key: 'bear_hunter_idiot',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 4], ['平民', 4], ['熊', 1], ['女巫', 1], ['猎人', 1], ['白痴', 1]]),
    nightOrder: ['wolves', 'witch'],
    special: '熊没有主动查验，每天天亮检查相邻两名存活玩家中是否有狼人并宣布熊咆哮；女巫不可自救，白痴翻牌后失去投票权。'
  }),
  nightmare_guard: Object.freeze({
    key: 'nightmare_guard',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 3], ['梦魇', 1], ['平民', 4], ['预言家', 1], ['女巫', 1], ['猎人', 1], ['守卫', 1]]),
    nightOrder: ['nightmare', 'guard', 'wolves', 'witch', 'seer'],
    special: '梦魇入夜第一个行动，恐惧一人使其当夜技能全部失效，不能连续恐惧同一人；恐惧狼人会令狼队空刀。'
  }),
  evil_knight: Object.freeze({
    key: 'evil_knight',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 3], ['恶灵骑士', 1], ['平民', 4], ['预言家', 1], ['女巫', 1], ['猎人', 1], ['守卫', 1]]),
    nightOrder: ['guard', 'wolves', 'witch', 'seer'],
    special: '恶灵骑士属狼人阵营，免疫夜间伤害；被预言家查验则次日预言家出局，被女巫毒杀则次日女巫出局，反伤每局一次。'
  }),
  magician_wolfking: Object.freeze({
    key: 'magician_wolfking',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 3], ['狼王', 1], ['平民', 4], ['预言家', 1], ['女巫', 1], ['猎人', 1], ['魔术师', 1]]),
    nightOrder: ['magician', 'wolves', 'witch', 'seer'],
    special: '魔术师每晚交换两名玩家的夜间目标，不能连续交换同一对；白天猎人开枪不受交换影响。狼王符合死因条件时可开枪。'
  }),
  medium_mechanical_wolf: Object.freeze({
    key: 'medium_mechanical_wolf',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 3], ['机械狼', 1], ['平民', 4], ['通灵师', 1], ['女巫', 1], ['猎人', 1], ['守卫', 1]]),
    nightOrder: ['mechanicalWolf', 'guard', 'wolves', 'witch', 'medium'],
    special: '机械狼不与普通狼人互认，每晚学习一名玩家的具体身份并复制其技能一次；通灵师每晚查验具体身份，机械狼显示为狼人。'
  }),
  black_death: Object.freeze({
    key: 'black_death',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['平民', 8], ['预言家', 1], ['女巫', 1], ['猎人', 1], ['白痴', 1]]),
    nightOrder: ['witch', 'seer'],
    special: '无狼局：所有狼人牌暗中替换为平民，主持人不得告知玩家；所有人必须在没有狼人的信息环境中完成推理。'
  }),
  bomber: Object.freeze({
    key: 'bomber',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 4], ['平民', 4], ['预言家', 1], ['女巫', 1], ['猎人', 1], ['炸弹人', 1]]),
    nightOrder: ['wolves', 'witch', 'seer'],
    special: '炸弹人被投票放逐时翻牌并带走所有投他票的玩家，被带走者不能发动技能；女巫不可自救。'
  }),
  sun_moon: Object.freeze({
    key: 'sun_moon',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 4], ['平民', 3], ['预言家', 1], ['女巫', 1], ['猎人', 1], ['太阳', 1], ['月亮', 1]]),
    nightOrder: ['wolves', 'witch', 'seer'],
    special: '太阳死亡后额外进行一个白天，月亮死亡后额外进行一个夜晚；同夜死亡按太阳后月亮顺序结算。'
  }),
  cupid: Object.freeze({
    key: 'cupid',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 3], ['平民', 4], ['预言家', 1], ['女巫', 1], ['猎人', 1], ['守卫', 1], ['丘比特', 1]]),
    nightOrder: ['cupid', 'guard', 'wolves', 'witch', 'seer'],
    special: '丘比特首夜连接两名情侣并让其互认；一人死亡另一人殉情。人狼恋成为第三方阵营，需杀光其他玩家。'
  }),
  wolf_brothers: Object.freeze({
    key: 'wolf_brothers',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 2], ['狼兄', 1], ['狼弟', 1], ['平民', 4], ['预言家', 1], ['女巫', 1], ['猎人', 1], ['守卫', 1]]),
    nightOrder: ['guard', 'wolves', 'witch', 'seer'],
    special: '狼兄狼弟首夜互认；狼兄存活时狼弟闭眼且预言家验为好人，狼兄死亡后下一夜狼弟苏醒并显示为狼人。'
  }),
  janus: Object.freeze({
    key: 'janus',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 3], ['平民', 4], ['预言家', 1], ['女巫', 1], ['猎人', 1], ['守卫', 1], ['千面人', 1]]),
    nightOrder: ['janus', 'guard', 'wolves', 'witch', 'seer'],
    special: '千面人首夜最先从两张身份牌中选择，若出现狼人牌必须选狼人；之后按新身份的阵营和技能行动。'
  }),
  cursed_fox: Object.freeze({
    key: 'cursed_fox',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 3], ['平民', 4], ['预言家', 1], ['女巫', 1], ['猎人', 1], ['守卫', 1], ['咒狐', 1]]),
    nightOrder: ['guard', 'wolves', 'witch', 'seer'],
    special: '咒狐独自存活到最后获胜；免疫狼人夜刀，但被预言家查验立即死亡，属于第三方阵营。'
  }),
  shapeshifter_wolfking: Object.freeze({
    key: 'shapeshifter_wolfking',
    players: 12,
    sheriff: true,
    witchCanSelfSave: false,
    roles: roles([['狼人', 3], ['平民', 4], ['预言家', 1], ['女巫', 1], ['猎人', 1], ['摄梦人', 1], ['熊', 1]]),
    nightOrder: ['dreamer', 'wolves', 'witch', 'seer'],
    special: '开局从预言家、女巫、猎人、摄梦人、熊中随机抽一张变异为百变狼王；百变狼王属于狼人阵营并复制该角色的变异技能。'
  })
})

export const WEREWOLF_KNOWLEDGE = [
  '金水：预言家查验为好人的结果；查杀：预言家查验为狼人的结果。',
  '银水：女巫用解药救起的人；铜水：守卫守中且恰好挡住狼刀的平安夜。',
  '悍跳：狼人冒充预言家等神职；倒钩狼：站真预言家、卖队友做高身份；冲锋狼：为悍跳狼强势冲票。',
  '上警是参与警长竞选，警下是不竞选，退水是竞选中退出；站边是选择相信某个对跳预言家。',
  '归票是警长在末置位号召集中投票；抗推是把缺乏硬证据的好人推出去；生推是没有预言家信息时靠发言票型找狼。',
  '常规警徽投票和常规放逐投票允许弃票（压手），但弃票会留下需要解释的票型；只有平票后的警徽PK重投或放逐PK重投必须从PK台中选择。',
  '奇迹商人倒牌时，公开视角必须区分狼刀与交易失败反噬两种可能；商人应准确交代赠予目标和技能，幸运儿再用是否收到技能及技能结果核对逻辑。',
  '奇迹商人若把技能给狼人，狼人不会获得技能且商人因反噬出局；若幸运儿获得查验，应公开报出金水或查杀，但不知道商人是谁。',
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

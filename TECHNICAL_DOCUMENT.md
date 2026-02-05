# AI狼人杀游戏技术文档

## 项目概述

AI狼人杀是一个基于人工智能的狼人杀游戏系统，使用大语言模型（LLM）驱动AI玩家进行游戏。该项目采用前后端分离架构，后端使用Spring Boot，前端使用Vue 3，提供了完整的游戏框架和用户界面。

## 技术栈

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.5.14 | 核心框架 |
| MyBatis-Plus | 3.4.3.4 | ORM框架，简化数据库操作 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 6.0+ | 缓存、Session管理 |
| Druid | 1.2.15 | 数据库连接池 |
| JWT | 0.11.2 | 用户身份认证 |
| Swagger/OpenAPI 3 | 3.0.0 | API文档生成 |
| Quartz | - | 定时任务调度 |
| Lombok | 1.18.30 | 简化代码 |
| Hutool | 5.2.5 | 工具类库 |
| POI | 4.1.2 | Excel导入导出 |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue 3 | 3.x | 前端核心框架（Composition API） |
| Vite | 7.x | 现代化构建工具 |
| Element Plus | 2.x | UI组件库 |
| Vue Router | 4.x | 路由管理 |
| Pinia | 2.x | 状态管理 |
| Axios | 1.x | HTTP请求库 |
| SCSS | 1.x | CSS预处理器 |

## 项目结构

### 后端结构

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/werewolf/game/
│   │   │   ├── Application.java       # 主启动类
│   │   │   ├── controller/            # 控制器层
│   │   │   ├── service/               # 服务层
│   │   │   ├── service/impl/          # 服务实现层
│   │   │   ├── mapper/                # 数据访问层
│   │   │   ├── entity/                # 实体类
│   │   │   ├── config/                # 配置类
│   │   │   └── utils/                 # 工具类
│   │   └── resources/
│   │       ├── application.yml        # 应用配置
│   │       ├── mapper/                # XML映射文件
│   │       └── static/                # 静态资源
│   └── test/                          # 测试代码
├── pom.xml                            # Maven配置
└── README.md                          # 后端说明文档
```

### 前端结构

```
frontend/
├── src/
│   ├── assets/                        # 静态资源
│   ├── components/                    # 通用组件
│   ├── views/                         # 页面组件
│   │   ├── Login.vue                  # 登录页
│   │   ├── Register.vue               # 注册页
│   │   ├── Home.vue                   # 首页
│   │   └── game/                      # 游戏相关页面
│   ├── router/                        # 路由配置
│   ├── stores/                        # 状态管理
│   ├── utils/                         # 工具类
│   ├── api/                           # API请求
│   ├── App.vue                        # 根组件
│   └── main.js                        # 入口文件
├── public/                            # 公共资源
├── index.html                         # HTML模板
├── vite.config.js                     # Vite配置
├── package.json                       # 依赖配置
└── README.md                          # 前端说明文档
```

### 提示词模板结构

```
templates/
├── judge_prompt.txt                   # 法官系统提示词
├── player_prompt.txt                  # AI玩家通用提示词
└── roles/                             # 角色策略模板
    ├── werewolf_strategy.txt          # 狼人策略
    ├── seer_strategy.txt              # 预言家策略
    ├── witch_strategy.txt             # 女巫策略
    └── civilian_strategy.txt          # 平民策略
```

### 游戏配置结构

```
configs/
├── 9p_standard.json                   # 9人标准局配置
├── 12p_wolfking_guard.json            # 12人狼王守卫场配置
└── 12p_miracle_merchant.json          # 12人奇迹商人场配置
```

## 核心功能

### 1. 用户系统

- **用户认证**：登录、注册、密码重置
- **权限管理**：基于JWT的身份认证
- **用户信息**：个人资料管理、历史记录

### 2. 游戏房间系统

- **房间管理**：创建、加入、离开房间
- **房间配置**：设置游戏人数、板子类型、规则
- **玩家管理**：玩家加入、角色分配、状态管理

### 3. 游戏核心系统

- **法官系统**：严格按照规则主持游戏，控制流程
- **AI玩家**：基于提示词系统驱动的AI玩家，具有不同性格和策略
- **角色系统**：支持狼人、预言家、女巫、猎人、守卫、奇迹商人等角色
- **游戏流程**：夜晚行动、白天发言、投票放逐、胜利判定

### 4. API管理系统

- **API配置**：设置API地址、参数、认证信息
- **API分配**：根据角色性格和策略分配API
- **测试功能**：测试API连接、响应时间、稳定性

### 5. 语音系统

- **TTS集成**：支持OpenAI TTS、Azure Speech、Google TTS等
- **个性化语音**：根据角色性格和情绪生成不同风格的语音
- **语音管理**：控制语音播放、音量、语速等

### 6. 游戏记录系统

- **全程记录**：记录所有发言、行动、投票
- **战报生成**：生成详细的游戏战报
- **数据分析**：分析游戏数据、玩家表现

## 数据库设计

### 1. 用户表（user）

| 字段名 | 数据类型 | 描述 |
|--------|----------|------|
| id | BIGINT | 用户ID |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(100) | 密码（MD5加密） |
| nickname | VARCHAR(50) | 昵称 |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| status | INT | 状态（1-正常，0-禁用） |
| avatar | VARCHAR(255) | 头像URL |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| last_login_time | DATETIME | 最后登录时间 |

### 2. 游戏房间表（game_room）

| 字段名 | 数据类型 | 描述 |
|--------|----------|------|
| id | BIGINT | 房间ID |
| room_code | VARCHAR(20) | 房间号 |
| room_name | VARCHAR(100) | 房间名称 |
| player_count | INT | 玩家数量 |
| game_board | VARCHAR(50) | 游戏板子 |
| status | INT | 状态（1-等待中，2-游戏中，3-已结束） |
| creator_id | BIGINT | 创建者ID |
| create_time | DATETIME | 创建时间 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| winner | VARCHAR(20) | 获胜阵营 |

### 3. 游戏玩家表（game_player）

| 字段名 | 数据类型 | 描述 |
|--------|----------|------|
| id | BIGINT | 玩家ID |
| room_id | BIGINT | 房间ID |
| user_id | BIGINT | 用户ID |
| player_number | INT | 玩家编号 |
| role | VARCHAR(50) | 角色 |
| personality | VARCHAR(50) | 性格 |
| strategy | VARCHAR(50) | 策略 |
| status | INT | 状态（1-存活，0-死亡） |
| is_sheriff | INT | 是否为警长（1-是，0-否） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 4. 游戏记录表（game_record）

| 字段名 | 数据类型 | 描述 |
|--------|----------|------|
| id | BIGINT | 记录ID |
| room_id | BIGINT | 房间ID |
| day_number | INT | 天数 |
| phase | VARCHAR(20) | 阶段（night-夜晚，day-白天） |
| action_type | VARCHAR(50) | 行动类型 |
| action_content | TEXT | 行动内容 |
| player_id | BIGINT | 玩家ID |
| target_player | VARCHAR(50) | 目标玩家 |
| create_time | DATETIME | 创建时间 |

## API接口

### 1. 用户接口

| 接口路径 | 方法 | 描述 |
|----------|------|------|
| /api/user/login | POST | 用户登录 |
| /api/user/register | POST | 用户注册 |
| /api/user/info/{id} | GET | 获取用户信息 |
| /api/user/update | PUT | 更新用户信息 |
| /api/user/updatePassword | PUT | 更新密码 |

### 2. 游戏房间接口

| 接口路径 | 方法 | 描述 |
|----------|------|------|
| /api/game/room/create | POST | 创建游戏房间 |
| /api/game/room/info/{id} | GET | 获取房间信息 |
| /api/game/room/byCode/{code} | GET | 根据房间号获取房间 |
| /api/game/room/available | GET | 获取可用房间列表 |
| /api/game/room/updateStatus | PUT | 更新房间状态 |
| /api/game/room/start | PUT | 开始游戏 |
| /api/game/room/end | PUT | 结束游戏 |

### 3. 游戏玩家接口

| 接口路径 | 方法 | 描述 |
|----------|------|------|
| /api/game/player/list/{roomId} | GET | 获取房间玩家列表 |
| /api/game/player/info/{id} | GET | 获取玩家信息 |
| /api/game/player/add | POST | 添加玩家到房间 |
| /api/game/player/updateStatus | PUT | 更新玩家状态 |
| /api/game/player/updateRole | PUT | 更新玩家角色 |
| /api/game/player/setSheriff | PUT | 设置警长 |
| /api/game/player/alive/{roomId} | GET | 获取房间存活玩家 |

### 4. 游戏记录接口

| 接口路径 | 方法 | 描述 |
|----------|------|------|
| /api/game/record/list/{roomId} | GET | 获取房间游戏记录 |
| /api/game/record/day/{roomId}/{day} | GET | 获取房间某天的游戏记录 |
| /api/game/record/add | POST | 添加游戏记录 |
| /api/game/record/latest/{roomId} | GET | 获取房间最新记录 |

### 5. API管理接口

| 接口路径 | 方法 | 描述 |
|----------|------|------|
| /api/api/config | GET | 获取API配置 |
| /api/api/config | POST | 更新API配置 |
| /api/api/test | POST | 测试API连接 |
| /api/api/allocate | POST | 分配API |

### 6. 语音系统接口

| 接口路径 | 方法 | 描述 |
|----------|------|------|
| /api/voice/config | GET | 获取语音配置 |
| /api/voice/config | POST | 更新语音配置 |
| /api/voice/generate | POST | 生成语音 |
| /api/voice/test | POST | 测试语音引擎 |

## 使用说明

### 1. 环境准备

- **后端环境**：JDK 8+, Maven 3.6+, MySQL 8.0+, Redis 6.0+
- **前端环境**：Node.js 18+, npm 9+
- **AI模型**：支持OpenAI GPT、Anthropic Claude、百度文心一言等大语言模型
- **TTS服务**：支持OpenAI TTS、Azure Speech、Google TTS等

### 2. 项目启动

#### 后端启动

1. 配置数据库连接：修改 `backend/src/main/resources/application.yml` 中的数据库配置
2. 配置Redis连接：修改 `backend/src/main/resources/application.yml` 中的Redis配置
3. 编译项目：`mvn clean package`
4. 启动项目：`java -jar backend/target/ai-werewolf-game-1.0.0.jar`

#### 前端启动

1. 安装依赖：`cd frontend && npm install`
2. 配置API地址：修改 `frontend/src/main.js` 中的 `axios.defaults.baseURL`
3. 开发模式启动：`npm run dev`
4. 生产模式构建：`npm run build`

### 3. 游戏流程

#### 1. 创建房间

1. 登录系统
2. 选择游戏模式（9人局或12人局）
3. 选择游戏板子（标准场、狼王守卫场、奇迹商人场）
4. 设置房间名称和密码（可选）
5. 创建房间

#### 2. 配置AI

1. 进入API配置页面
2. 配置AI API地址、认证信息
3. 设置API性格和策略
4. 测试API连接
5. 保存配置

#### 3. 开始游戏

1. 等待玩家加入（或直接开始AI游戏）
2. 系统自动分配角色
3. 法官宣布游戏开始
4. 进入夜晚阶段：狼人杀人、预言家查验、女巫用药、守卫守护
5. 进入白天阶段：宣布死亡信息、警长竞选（第一天）、玩家发言、投票放逐
6. 重复夜晚和白天阶段，直到某阵营获胜

#### 4. 游戏结束

1. 系统宣布获胜阵营
2. 生成游戏战报
3. 保存游戏记录

### 4. 配置说明

#### 1. 游戏配置

修改 `configs/` 目录下的JSON文件，配置游戏人数、角色、规则等。

#### 2. 提示词配置

修改 `templates/` 目录下的提示词文件，调整法官和玩家的提示词内容。

#### 3. API配置

在前端API配置页面设置API相关信息，或直接修改 `src/api/api_config.json` 文件。

#### 4. 语音配置

在前端语音配置页面设置TTS相关信息，或直接修改 `src/voice/voice_config.json` 文件。

## 技术亮点

### 1. 智能AI玩家

- **多性格系统**：支持谨慎型、激进型、逻辑型、直觉型、表演型、沉默型等多种性格
- **多策略系统**：支持保守、进攻、平衡、随机、学习型等多种策略
- **自适应行为**：根据游戏局势和其他玩家行为调整策略
- **真实对话**：使用大语言模型生成自然、符合角色性格的对话

### 2. 完整的法官系统

- **严格规则**：严格按照狼人杀规则主持游戏
- **流程控制**：精确控制游戏流程，确保规则正确执行
- **状态管理**：实时跟踪游戏状态，处理各种特殊情况
- **公平公正**：保持中立，不偏向任何阵营

### 3. 灵活的配置系统

- **多板子支持**：支持多种游戏板子配置
- **自定义规则**：可根据需要调整游戏规则
- **API适配**：支持多种大语言模型API
- **语音定制**：支持多种TTS引擎和语音风格

### 4. 现代化技术架构

- **前后端分离**：使用Vue 3 + Spring Boot的前后端分离架构
- **微服务潜力**：模块化设计，易于扩展为微服务架构
- **容器化支持**：支持Docker容器化部署
- **云原生兼容**：可部署到各种云平台

### 5. 丰富的功能扩展

- **数据分析**：可扩展游戏数据分析功能
- **AI训练**：可基于游戏数据训练自定义AI模型
- **社交系统**：可扩展好友、聊天、排行榜等社交功能
- **多语言支持**：可扩展多语言界面和语音

## 项目未来规划

### 1. 功能增强

- **更多角色**：添加骑士、白痴、魔术师、摄梦人等角色
- **更多板子**：支持更多自定义板子配置
- **AI优化**：优化AI决策逻辑，提高游戏体验
- **语音增强**：支持更多TTS引擎和语音风格

### 2. 技术升级

- **Spring Boot 3.0**：升级到Spring Boot 3.0
- **Vue 4.0**：跟进Vue 4.0的新特性
- **微服务架构**：将项目拆分为微服务
- **容器化部署**：提供Docker镜像和Kubernetes配置

### 3. 生态建设

- **开发者文档**：完善开发者文档，支持插件开发
- **API开放**：开放API接口，支持第三方集成
- **社区建设**：建立玩家和开发者社区
- **赛事系统**：支持AI狼人杀比赛

### 4. 商业化探索

- **高级功能**：提供付费高级功能
- **AI模型**：训练和销售自定义AI模型
- **企业服务**：为企业提供团队建设、决策训练等服务
- **教育应用**：将游戏应用于逻辑思维、沟通表达等教育领域

## 总结

AI狼人杀游戏是一个融合了人工智能、游戏设计、现代Web技术的创新项目，为用户提供了全新的游戏体验。项目采用了现代化的技术架构，具有良好的扩展性和可维护性，为未来的功能增强和技术升级奠定了坚实的基础。

通过大语言模型驱动的AI玩家，游戏不仅提供了单人游戏的乐趣，也为研究AI行为、多智能体交互等领域提供了实验平台。同时，项目的语音系统、数据分析等功能，也为游戏增添了更多的趣味性和实用性。

未来，随着AI技术的不断发展和游戏功能的不断完善，AI狼人杀游戏有望成为一个受欢迎的休闲娱乐平台，同时也为人工智能的应用探索新的方向。

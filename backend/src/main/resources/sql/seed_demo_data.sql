-- 本地演示 AI 配置；不包含真实 Provider 密钥。该文件仅由显式开启的演示数据开关执行。
-- 演示账号仅供本机数据库验证：demo / demo123；不要在生产环境启用演示数据。
INSERT INTO user (username, password, nickname, email, status)
SELECT 'demo', MD5('demo123'), '本地演示账号', 'demo@localhost.invalid', 1
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'demo');

INSERT INTO ai_player (
  name, model_type, model_name, personality, strategy, language, api_key, api_base_url,
  temperature, max_tokens, status
)
SELECT
  '演示玩家-DeepSeek', 'deepseek', 'deepseek-chat', '谨慎、重视逻辑链', '分析公开发言与投票关系，不编造信息', 'zh-CN', '', 'https://api.deepseek.com/v1',
  7, 1000, 1
WHERE NOT EXISTS (SELECT 1 FROM ai_player WHERE name = '演示玩家-DeepSeek');

INSERT INTO ai_player (
  name, model_type, model_name, personality, strategy, language, api_key, api_base_url,
  temperature, max_tokens, status
)
SELECT
  '演示玩家-OpenAI', 'openai', 'gpt-4o-mini', '平衡、表达清楚', '先梳理证据，再给出候选判断', 'zh-CN', '', 'https://api.openai.com/v1',
  7, 1000, 1
WHERE NOT EXISTS (SELECT 1 FROM ai_player WHERE name = '演示玩家-OpenAI');

INSERT INTO ai_player (
  name, model_type, model_name, personality, strategy, language, api_key, api_base_url,
  temperature, max_tokens, status
)
SELECT
  '演示玩家-Claude', 'claude', 'claude-3-5-sonnet-20241022', '谨慎、善于归纳', '区分已知事实与推测，避免暴露无关隐私', 'zh-CN', '', 'https://api.anthropic.com/v1',
  7, 1000, 1
WHERE NOT EXISTS (SELECT 1 FROM ai_player WHERE name = '演示玩家-Claude');

INSERT INTO ai_player (
  name, model_type, model_name, personality, strategy, language, api_key, api_base_url,
  temperature, max_tokens, status
)
SELECT
  '演示玩家-ModelScope', 'modelscope', 'Qwen/Qwen2.5-72B-Instruct', '平和、条理清晰', '根据公开信息发言，遵守当前角色目标', 'zh-CN', '', 'https://api-inference.modelscope.cn/v1',
  7, 1000, 1
WHERE NOT EXISTS (SELECT 1 FROM ai_player WHERE name = '演示玩家-ModelScope');

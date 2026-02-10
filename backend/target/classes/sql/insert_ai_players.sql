-- 插入AI玩家示例数据
INSERT INTO ai_player (name, model_type, model_name, personality, strategy, api_key, api_base_url, temperature, max_tokens, status) VALUES
('智能狼人', 'openai', 'gpt-3.5-turbo', '狡猾型', '伪装好人，寻找神位', 'sk-test-key-1', 'https://api.openai.com/v1/chat/completions', 7, 500, 1),
('冷静预言家', 'openai', 'gpt-3.5-turbo', '谨慎型', '报查验信息，带队分析', 'sk-test-key-2', 'https://api.openai.com/v1/chat/completions', 7, 500, 1),
('机智女巫', 'openai', 'gpt-3.5-turbo', '逻辑型', '隐藏身份，救好人，毒狼人', 'sk-test-key-3', 'https://api.openai.com/v1/chat/completions', 7, 500, 1),
('勇敢猎人', 'openai', 'gpt-3.5-turbo', '激进型', '隐藏身份，准备开枪', 'sk-test-key-4', 'https://api.openai.com/v1/chat/completions', 7, 500, 1),
('普通村民', 'openai', 'gpt-3.5-turbo', '直觉型', '凭感觉发言，直觉判断', 'sk-test-key-5', 'https://api.openai.com/v1/chat/completions', 7, 500, 1),
('神秘守卫', 'openai', 'gpt-3.5-turbo', '沉默型', '言简意赅，少言寡语', 'sk-test-key-6', 'https://api.openai.com/v1/chat/completions', 7, 500, 1),
('奇迹商人', 'openai', 'gpt-3.5-turbo', '表演型', '生动形象，善于煽动', 'sk-test-key-7', 'https://api.openai.com/v1/chat/completions', 7, 500, 1),
('狼王', 'openai', 'gpt-3.5-turbo', '谨慎型', '伪装好人，保护同伴', 'sk-test-key-8', 'https://api.openai.com/v1/chat/completions', 7, 500, 1),
('平民A', 'openai', 'gpt-3.5-turbo', '逻辑型', '分析局势，站边好人', 'sk-test-key-9', 'https://api.openai.com/v1/chat/completions', 7, 500, 1),
('平民B', 'openai', 'gpt-3.5-turbo', '表演型', '生动形象，善于煽动', 'sk-test-key-10', 'https://api.openai.com/v1/chat/completions', 7, 500, 1),
('平民C', 'openai', 'gpt-3.5-turbo', '直觉型', '凭感觉发言，直觉判断', 'sk-test-key-11', 'https://api.openai.com/v1/chat/completions', 7, 500, 1),
('平民D', 'openai', 'gpt-3.5-turbo', '沉默型', '言简意赅，少言寡语', 'sk-test-key-12', 'https://api.openai.com/v1/chat/completions', 7, 500, 1);

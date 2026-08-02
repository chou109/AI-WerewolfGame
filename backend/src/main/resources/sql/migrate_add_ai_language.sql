ALTER TABLE `ai_player`
  ADD COLUMN `language` VARCHAR(10) NOT NULL DEFAULT 'zh-CN' COMMENT '对局发言语言' AFTER `strategy`;

-- 创建AI玩家表
CREATE TABLE IF NOT EXISTS `ai_player` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL COMMENT 'AI玩家名称',
  `model_type` varchar(50) NOT NULL COMMENT '模型类型',
  `model_name` varchar(255) NOT NULL COMMENT '模型名称',
  `personality` varchar(255) DEFAULT NULL COMMENT '个性',
  `strategy` varchar(255) DEFAULT NULL COMMENT '策略',
  `api_key` varchar(255) NOT NULL COMMENT 'API Key',
  `api_base_url` varchar(255) DEFAULT NULL COMMENT 'API Base URL',
  `temperature` int(11) DEFAULT '7' COMMENT '温度',
  `max_tokens` int(11) DEFAULT '1000' COMMENT '最大tokens',
  `status` int(11) DEFAULT '1' COMMENT '状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='AI玩家表';

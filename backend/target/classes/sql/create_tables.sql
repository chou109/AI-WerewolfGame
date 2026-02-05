CREATE TABLE IF NOT EXISTS user (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(100) NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  email VARCHAR(100) NOT NULL,
  phone VARCHAR(20) DEFAULT NULL,
  status INT(11) DEFAULT 1,
  avatar VARCHAR(255) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  last_login_time DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS game_room (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  room_code VARCHAR(20) NOT NULL,
  room_name VARCHAR(100) NOT NULL,
  player_count INT(11) DEFAULT 0,
  game_board TEXT DEFAULT NULL,
  status INT(11) DEFAULT 1,
  creator_id BIGINT(20) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  start_time DATETIME DEFAULT NULL,
  end_time DATETIME DEFAULT NULL,
  winner VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_room_code (room_code),
  KEY idx_creator_id (creator_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS game_player (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  room_id BIGINT(20) NOT NULL,
  user_id BIGINT(20) NOT NULL,
  player_number INT(11) NOT NULL,
  role VARCHAR(50) DEFAULT NULL,
  personality VARCHAR(255) DEFAULT NULL,
  strategy TEXT DEFAULT NULL,
  status INT(11) DEFAULT 1,
  is_sheriff INT(11) DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_room_id (room_id),
  KEY idx_user_id (user_id),
  KEY idx_room_user (room_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS game_record (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  room_id BIGINT(20) NOT NULL,
  day_number INT(11) NOT NULL,
  phase VARCHAR(50) NOT NULL,
  action_type VARCHAR(50) NOT NULL,
  action_content TEXT NOT NULL,
  player_id BIGINT(20) DEFAULT NULL,
  target_player VARCHAR(255) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_room_id (room_id),
  KEY idx_day_phase (room_id, day_number, phase)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
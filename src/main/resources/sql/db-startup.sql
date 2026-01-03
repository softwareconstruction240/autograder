CREATE TABLE IF NOT EXISTS `user` (
    `net_id` varchar(20) NOT NULL,
    `canvas_user_id` int NOT NULL,
    `first_name` varchar(50) NOT NULL,
    `last_name` varchar(50) NOT NULL,
    `repo_url` varchar(200),
    `role` varchar(15) NOT NULL,
    PRIMARY KEY (`net_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `submission` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `net_id` VARCHAR(20) NOT NULL,
    `repo_url` VARCHAR(200) NOT NULL,
    `head_hash` VARCHAR(40) NOT NULL,
    `timestamp` DATETIME NOT NULL,
    `phase` VARCHAR(9) NOT NULL,
    `passed` BOOL NOT NULL,
    `score` FLOAT NOT NULL,
    `raw_score` FLOAT NOT NULL,
    `notes` TEXT,
    `rubric` JSON,
    `verified_status` VARCHAR(30),
    `commit_context` JSON,
    `commit_result` JSON,
    `verification` JSON,
    `admin` BOOL NOT NULL,
    PRIMARY KEY (`id`),
    INDEX sort_index (`net_id`,`phase`,`passed`,`score`,`timestamp`),
    CONSTRAINT `submission_net_id`
        FOREIGN KEY (`net_id`)
        REFERENCES `user` (`net_id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `queue` (
    `net_id` VARCHAR(20) NOT NULL,
    `phase` VARCHAR(9) NOT NULL,
    `time_added` DATETIME NOT NULL,
    `started` BOOL,
    PRIMARY KEY (`net_id`),
    CONSTRAINT `queue_net_id`
        FOREIGN KEY (`net_id`)
        REFERENCES `user` (`net_id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `rubric_config` (
    `phase` VARCHAR(9) NOT NULL,
    `type` VARCHAR(15) NOT NULL,
    `category` TEXT NOT NULL,
    `criteria` TEXT NOT NULL,
    `points` INT NOT NULL,
    `rubric_id` VARCHAR(15),
    PRIMARY KEY (`phase`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `configuration` (
    `config_key` VARCHAR(50) NOT NULL,
    `value` TEXT NOT NULL,
    PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `repo_update` (
    `timestamp` TIMESTAMP NOT NULL,
    `net_id` VARCHAR(255) NOT NULL,
    `repo_url` VARCHAR(2048) NOT NULL,
    `admin_update` BOOLEAN NOT NULL,
    `admin_net_id` VARCHAR(255),
    PRIMARY KEY (`timestamp`),
    CONSTRAINT `repo_net_id`
        FOREIGN KEY (`net_id`)
        REFERENCES `user` (`net_id`)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
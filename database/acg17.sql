/*
 Navicat Premium Dump SQL

 Source Server         : WSL2-acg17
 Source Server Type    : MySQL
 Source Server Version : 80411 (8.4.11)
 Source Host           : 127.0.0.1:3306
 Source Schema         : acg17

 Target Server Type    : MySQL
 Target Server Version : 80411 (8.4.11)
 File Encoding         : 65001

 Date: 13/08/2026 00:33:10
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for game
-- ----------------------------
DROP TABLE IF EXISTS `game`;
CREATE TABLE `game`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '游戏id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '游戏名称',
  `chinese_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '中文名称',
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '游戏版本',
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '游戏封面路径',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '游戏图标',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '游戏简介',
  `preview_images` json NULL COMMENT '游戏预览图，存储多张预览图路径的JSON数组',
  `favorite` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '收藏状态：0-未收藏，1-已收藏',
  `deleted` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `user_id` int NOT NULL COMMENT '用户id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_game_owner_state_id`(`user_id` ASC, `deleted` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_game_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_game_deleted` CHECK (`deleted` in (0,1)),
  CONSTRAINT `chk_game_favorite` CHECK (`favorite` in (0,1))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '游戏信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for illustration
-- ----------------------------
DROP TABLE IF EXISTS `illustration`;
CREATE TABLE `illustration`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '插画id',
  `path` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '插画的路径地址',
  `size` int NOT NULL COMMENT '插画大小',
  `ratio` double(8, 6) NULL DEFAULT NULL COMMENT '宽高比（宽/高）',
  `sort_order` int NOT NULL COMMENT '排序序号，数值越大越靠前',
  `deleted` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
  `user_id` int NOT NULL COMMENT '用户id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_illustration_owner_state_sort`(`user_id` ASC, `deleted` ASC, `sort_order` DESC, `id` DESC) USING BTREE,
  UNIQUE INDEX `uk_illustration_user_sort`(`user_id` ASC, `sort_order` ASC) USING BTREE,
  CONSTRAINT `fk_illustration_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_illustration_deleted` CHECK (`deleted` in (0,1))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for manga
-- ----------------------------
DROP TABLE IF EXISTS `manga`;
CREATE TABLE `manga`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '漫画ID，自增主键',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `chinese_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '中文标题',
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面图片路径',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '简介',
  `pages` json NULL COMMENT '页数据，存储所有话(集)和对应的页集合，格式：[{\"chapter\":1,\"title\":\"第1话\",\"pagelist\":[{\"page\":1,\"path\":\"/path/to/page1.jpg\"},{\"page\":2,\"path\":\"/path/to/page2.jpg\"}]}]',
  `size` bigint NOT NULL DEFAULT 0 COMMENT '大小，单位为字节',
  `favorite` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '收藏状态，0-未收藏，1-已收藏',
  `deleted` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0-未删除，1-已删除',
  `user_id` int NOT NULL COMMENT '用户id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_manga_id_user`(`id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_manga_owner_state_id`(`user_id` ASC, `deleted` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_manga_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_manga_deleted` CHECK (`deleted` in (0,1)),
  CONSTRAINT `chk_manga_favorite` CHECK (`favorite` in (0,1))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '漫画表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for manga_tag
-- ----------------------------
DROP TABLE IF EXISTS `manga_tag`;
CREATE TABLE `manga_tag`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '标签ID，自增主键',
  `user_id` int NOT NULL COMMENT '标签所属用户ID',
  `tag_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签名称',
  `category` tinyint UNSIGNED NOT NULL COMMENT '分类标记，1-角色，2-男性，3-女性，4-混合，5-其他，6-原作，7-艺术家，8-团队',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_manga_tag_user_category_name`(`user_id` ASC, `category` ASC, `tag_name` ASC) USING BTREE,
  UNIQUE INDEX `uk_manga_tag_id_user`(`id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_manga_tag_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_manga_tag_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_manga_tag_category` CHECK (`category` between 1 and 8)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '漫画标签表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for manga_tag_relation
-- ----------------------------
DROP TABLE IF EXISTS `manga_tag_relation`;
CREATE TABLE `manga_tag_relation`  (
  `manga_id` int NOT NULL COMMENT '漫画id',
  `tag_id` int NOT NULL COMMENT '标签id',
  `user_id` int NOT NULL COMMENT '用户id',
  PRIMARY KEY (`manga_id`, `tag_id`) USING BTREE,
  INDEX `idx_manga_tag_relation_tag`(`tag_id` ASC, `manga_id` ASC) USING BTREE,
  INDEX `idx_manga_tag_relation_manga_owner`(`manga_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_manga_tag_relation_tag_owner`(`tag_id` ASC, `user_id` ASC) USING BTREE,
  CONSTRAINT `fk_manga_tag_relation_manga_owner` FOREIGN KEY (`manga_id`, `user_id`) REFERENCES `manga` (`id`, `user_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_manga_tag_relation_tag_owner` FOREIGN KEY (`tag_id`, `user_id`) REFERENCES `manga_tag` (`id`, `user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel
-- ----------------------------
DROP TABLE IF EXISTS `novel`;
CREATE TABLE `novel`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '小说ID，自增主键',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '书名',
  `author` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '作者',
  `total_words` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '总字数',
  `deleted` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `user_id` int NOT NULL COMMENT '用户id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_novel_owner_state_id`(`user_id` ASC, `deleted` ASC, `id` ASC) USING BTREE,
  UNIQUE INDEX `uk_novel_id_user`(`id` ASC, `user_id` ASC) USING BTREE,
  CONSTRAINT `fk_novel_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_novel_deleted` CHECK (`deleted` in (0,1))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for novel_chapter
-- ----------------------------
DROP TABLE IF EXISTS `novel_chapter`;
CREATE TABLE `novel_chapter`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '章节ID，自增主键',
  `novel_id` int NOT NULL COMMENT '小说id',
  `title` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '章节标题',
  `content` json NOT NULL COMMENT '章节内容',
  `total_words` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '章节字数',
  `sort_order` int UNSIGNED NOT NULL COMMENT '章节排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_novel_chapter_order`(`novel_id` ASC, `sort_order` ASC) USING BTREE,
  CONSTRAINT `fk_novel_chapter_novel` FOREIGN KEY (`novel_id`) REFERENCES `novel` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel_tag
-- ----------------------------
DROP TABLE IF EXISTS `novel_tag`;
CREATE TABLE `novel_tag`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '小说标签id',
  `user_id` int NOT NULL COMMENT '用户id',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签名称',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_novel_tag_user_name`(`user_id` ASC, `name` ASC) USING BTREE,
  UNIQUE INDEX `uk_novel_tag_id_user`(`id` ASC, `user_id` ASC) USING BTREE,
  CONSTRAINT `fk_novel_tag_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for novel_tag_relation
-- ----------------------------
DROP TABLE IF EXISTS `novel_tag_relation`;
CREATE TABLE `novel_tag_relation`  (
  `novel_id` int NOT NULL COMMENT '小说id',
  `tag_id` int NOT NULL COMMENT '标签id',
  `user_id` int NOT NULL COMMENT '用户id',
  PRIMARY KEY (`novel_id`, `tag_id`) USING BTREE,
  INDEX `idx_novel_tag_relation_tag`(`tag_id` ASC, `novel_id` ASC) USING BTREE,
  INDEX `idx_novel_tag_relation_novel_owner`(`novel_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_novel_tag_relation_tag_owner`(`tag_id` ASC, `user_id` ASC) USING BTREE,
  CONSTRAINT `fk_novel_tag_relation_novel_owner` FOREIGN KEY (`novel_id`, `user_id`) REFERENCES `novel` (`id`, `user_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_novel_tag_relation_tag_owner` FOREIGN KEY (`tag_id`, `user_id`) REFERENCES `novel_tag` (`id`, `user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户密码',
  `auth_version` int NOT NULL DEFAULT 1 COMMENT '认证版本。递增后，之前签发的令牌全部失效。',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最近一次成功登录时间',
  `deleted` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除（0否1是）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_username`(`username` ASC) USING BTREE,
  CONSTRAINT `chk_user_auth_version` CHECK (`auth_version` >= 1),
  CONSTRAINT `chk_user_deleted` CHECK (`deleted` in (0,1))
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info`  (
  `user_id` int NOT NULL COMMENT '用户id',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户昵称',
  `avatar_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像路径',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE,
  CONSTRAINT `fk_user_info_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Initial administrator account
-- Username: admin
-- Initial password: admin123
-- Change the password immediately after the first login.
-- ----------------------------
INSERT INTO `user` (
  `id`, `username`, `password`, `auth_version`, `last_login_time`,
  `deleted`, `create_time`, `update_time`
) VALUES (
  1,
  'admin',
  '$2b$12$s7Z2gP0oCX/NeP7DiB5vEOmTVHlPAntm7Q.2L//gmr4hUyBY.Gol.',
  1,
  '2026-01-01 00:00:00',
  0,
  '2026-01-01 00:00:00',
  '2026-01-01 00:00:00'
);

INSERT INTO `user_info` (
  `user_id`, `nickname`, `avatar_path`, `create_time`, `update_time`
) VALUES (
  1,
  'admin',
  NULL,
  '2026-01-01 00:00:00',
  '2026-01-01 00:00:00'
);

SET FOREIGN_KEY_CHECKS = 1;

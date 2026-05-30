SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS enpense_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE enpense_system;

-- 1. 报销单主表
-- 存列表页和详情页基础信息，一张报销单一条记录。
CREATE TABLE IF NOT EXISTS fk_reim_main (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  reim_no VARCHAR(64) NOT NULL COMMENT '报销单号，唯一',
  bill_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态：DRAFT草稿，SUBMITTED已提交，APPROVING审批中，APPROVED审批通过，VOIDED已作废',
  bill_status_name VARCHAR(64) NOT NULL DEFAULT '草稿' COMMENT '单据状态名称',
  bill_type VARCHAR(32) NOT NULL DEFAULT 'TRAVEL_REIMBURSEMENT' COMMENT '单据类型编码',
  bill_type_name VARCHAR(64) NOT NULL DEFAULT '差旅费用报销单' COMMENT '单据类型名称',

  reimburser_id VARCHAR(64) NOT NULL COMMENT '报销人ID，前端写死选项的ID',
  reimburser_no VARCHAR(64) DEFAULT NULL COMMENT '报销人工号',
  reimburser_name VARCHAR(100) NOT NULL COMMENT '报销人姓名',

  reim_department_id VARCHAR(64) NOT NULL COMMENT '报销部门ID，前端写死选项的ID',
  reim_department_no VARCHAR(64) DEFAULT NULL COMMENT '报销部门编号',
  reim_department_name VARCHAR(100) NOT NULL COMMENT '报销部门名称',

  business_type_id VARCHAR(64) NOT NULL COMMENT '业务类型ID，前端写死选项的ID',
  business_type_no VARCHAR(64) DEFAULT NULL COMMENT '业务类型编号',
  business_type_name VARCHAR(100) NOT NULL COMMENT '业务类型名称',

  title VARCHAR(500) NOT NULL COMMENT '报销标题',
  reason VARCHAR(500) NOT NULL COMMENT '报销事由',

  allowance_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '补助总金额',
  meal_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '餐费补助合计',
  traffic_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '交通补助合计',
  communication_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '通讯补助合计',

  remark VARCHAR(1000) DEFAULT NULL COMMENT '备注信息',
  submitted_at DATETIME DEFAULT NULL COMMENT '提交时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (id),
  UNIQUE KEY uk_fk_reim_main_reim_no (reim_no),
  KEY idx_fk_reim_main_status (bill_status),
  KEY idx_fk_reim_main_reimburser (reimburser_id),
  KEY idx_fk_reim_main_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报销单主表';

-- 2. 行程表
-- 一个报销单可以有多个行程；一个行程会生成多条每日补助明细。
CREATE TABLE IF NOT EXISTS fk_reim_itinerary (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  main_id BIGINT NOT NULL COMMENT '报销单主表ID',

  traveler_id VARCHAR(64) NOT NULL COMMENT '出行人ID，前端写死选项的ID',
  traveler_no VARCHAR(64) DEFAULT NULL COMMENT '出行人工号',
  traveler_name VARCHAR(100) NOT NULL COMMENT '出行人姓名',

  start_city_code VARCHAR(32) NOT NULL COMMENT '出发城市编码',
  start_city_name VARCHAR(100) NOT NULL COMMENT '出发城市名称',
  end_city_code VARCHAR(32) NOT NULL COMMENT '到达城市编码',
  end_city_name VARCHAR(100) NOT NULL COMMENT '到达城市名称',

  start_date DATE NOT NULL COMMENT '出发日期',
  end_date DATE NOT NULL COMMENT '到达日期',
  days INT NOT NULL COMMENT '行程天数，包含开始和结束日期',
  route_text VARCHAR(255) NOT NULL COMMENT '行程展示文本，如：武汉 - 北京',
  description VARCHAR(500) NOT NULL COMMENT '行程说明',

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (id),
  KEY idx_fk_reim_itinerary_main_id (main_id),
  KEY idx_fk_reim_itinerary_traveler_date (main_id, traveler_id, start_date, end_date),
  KEY idx_fk_reim_itinerary_end_city (end_city_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报销单行程表';

-- 3. 每日补助明细表
-- 一个行程按日期生成多条记录；用户每天勾选哪些补助、填写多少金额，都存在这里。
CREATE TABLE IF NOT EXISTS fk_reim_allowance_day (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  main_id BIGINT NOT NULL COMMENT '报销单主表ID',
  itinerary_id BIGINT NOT NULL COMMENT '行程表ID',

  allowance_date DATE NOT NULL COMMENT '补助日期',
  week_name VARCHAR(16) NOT NULL COMMENT '星期名称，如：星期一',

  city_code VARCHAR(32) NOT NULL COMMENT '补助城市编码，通常取行程到达城市',
  city_name VARCHAR(100) NOT NULL COMMENT '补助城市名称',
  city_level TINYINT NOT NULL COMMENT '城市等级：1一线，2二线，3三线',

  meal_standard DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '餐费补助标准金额',
  meal_selected TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否选择餐费补助：1是，0否',
  meal_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '实际餐费补助金额',

  traffic_standard DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '交通补助标准金额',
  traffic_selected TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否选择交通补助：1是，0否',
  traffic_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '实际交通补助金额',

  communication_standard DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '通讯补助标准金额',
  communication_selected TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否选择通讯补助：1是，0否',
  communication_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '实际通讯补助金额',

  day_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '当日补助合计',

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (id),
  UNIQUE KEY uk_fk_reim_allowance_day (itinerary_id, allowance_date),
  KEY idx_fk_reim_allowance_day_main_id (main_id),
  KEY idx_fk_reim_allowance_day_city (city_code),
  KEY idx_fk_reim_allowance_day_date (allowance_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日补助明细表';

-- 4. 城市补助标准表
-- 后端生成每日补助明细、校验补助金额上限时使用。
CREATE TABLE IF NOT EXISTS fk_city_allowance (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  city_code VARCHAR(32) NOT NULL COMMENT '城市编码',
  city_name VARCHAR(100) NOT NULL COMMENT '城市名称',
  city_level TINYINT NOT NULL COMMENT '城市等级：1一线，2二线，3三线',
  city_level_name VARCHAR(32) NOT NULL COMMENT '城市等级名称',
  meal_standard DECIMAL(12,2) NOT NULL COMMENT '餐费补助标准金额',
  traffic_standard DECIMAL(12,2) NOT NULL DEFAULT 40.00 COMMENT '交通补助标准金额',
  communication_standard DECIMAL(12,2) NOT NULL DEFAULT 40.00 COMMENT '通讯补助标准金额',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (id),
  UNIQUE KEY uk_fk_city_allowance_city_code (city_code),
  KEY idx_fk_city_allowance_level (city_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='城市补助标准表';

-- 5. 费用归属及分摊表
-- 对应 5.2.2.6 费用归属及分摊；一个报销单可以有多条分摊。
CREATE TABLE IF NOT EXISTS fk_reim_allocation (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  main_id BIGINT NOT NULL COMMENT '报销单主表ID',

  allocation_owner_type VARCHAR(32) NOT NULL COMMENT '分摊归属方类型：COMPANY公司，DEPARTMENT部门',
  allocation_owner_id VARCHAR(64) NOT NULL COMMENT '分摊归属方ID，前端写死选项的ID',
  allocation_owner_no VARCHAR(64) DEFAULT NULL COMMENT '分摊归属方编号',
  allocation_owner_name VARCHAR(100) NOT NULL COMMENT '分摊归属方名称，如公司名称或部门名称',

  business_id VARCHAR(64) DEFAULT NULL COMMENT '分摊业务ID，前端写死选项的ID',
  business_name VARCHAR(100) DEFAULT NULL COMMENT '分摊业务名称',

  allocation_ratio DECIMAL(10,6) NOT NULL DEFAULT 0.000000 COMMENT '分摊比例，数据库存 0-1，页面展示为百分比',
  allocation_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '分摊金额',
  sort_no INT NOT NULL DEFAULT 1 COMMENT '排序号',

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (id),
  KEY idx_fk_reim_allocation_main_id (main_id),
  KEY idx_fk_reim_allocation_owner (allocation_owner_type, allocation_owner_id),
  KEY idx_fk_reim_allocation_business (business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用归属及分摊表';

-- 城市补助标准初始化数据，来自概要设计 5.3.5 城市控件数据和 5.2.2.4 补助规则。
-- 文档规则：一线餐补100，二线餐补80，三线餐补50；交通补助和通讯补助均为40。
INSERT INTO fk_city_allowance (
  city_code,
  city_name,
  city_level,
  city_level_name,
  meal_standard,
  traffic_standard,
  communication_standard
) VALUES
('10119', '北京', 1, '一线城市', 100.00, 40.00, 40.00),
('10621', '上海', 1, '一线城市', 100.00, 40.00, 40.00),
('10458', '武汉', 2, '二线城市', 80.00, 40.00, 40.00),
('10216', '杭州', 2, '二线城市', 80.00, 40.00, 40.00),
('10455', '荆州', 3, '三线城市', 50.00, 40.00, 40.00)
ON DUPLICATE KEY UPDATE
  city_name = VALUES(city_name),
  city_level = VALUES(city_level),
  city_level_name = VALUES(city_level_name),
  meal_standard = VALUES(meal_standard),
  traffic_standard = VALUES(traffic_standard),
  communication_standard = VALUES(communication_standard),
  updated_at = CURRENT_TIMESTAMP;

CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(40) NULL COMMENT '本地登录账号',
    password_hash VARCHAR(100) NULL COMMENT 'BCrypt 密码哈希',
    display_name VARCHAR(80) NOT NULL DEFAULT '' COMMENT '显示名称',
    mobile VARCHAR(30) NOT NULL DEFAULT '' COMMENT '手机号，当前预留',
    wechat_no VARCHAR(80) NOT NULL DEFAULT '' COMMENT '微信号，当前预留',
    qq_no VARCHAR(40) NOT NULL DEFAULT '' COMMENT 'QQ号，当前预留',
    avatar_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像地址',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    last_login_time DATETIME NULL COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

CREATE TABLE sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(60) NOT NULL COMMENT '角色编码：super_admin/user',
    role_name VARCHAR(80) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '角色说明',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

CREATE TABLE sys_permission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    permission_type VARCHAR(20) NOT NULL DEFAULT 'api' COMMENT '权限类型：api/menu/button',
    description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '权限说明',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_permission_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限';

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系';

CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关系';

CREATE TABLE sys_oauth_account (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '第三方账号绑定ID',
    user_id BIGINT NOT NULL COMMENT '系统用户ID',
    provider VARCHAR(30) NOT NULL COMMENT '提供方：wechat/qq 等',
    provider_account_id VARCHAR(120) NOT NULL COMMENT '第三方平台账号唯一标识',
    union_id VARCHAR(120) NULL COMMENT '平台统一标识，可为空',
    nickname VARCHAR(120) NOT NULL DEFAULT '' COMMENT '第三方昵称',
    avatar_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT '第三方头像',
    bind_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_oauth_provider_account (provider, provider_account_id),
    UNIQUE KEY uk_sys_oauth_user_provider (user_id, provider),
    KEY idx_sys_oauth_union_id (union_id),
    CONSTRAINT fk_sys_oauth_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方账号绑定';

CREATE TABLE sys_login_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '登录记录ID',
    user_id BIGINT NULL COMMENT '用户ID，失败场景可为空',
    username VARCHAR(40) NOT NULL DEFAULT '' COMMENT '登录账号快照',
    login_type VARCHAR(30) NOT NULL DEFAULT 'password' COMMENT '登录方式',
    success TINYINT NOT NULL DEFAULT 0 COMMENT '是否成功',
    reason VARCHAR(80) NOT NULL DEFAULT '' COMMENT '结果原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_sys_login_record_username_created (username, create_time),
    KEY idx_sys_login_record_user_created (user_id, create_time),
    CONSTRAINT fk_sys_login_record_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录记录';
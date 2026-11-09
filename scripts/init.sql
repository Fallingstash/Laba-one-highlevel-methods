-- Schema for the access-control application (H2). Run before fill.sql.

CREATE TABLE users (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    login     VARCHAR(100) NOT NULL UNIQUE,
    algorithm VARCHAR(20)  NOT NULL,
    salt_hex  VARCHAR(64)  NOT NULL,
    hash_hex  VARCHAR(128) NOT NULL
);

-- Resource tree: top-level resources have parent_id = NULL.
-- "path" is the full dotted path (A.B.C), stored so a resource is found with one indexed query.
CREATE TABLE resources (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    parent_id  INT NULL,
    name       VARCHAR(20)  NOT NULL,
    path       VARCHAR(500) NOT NULL UNIQUE,
    max_volume INT NOT NULL,
    CONSTRAINT fk_resources_parent FOREIGN KEY (parent_id) REFERENCES resources (id),
    CONSTRAINT chk_resources_name CHECK (REGEXP_LIKE(name, '^[A-Za-z0-9_]{1,20}$')),
    CONSTRAINT chk_resources_volume CHECK (max_volume >= 0)
);

-- One row = one action allowed for a user on a resource (and, by inheritance, on its subtree).
CREATE TABLE permissions (
    user_id     INT NOT NULL,
    resource_id INT NOT NULL,
    action      VARCHAR(10) NOT NULL,
    PRIMARY KEY (user_id, resource_id, action),
    CONSTRAINT fk_permissions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_permissions_resource FOREIGN KEY (resource_id) REFERENCES resources (id) ON DELETE CASCADE,
    CONSTRAINT chk_permissions_action CHECK (action IN ('READ', 'WRITE', 'EXECUTE'))
);

CREATE INDEX idx_permissions_user ON permissions (user_id);

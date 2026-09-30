-- IAM: e-post, status, roll-lista och inloggningsmetoder per konto.

ALTER TABLE app_users ADD COLUMN email VARCHAR(255);
ALTER TABLE app_users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE app_users ADD COLUMN last_login_at TIMESTAMP;
ALTER TABLE app_users ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE app_users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- E-post är unik när den är satt (flera NULL tillåts). En person har högst ett konto.
CREATE UNIQUE INDEX uq_app_users_email ON app_users (email);
CREATE UNIQUE INDEX uq_app_users_person ON app_users (person_id);

CREATE TABLE app_user_roles (
    user_id      BIGINT       NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    role_name    VARCHAR(100) NOT NULL,
    access_level VARCHAR(2)   NOT NULL,
    PRIMARY KEY (user_id, role_name)
);

CREATE TABLE app_user_login_methods (
    user_id BIGINT      NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    method  VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, method)
);

-- Befintliga roller: MEMBER blir member:rw, ADMIN och HARBOUR_MASTER blir admin:rw.
INSERT INTO app_user_roles (user_id, role_name, access_level)
SELECT id, CASE WHEN role = 'MEMBER' THEN 'member' ELSE 'admin' END, 'RW' FROM app_users;

INSERT INTO app_user_login_methods (user_id, method)
SELECT id, 'PASSWORD' FROM app_users;

ALTER TABLE app_users DROP COLUMN role;

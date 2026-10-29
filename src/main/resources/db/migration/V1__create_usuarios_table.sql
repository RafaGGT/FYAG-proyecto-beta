CREATE TABLE usuarios (
                          id                    BIGSERIAL PRIMARY KEY,
                          nombre                VARCHAR(100) NOT NULL,
                          apellido              VARCHAR(100) NOT NULL,
                          rut                   VARCHAR(20)  NOT NULL UNIQUE,
                          email                 VARCHAR(150) NOT NULL UNIQUE,
                          telefono              VARCHAR(20)  NULL,
                          username              VARCHAR(20)  NOT NULL UNIQUE,
                          password              VARCHAR(255) NOT NULL,
                          role                  VARCHAR(20)  NOT NULL
                              CHECK (role IN ('ADMIN','GERENCIA','FINANZAS','SUPERVISOR','TECNICO')),
                          activo                BOOLEAN      NOT NULL DEFAULT TRUE,
                          debe_cambiar_password BOOLEAN      NOT NULL DEFAULT FALSE,
                          intentos_fallidos     INTEGER      NOT NULL DEFAULT 0,
                          bloqueado_hasta       TIMESTAMP    NULL
);

INSERT INTO usuarios (nombre, apellido, rut, email, telefono, username, password, role, debe_cambiar_password)
VALUES (
           'Admin',
           'Sistema',
           '11111111-1',
           'admin@fyag.cl',
           '+56900000000',
           'admin',
           '$2b$10$.hvl.5v3hezce.XlhqXj5eYruT9X0aGIueRA09MPn8TyHTBeiBbE6',
           'ADMIN',
           TRUE
       );
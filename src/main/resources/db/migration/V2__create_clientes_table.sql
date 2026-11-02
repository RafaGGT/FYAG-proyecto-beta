CREATE TABLE clientes (
                          id            BIGSERIAL PRIMARY KEY,
                          rut           VARCHAR(20)  NOT NULL UNIQUE,
                          razon_social  VARCHAR(150) NOT NULL,
                          direccion     VARCHAR(200) NULL,
                          email         VARCHAR(150) NULL,
                          telefono      VARCHAR(20)  NULL
);

-- Búsqueda por razón social (HU-01: búsqueda por RUT o razón social).
-- El RUT ya tiene índice por su restricción UNIQUE.
CREATE INDEX idx_clientes_razon_social ON clientes (LOWER(razon_social));
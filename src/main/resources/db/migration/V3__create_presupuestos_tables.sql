-- Número correlativo del presupuesto (Tabla 5: numero_presupuesto)
CREATE SEQUENCE seq_numero_presupuesto START 1;

CREATE TABLE presupuestos (
                              id                  BIGSERIAL PRIMARY KEY,
                              numero_presupuesto  VARCHAR(30)   NOT NULL UNIQUE,
                              cliente_id          BIGINT        NOT NULL REFERENCES clientes (id),
                              estado              VARCHAR(20)   NOT NULL
                                  CHECK (estado IN ('PENDIENTE','APROBADO','RECHAZADO')),
                              fecha_creacion      DATE          NOT NULL,
                              fecha_aprobacion    DATE          NULL,
                              motivo_rechazo      VARCHAR(500)  NULL,
                              monto_total         NUMERIC(14,2) NOT NULL
);

CREATE TABLE items_presupuesto (
                                   id              BIGSERIAL PRIMARY KEY,
                                   presupuesto_id  BIGINT        NOT NULL REFERENCES presupuestos (id),
                                   descripcion     VARCHAR(300)  NOT NULL,
                                   monto           NUMERIC(14,2) NOT NULL
);
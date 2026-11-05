CREATE TABLE obras (
                       id                BIGSERIAL     PRIMARY KEY,
                       nombre            VARCHAR(30)   NOT NULL,
                       descripcion       VARCHAR(500)  NULL,
                       estado            VARCHAR(20)   NOT NULL
                           CHECK (estado IN ('EN_PROCESO','FINALIZADA','SUSPENDIDA','CANCELADA')),
                       fecha_inicio      DATE          NOT NULL,
                       fecha_termino     DATE          NULL,
                       motivo_suspension VARCHAR(500)  NULL,
                       presupuesto_id    BIGINT        NOT NULL UNIQUE REFERENCES presupuestos(id),
                       tipo_servicio_id  BIGINT        NOT NULL REFERENCES tipos_servicio(id),
                       tecnico_id        BIGINT        NULL REFERENCES usuarios(id)
);
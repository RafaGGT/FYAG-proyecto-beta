CREATE TABLE facturas (
                          id                BIGSERIAL      PRIMARY KEY,
                          numero_factura    VARCHAR(30)    NOT NULL UNIQUE,
                          obra_id           BIGINT         NOT NULL REFERENCES obras(id),
                          fecha_emision     DATE           NOT NULL,
                          fecha_vencimiento DATE           NOT NULL,
                          fecha_pago        DATE           NULL,
                          monto             NUMERIC(14,2)  NOT NULL,
                          estado_pago       VARCHAR(20)    NOT NULL
                              CHECK (estado_pago IN ('PENDIENTE','PAGADO')),
                          anulada           BOOLEAN        NOT NULL DEFAULT FALSE,
                          motivo_anulacion  VARCHAR(500)   NULL,
                          anulada_por       BIGINT         NULL REFERENCES usuarios(id),
                          fecha_anulacion   TIMESTAMP      NULL
);
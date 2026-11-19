-- Novedades que dejan los técnicos (y el supervisor) sobre una obra. Solo se agregan: no se editan ni se borran.
CREATE TABLE incidencias_obra (
                                  id        BIGSERIAL    PRIMARY KEY,
                                  obra_id   BIGINT       NOT NULL REFERENCES obras(id),
                                  autor_id  BIGINT       NOT NULL REFERENCES usuarios(id),
                                  tipo      VARCHAR(20)  NOT NULL
                                      CHECK (tipo IN ('COMENTARIO','AUSENCIA','RETRASO','FALTA_MATERIAL','PROBLEMA_ACCESO')),
                                  texto     VARCHAR(500) NOT NULL,
                                  fecha     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_incidencias_obra ON incidencias_obra (obra_id, fecha DESC);

-- Avisos guardados en el servidor: cada usuario ve los suyos desde cualquier dispositivo.
CREATE TABLE notificaciones (
                                id              BIGSERIAL    PRIMARY KEY,
                                destinatario_id BIGINT       NOT NULL REFERENCES usuarios(id),
                                obra_id         BIGINT       NULL REFERENCES obras(id),
                                tipo            VARCHAR(30)  NOT NULL
                                    CHECK (tipo IN ('OBRA_ASIGNADA','OBRA_RETIRADA','CAMBIO_ESTADO','LISTA_PARA_FACTURAR','INCIDENCIA')),
                                mensaje         VARCHAR(300) NOT NULL,
                                tono            VARCHAR(10)  NOT NULL CHECK (tono IN ('info','ok','warn','bad')),
                                leida           BOOLEAN      NOT NULL DEFAULT FALSE,
                                fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notif_destinatario ON notificaciones (destinatario_id, leida, fecha_creacion DESC);
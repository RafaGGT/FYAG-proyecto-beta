CREATE TABLE costos_reales (
                               id             BIGSERIAL     PRIMARY KEY,
                               obra_id        BIGINT        NOT NULL UNIQUE REFERENCES obras(id),
                               materiales     NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (materiales >= 0),
                               horas_tecnicas NUMERIC(8,2)  NOT NULL DEFAULT 0 CHECK (horas_tecnicas >= 0),
                               valor_hora     NUMERIC(14,2) NOT NULL CHECK (valor_hora >= 0)
);
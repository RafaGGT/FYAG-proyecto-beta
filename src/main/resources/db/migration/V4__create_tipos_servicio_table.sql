CREATE TABLE tipos_servicio (
                                id      BIGSERIAL    PRIMARY KEY,
                                nombre  VARCHAR(80)  NOT NULL UNIQUE,
                                activo  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Tipos de servicio definidos en HU-06
INSERT INTO tipos_servicio (nombre) VALUES
                                        ('Proyecto'),
                                        ('Mantención'),
                                        ('Climatización'),
                                        ('Proyecto eléctrico'),
                                        ('Emergencia'),
                                        ('Garantía'),
                                        ('Venta de repuestos'),
                                        ('Mejoras');
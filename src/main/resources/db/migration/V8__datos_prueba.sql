-- Datos de prueba FYAG (solo staging). Solo INSERT, se aplica con Flyway después de V1 a V7.
-- Credenciales (usuario = contraseña, ninguna pide cambio de clave):
--   gerencia / gerencia   finanzas / finanzas   supervisor / supervisor
--   tecnico1 / tecnico1   tecnico2 / tecnico2
-- Fechas relativas a hoy. Presupuestos numerados PRES-T001.. para no chocar con la secuencia de la app.

-- Usuarios
INSERT INTO usuarios (nombre, apellido, rut, email, telefono, username, password, role, activo, debe_cambiar_password) VALUES
                                                                                                                           ('Camila',  'Rojas',   '10000001-1', 'gerencia@fyag.cl',   '+56911111111', 'gerencia',   '$2a$10$q35s.eSWNoDBqVIoWVKmb.Qc0AlIgPFcerdwHhzkddTJXvIjJk6Ha', 'GERENCIA',   TRUE, FALSE),
                                                                                                                           ('Paula',   'Soto',    '10000002-2', 'finanzas@fyag.cl',   '+56922222222', 'finanzas',   '$2a$10$1u1yQAaHrT2eRk2GQvRL2eiUK7XKbGT7aQSdEdqawYEHIrvyLxnuC', 'FINANZAS',   TRUE, FALSE),
                                                                                                                           ('Marcelo', 'Fuentes', '10000003-3', 'supervisor@fyag.cl', '+56933333333', 'supervisor', '$2a$10$0IdRtVAC0QKFKAPLax3azem2tRh3nDg5pZzpJ9rORmnU/IOyH8.pq', 'SUPERVISOR', TRUE, FALSE),
                                                                                                                           ('Diego',   'Araya',   '10000004-4', 'tecnico1@fyag.cl',  '+56944444444', 'tecnico1',   '$2a$10$YU0hGNlW.VAWP5COlaP6wewjIxy1GZc0V5WzjWtR2M5rt8boXvWFu', 'TECNICO',    TRUE, FALSE),
                                                                                                                           ('Javier',  'Mella',   '10000005-5', 'tecnico2@fyag.cl',  '+56955555555', 'tecnico2',   '$2a$10$S9fsvXYnEoimTDxdmo4O0OrinEgB7Mf.qxIHA1uDDbpN30jsshlqW', 'TECNICO',    TRUE, FALSE);

-- Clientes
INSERT INTO clientes (rut, razon_social, direccion, email, telefono) VALUES
                                                                         ('76111111-1', 'Packing Ñuble SpA',                  'Camino a Coihueco km 5, Chillán', 'contacto@packingnuble.cl',  '+56422111111'),
                                                                         ('76222222-2', 'Viña Los Robles Ltda.',              'Ruta N-59, Quillón',              'compras@vinalosrobles.cl',  '+56422222222'),
                                                                         ('76333333-3', 'Frutícola Andes S.A.',               'Parcela 12, San Carlos',          'admin@fruticolaandes.cl',   '+56422333333'),
                                                                         ('76444444-4', 'Centro de Distribución Chillán SpA', 'Av. Collín 1500, Chillán',        'logistica@cdchillan.cl',    '+56422444444');

-- Presupuestos: T001-T005 aprobados con obra, T006 aprobado sin obra,
-- T007 pendiente bajo el umbral, T008 pendiente sobre el umbral (5.000.000), T009 rechazado
INSERT INTO presupuestos (numero_presupuesto, cliente_id, estado, fecha_creacion, fecha_aprobacion, motivo_rechazo, monto_total)
SELECT v.numero, c.id, v.estado, v.creacion, v.aprobacion, v.motivo, v.monto
FROM (VALUES
          ('PRES-T001', '76111111-1', 'APROBADO',  CURRENT_DATE - 75,  CURRENT_DATE - 70, NULL::text,                        3200000),
          ('PRES-T002', '76222222-2', 'APROBADO',  CURRENT_DATE - 100, CURRENT_DATE - 95, NULL::text,                        8500000),
          ('PRES-T003', '76111111-1', 'APROBADO',  CURRENT_DATE - 30,  CURRENT_DATE - 25, NULL::text,                        4100000),
          ('PRES-T004', '76333333-3', 'APROBADO',  CURRENT_DATE - 20,  CURRENT_DATE - 15, NULL::text,                        12000000),
          ('PRES-T005', '76444444-4', 'APROBADO',  CURRENT_DATE - 12,  CURRENT_DATE - 10, NULL::text,                        2800000),
          ('PRES-T006', '76333333-3', 'APROBADO',  CURRENT_DATE - 5,   CURRENT_DATE - 3,  NULL::text,                        1500000),
          ('PRES-T007', '76222222-2', 'PENDIENTE', CURRENT_DATE - 2,   NULL::date,        NULL::text,                        3900000),
          ('PRES-T008', '76444444-4', 'PENDIENTE', CURRENT_DATE - 1,   NULL::date,        NULL::text,                        9800000),
          ('PRES-T009', '76111111-1', 'RECHAZADO', CURRENT_DATE - 40,  NULL::date,        'Cliente optó por otro proveedor', 6000000)
     ) AS v(numero, rut, estado, creacion, aprobacion, motivo, monto)
         JOIN clientes c ON c.rut = v.rut;

-- Ítems (la suma de cada presupuesto coincide con su monto_total)
INSERT INTO items_presupuesto (presupuesto_id, descripcion, monto)
SELECT p.id, v.descripcion, v.monto
FROM (VALUES
          ('PRES-T001', 'Mantención preventiva de cámara de frío',    1200000),
          ('PRES-T001', 'Repuestos y gas refrigerante',               2000000),
          ('PRES-T002', 'Instalación de túnel de congelado',          5000000),
          ('PRES-T002', 'Equipos y tuberías',                         3500000),
          ('PRES-T003', 'Climatización de sala de proceso',           2600000),
          ('PRES-T003', 'Ductos y difusores',                         1500000),
          ('PRES-T004', 'Tablero eléctrico principal de planta',      7000000),
          ('PRES-T004', 'Cableado y canalización',                    5000000),
          ('PRES-T005', 'Reparación de emergencia de compresor',      2800000),
          ('PRES-T006', 'Venta de repuestos de refrigeración',        1500000),
          ('PRES-T007', 'Mantención de sistema de climatización',     2400000),
          ('PRES-T007', 'Cambio de filtros y sensores',               1500000),
          ('PRES-T008', 'Ampliación de cámara de refrigeración',      6300000),
          ('PRES-T008', 'Equipos de control y monitoreo',             3500000),
          ('PRES-T009', 'Renovación de línea de frío',                6000000)
     ) AS v(numero, descripcion, monto)
         JOIN presupuestos p ON p.numero_presupuesto = v.numero;

-- Obras: 2 finalizadas, 2 en proceso (la del tablero eléctrico sin costos), 1 suspendida
INSERT INTO obras (nombre, descripcion, estado, fecha_inicio, fecha_termino, motivo_suspension, presupuesto_id, tipo_servicio_id, tecnico_id)
SELECT v.nombre, v.descripcion, v.estado, v.inicio, v.termino, v.motivo, p.id, t.id, u.id
FROM (VALUES
          ('Mantención cámara fría',     'Mantención preventiva anual',                 'FINALIZADA', CURRENT_DATE - 60, CURRENT_DATE - 45,                                  NULL::text,            'PRES-T001', 'Mantención',         'tecnico1'),
          ('Proyecto túnel congelado',   'Instalación completa de túnel de congelado',  'FINALIZADA', CURRENT_DATE - 90, date_trunc('month', CURRENT_DATE)::date - 2,        NULL::text,            'PRES-T002', 'Proyecto',           'tecnico2'),
          ('Climatización sala proceso', 'Climatización de sala de proceso',            'EN_PROCESO', CURRENT_DATE - 20, NULL::date,                                         NULL::text,            'PRES-T003', 'Climatización',      'tecnico1'),
          ('Tablero eléctrico planta',   'Recambio de tablero eléctrico principal',     'EN_PROCESO', CURRENT_DATE - 10, NULL::date,                                         NULL::text,            'PRES-T004', 'Proyecto eléctrico', 'tecnico2'),
          ('Emergencia compresor',       'Falla de compresor en cámara 2',              'SUSPENDIDA', CURRENT_DATE - 8,  NULL::date,                                         'Falta de materiales', 'PRES-T005', 'Emergencia',         'tecnico1')
     ) AS v(nombre, descripcion, estado, inicio, termino, motivo, numero, tipo, tecnico)
         JOIN presupuestos p   ON p.numero_presupuesto = v.numero
         JOIN tipos_servicio t ON t.nombre = v.tipo
         JOIN usuarios u       ON u.username = v.tecnico;

-- Costos reales (las obras en proceso con tablero eléctrico y la suspendida quedan sin costos)
INSERT INTO costos_reales (obra_id, materiales, horas_tecnicas, valor_hora)
SELECT o.id, v.materiales, v.horas, v.valor_hora
FROM (VALUES
          ('Mantención cámara fría',      900000, 40, 15000),
          ('Proyecto túnel congelado',   3000000, 80, 18000),
          ('Climatización sala proceso', 1200000, 20, 15000)
     ) AS v(obra, materiales, horas, valor_hora)
         JOIN obras o ON o.nombre = v.obra;

-- Facturas: F-0001 pendiente y vencida; F-0002 emitida este mes y pagada
INSERT INTO facturas (numero_factura, obra_id, fecha_emision, fecha_vencimiento, fecha_pago, monto, estado_pago)
SELECT v.numero, o.id, v.emision, v.vencimiento, v.pago, v.monto, v.estado
FROM (VALUES
          ('F-0001', 'Mantención cámara fría',    CURRENT_DATE - 45,                            CURRENT_DATE - 15,                                NULL::date,   3200000, 'PENDIENTE'),
          ('F-0002', 'Proyecto túnel congelado',  date_trunc('month', CURRENT_DATE)::date,      date_trunc('month', CURRENT_DATE)::date + 30,     CURRENT_DATE, 8500000, 'PAGADO')
     ) AS v(numero, obra, emision, vencimiento, pago, monto, estado)
         JOIN obras o ON o.nombre = v.obra;
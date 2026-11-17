CREATE SEQUENCE factura_numero_seq;
SELECT setval('factura_numero_seq',
              COALESCE((SELECT MAX(CAST(SUBSTRING(numero_factura FROM '[0-9]+$') AS BIGINT)) FROM facturas), 0) + 1,
              false);
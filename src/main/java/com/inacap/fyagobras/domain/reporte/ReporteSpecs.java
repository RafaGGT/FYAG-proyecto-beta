package com.inacap.fyagobras.domain.reporte;

import com.inacap.fyagobras.domain.factura.EstadoPago;
import com.inacap.fyagobras.domain.factura.Factura;
import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.Obra;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;

/** Filtros opcionales: solo se aplican los que el usuario eligió (HU-19). */
public final class ReporteSpecs {

    private ReporteSpecs() {
    }

    public static Specification<Obra> obras(LocalDate desde, LocalDate hasta,
                                            Long clienteId, Long tecnicoId, EstadoObra estado) {
        return (root, query, cb) -> {
            var predicados = new ArrayList<Predicate>();
            if (desde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("fechaInicio"), desde));
            }
            if (hasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.<LocalDate>get("fechaInicio"), hasta));
            }
            if (clienteId != null) {
                predicados.add(cb.equal(root.get("presupuesto").get("cliente").get("id"), clienteId));
            }
            if (tecnicoId != null) {
                predicados.add(cb.equal(root.get("tecnico").get("id"), tecnicoId));
            }
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    public static Specification<Factura> facturas(LocalDate desde, LocalDate hasta,
                                                  Long clienteId, EstadoPago estadoPago) {
        return (root, query, cb) -> {
            var predicados = new ArrayList<Predicate>();
            if (desde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("fechaEmision"), desde));
            }
            if (hasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.<LocalDate>get("fechaEmision"), hasta));
            }
            if (clienteId != null) {
                predicados.add(cb.equal(root.get("obra").get("presupuesto").get("cliente").get("id"), clienteId));
            }
            if (estadoPago != null) {
                predicados.add(cb.equal(root.get("estadoPago"), estadoPago));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
package com.inacap.fyagobras.obra;

import com.inacap.fyagobras.domain.obra.EstadoObra;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CP: Máquina de estados de la obra, revisando las 16 combinaciones posibles.
 * Trazabilidad: HU-09, RF-03, OWASP A04. Tipo: funcional (unitaria). Herramienta: JUnit 5 (automatizada).
 *
 * El diagrama de estados validado con el Product Owner solo permite:
 * EN_PROCESO a FINALIZADA, SUSPENDIDA o CANCELADA, y SUSPENDIDA a EN_PROCESO o CANCELADA.
 * FINALIZADA y CANCELADA son estados terminales. Cualquier otra combinación debe rechazarse.
 */
@DisplayName("Máquina de estados de la obra: todas las combinaciones (HU-09 / RF-03)")
// Prueba directamente la regla del enum, sin base de datos ni mocks.
class EstadoObraTest {

    @ParameterizedTest(name = "{0} a {1}: permitida")
    @CsvSource({
            "EN_PROCESO, FINALIZADA",
            "EN_PROCESO, SUSPENDIDA",
            "EN_PROCESO, CANCELADA",
            "SUSPENDIDA, EN_PROCESO",
            "SUSPENDIDA, CANCELADA"
    })
    @DisplayName("Las 5 transiciones del diagrama de estados se permiten")
    void transicionesDelDiagramaSePermiten(EstadoObra origen, EstadoObra destino) {
        // Verifica que cada transición definida en el diagrama sea aceptada.
        assertThat(origen.puedePasarA(destino)).isTrue();
    }

    @ParameterizedTest(name = "{0} a {1}: rechazada")
    @CsvSource({
            "EN_PROCESO, EN_PROCESO",
            "SUSPENDIDA, SUSPENDIDA",
            "SUSPENDIDA, FINALIZADA",
            "FINALIZADA, EN_PROCESO",
            "FINALIZADA, SUSPENDIDA",
            "FINALIZADA, CANCELADA",
            "FINALIZADA, FINALIZADA",
            "CANCELADA, EN_PROCESO",
            "CANCELADA, SUSPENDIDA",
            "CANCELADA, FINALIZADA",
            "CANCELADA, CANCELADA"
    })
    @DisplayName("Las otras 11 combinaciones se rechazan (FINALIZADA y CANCELADA son terminales)")
    void transicionesFueraDelDiagramaSeRechazan(EstadoObra origen, EstadoObra destino) {
        // Verifica que cualquier combinación no contemplada sea rechazada.
        assertThat(origen.puedePasarA(destino)).isFalse();
    }
}
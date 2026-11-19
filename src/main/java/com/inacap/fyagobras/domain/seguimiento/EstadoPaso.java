package com.inacap.fyagobras.domain.seguimiento;

public enum EstadoPaso {
    COMPLETADO, // ya ocurrió
    ACTUAL,     // el proceso está esperando este paso
    PAUSADO,    // la obra está suspendida
    PENDIENTE,  // todavía no toca
    ERROR       // el proceso terminó sin completarse (presupuesto rechazado u obra cancelada)
}

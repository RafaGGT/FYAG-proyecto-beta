package com.inacap.fyagobras.domain.seguimiento;

// Indica si un paso del seguimiento esta pendiente o completado.
public enum EstadoPaso {
    COMPLETADO, // ya ocurrió
    ACTUAL,     // el proceso está esperando este paso
    PAUSADO,    // la obra está suspendida
    PENDIENTE,  // todavía no toca
    ERROR       // el proceso terminó sin completarse (presupuesto rechazado u obra cancelada)
}

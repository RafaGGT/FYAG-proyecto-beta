package com.inacap.fyagobras.domain.obra;

public enum EstadoObra {
    // Define los posibles estados de una obra
    EN_PROCESO,
    FINALIZADA,
    SUSPENDIDA,
    CANCELADA;
    // Método que determina si es posible cambiar de un estado actual a un estado destino
    public boolean puedePasarA(EstadoObra destino) {
        return switch (this) {
            // Define las transiciones válidas entre estados
            // Desde EN_PROCESO se puede pasar a FINALIZADA, SUSPENDIDA o CANCELADA
            case EN_PROCESO -> destino == FINALIZADA || destino == SUSPENDIDA || destino == CANCELADA;
            // Desde SUSPENDIDA se puede pasar a EN_PROCESO o CANCELADA
            case SUSPENDIDA -> destino == EN_PROCESO || destino == CANCELADA;
            // Desde FINALIZADA o CANCELADA no se puede pasar a ningún otro estado
            case FINALIZADA, CANCELADA -> false;
        };
    }
}
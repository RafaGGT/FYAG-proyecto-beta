package com.inacap.fyagobras.domain.obra;

public enum EstadoObra {
    EN_PROCESO,
    FINALIZADA,
    SUSPENDIDA,
    CANCELADA;

    public boolean puedePasarA(EstadoObra destino) {
        return switch (this) {
            case EN_PROCESO -> destino == FINALIZADA || destino == SUSPENDIDA || destino == CANCELADA;
            case SUSPENDIDA -> destino == EN_PROCESO || destino == CANCELADA;
            case FINALIZADA, CANCELADA -> false;
        };
    }
}
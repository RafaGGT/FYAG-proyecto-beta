package com.inacap.fyagobras.domain.incidencia;

public enum TipoIncidencia {
    COMENTARIO("Comentario", "info"),
    AUSENCIA("No asistió / ausencia", "bad"),
    RETRASO("Retraso", "warn"),
    FALTA_MATERIAL("Falta de materiales", "warn"),
    PROBLEMA_ACCESO("Problema de acceso", "warn");

    private final String etiqueta;
    private final String tono;

    TipoIncidencia(String etiqueta, String tono) {
        this.etiqueta = etiqueta;
        this.tono = tono;
    }

    public String etiqueta() { return etiqueta; }
    public String tono() { return tono; }

    /** Todo lo que no es un simple comentario también le avisa a Gerencia. */
    public boolean esAlerta() { return this != COMENTARIO; }
}
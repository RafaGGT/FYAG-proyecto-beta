package com.inacap.fyagobras.domain.servicio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tipos_servicio")
@Getter
@NoArgsConstructor
public class TipoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    public TipoServicio(String nombre) {
        this.nombre = nombre;
        this.activo = true;
    }

    public void renombrar(String nombre) {
        this.nombre = nombre;
    }

    // Los tipos no se eliminan: se desactivan (se conservan en obras históricas)
    public void cambiarEstado(boolean activo) {
        this.activo = activo;
    }
}
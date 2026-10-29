package com.inacap.fyagobras.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import jakarta.persistence.GenerationType;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;
    @Column(nullable = false)
    protected String nombre;
    @Column(nullable = false)
    protected String apellido;
    @Column(nullable = false)
    protected String rut;
    @Column(nullable = false)
    protected String email;
    @Column
    protected String telefono;
}
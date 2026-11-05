package com.inacap.fyagobras.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import jakarta.persistence.GenerationType;

// Define la clase como una superclase mapeada en JPA (sus atributos se heredan en las entidades hijas, pero no genera una tabla propia)
@MappedSuperclass
@Getter // Genera automáticamente los métodos getters para todos los atributos mediante Lombok
@Setter // Genera automáticamente los métodos setters para todos los atributos mediante Lombok
@NoArgsConstructor // Genera un constructor vacío por defecto requerido por JPA
@ToString // Genera automáticamente el método toString para la clase
public class Persona {
    
    // Define el atributo como la clave primaria (ID) de la entidad
    @Id
    // Configura el autoincremento de la clave primaria gestionado por la base de datos
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;
    
    // Define la columna para el nombre, asegurando que no pueda ser nula en la base de datos
    @Column(nullable = false)
    protected String nombre;
    
    // Define la columna para el apellido, asegurando que no pueda ser nula
    @Column(nullable = false)
    protected String apellido;
    
    // Define la columna para el RUT, asegurando que no pueda ser nula
    @Column(nullable = false)
    protected String rut;
    
    // Define la columna para el correo electrónico, asegurando que no pueda ser nula
    @Column(nullable = false)
    protected String email;
    
    // Define la columna opcional para el número de teléfono (permite valores nulos por defecto)
    @Column
    protected String telefono;
}
package com.inacap.fyagobras.domain.cliente;

import com.inacap.fyagobras.domain.cliente.dto.ModificarClienteDTO;
import com.inacap.fyagobras.domain.cliente.dto.RegistrarClienteDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Entidad de la tabla clientes. El RUT identifica al cliente y no se puede modificar.
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String rut;

    @Column(name = "razon_social", nullable = false)
    private String razonSocial;

    @Column
    private String direccion;

    @Column
    private String email;

    @Column
    private String telefono;

    public Cliente(RegistrarClienteDTO datos) {
        this.rut = datos.rut().toUpperCase();   // la K del dígito verificador siempre en mayúscula
        this.razonSocial = datos.razonSocial();
        this.direccion = datos.direccion();
        this.email = datos.email();
        this.telefono = datos.telefono();
    }

    // Solo cambia los campos que vienen con valor (null = no tocar)
    public void actualizar(ModificarClienteDTO datos) {
        if (datos.razonSocial() != null) this.razonSocial = datos.razonSocial();
        if (datos.direccion() != null) this.direccion = datos.direccion();
        if (datos.email() != null) this.email = datos.email();
        if (datos.telefono() != null) this.telefono = datos.telefono();
    }
}
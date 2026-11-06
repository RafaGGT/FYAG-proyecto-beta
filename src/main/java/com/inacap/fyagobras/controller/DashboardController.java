package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.dashboard.DashboardService;
import com.inacap.fyagobras.domain.dashboard.dto.DashboardDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /** Panel gerencial. Solo Gerencia y Finanzas. */
    @PreAuthorize("hasAnyRole('GERENCIA','FINANZAS')")
    @GetMapping
    public ResponseEntity<DashboardDTO> obtener() {
        return ResponseEntity.ok(dashboardService.obtener());
    }
}
package com.colectivo.admin.controller;

import com.colectivo.admin.dto.BugReportDto;
import com.colectivo.admin.dto.BugReportUpdateDto;
import com.colectivo.admin.service.BugReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/bug-reports")
@RequiredArgsConstructor
public class BugReportController {

    private final BugReportService bugReportService;

    /** Lista los reportes; opcionalmente filtrados por status. */
    @GetMapping
    public ResponseEntity<List<BugReportDto>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(bugReportService.list(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BugReportDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(bugReportService.getById(id));
    }

    /** Actualiza el estado de triaje. */
    @PatchMapping("/{id}")
    public ResponseEntity<BugReportDto> update(@PathVariable String id,
                                               @Valid @RequestBody BugReportUpdateDto dto,
                                               Authentication auth) {
        return ResponseEntity.ok(bugReportService.update(id, dto, auth));
    }
}

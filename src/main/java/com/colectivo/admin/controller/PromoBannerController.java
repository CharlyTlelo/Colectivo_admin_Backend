package com.colectivo.admin.controller;

import com.colectivo.admin.dto.PromoBannerConfigDto;
import com.colectivo.admin.service.PromoBannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/promo-banners")
@RequiredArgsConstructor
public class PromoBannerController {

    private final PromoBannerService promoBannerService;

    @GetMapping("/{placement}")
    public ResponseEntity<PromoBannerConfigDto> get(@PathVariable String placement) {
        return ResponseEntity.ok(promoBannerService.get(placement));
    }

    @PutMapping("/{placement}")
    public ResponseEntity<PromoBannerConfigDto> save(@PathVariable String placement,
                                                     @Valid @RequestBody PromoBannerConfigDto dto) {
        return ResponseEntity.ok(promoBannerService.save(placement, dto));
    }
}

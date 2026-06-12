package com.colectivo.admin.service;

import com.colectivo.admin.dto.PromoBannerConfigDto;
import com.colectivo.admin.dto.PromoBannerSlideDto;
import com.colectivo.admin.model.PromoBannerConfig;
import com.colectivo.admin.model.PromoBannerSlide;
import com.colectivo.admin.repository.PromoBannerConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PromoBannerService {

    private static final Set<String> ALLOWED_PLACEMENTS = Set.of("driver_home");
    private static final Set<String> ALLOWED_ACTION_TYPES = Set.of("none", "link", "internal");

    private final PromoBannerConfigRepository repository;
    private final Clock clock;

    public PromoBannerConfigDto get(String placement) {
        validatePlacement(placement);
        return repository.findById(placement)
                .map(this::toDto)
                .orElseGet(() -> emptyDto(placement));
    }

    public PromoBannerConfigDto save(String placement, PromoBannerConfigDto dto) {
        validatePlacement(placement);
        if (!placement.equals(dto.getPlacement())) {
            throw new IllegalArgumentException("El placement de la URL no coincide con el cuerpo");
        }

        List<PromoBannerSlide> slides = IntStream.range(0, dto.getSlides().size())
                .mapToObj(i -> toEntity(dto.getSlides().get(i), i))
                .sorted(Comparator.comparingInt(PromoBannerSlide::getSortOrder))
                .collect(Collectors.toList());

        int rotationSeconds = Math.max(2, Math.min(120, dto.getRotationSeconds()));

        PromoBannerConfig config = PromoBannerConfig.builder()
                .placement(placement)
                .rotationSeconds(rotationSeconds)
                .enabled(dto.isEnabled())
                .slides(slides)
                .updatedAt(Instant.now(clock))
                .build();

        return toDto(repository.save(config));
    }

    private PromoBannerSlide toEntity(PromoBannerSlideDto dto, int index) {
        if (dto.getImageUrl() == null || dto.getImageUrl().isBlank()) {
            throw new IllegalArgumentException("Cada slide debe incluir una imagen");
        }

        String actionType = normalizeActionType(dto.getActionType());
        if ("link".equals(actionType)) {
            String url = trim(dto.getLinkUrl());
            if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
                throw new IllegalArgumentException("El enlace externo debe comenzar con http:// o https://");
            }
        }
        if ("internal".equals(actionType)) {
            String route = trim(dto.getInternalRoute());
            if (route == null || !route.startsWith("/")) {
                throw new IllegalArgumentException("La ruta interna debe comenzar con /");
            }
        }

        String id = trim(dto.getId());
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        return PromoBannerSlide.builder()
                .id(id)
                .imageUrl(dto.getImageUrl().trim())
                .actionType(actionType)
                .linkUrl(trim(dto.getLinkUrl()))
                .internalRoute(trim(dto.getInternalRoute()))
                .sortOrder(dto.getSortOrder() >= 0 ? dto.getSortOrder() : index)
                .enabled(dto.isEnabled())
                .build();
    }

    private PromoBannerConfigDto emptyDto(String placement) {
        return PromoBannerConfigDto.builder()
                .placement(placement)
                .rotationSeconds(5)
                .enabled(false)
                .slides(List.of())
                .updatedAt(null)
                .build();
    }

    private PromoBannerConfigDto toDto(PromoBannerConfig config) {
        List<PromoBannerSlideDto> slides = config.getSlides() == null
                ? List.of()
                : config.getSlides().stream()
                        .sorted(Comparator.comparingInt(PromoBannerSlide::getSortOrder))
                        .map(this::toSlideDto)
                        .collect(Collectors.toList());

        return PromoBannerConfigDto.builder()
                .placement(config.getPlacement())
                .rotationSeconds(config.getRotationSeconds() > 0 ? config.getRotationSeconds() : 5)
                .enabled(config.isEnabled())
                .slides(slides)
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private PromoBannerSlideDto toSlideDto(PromoBannerSlide slide) {
        return PromoBannerSlideDto.builder()
                .id(slide.getId())
                .imageUrl(slide.getImageUrl())
                .actionType(slide.getActionType())
                .linkUrl(slide.getLinkUrl())
                .internalRoute(slide.getInternalRoute())
                .sortOrder(slide.getSortOrder())
                .enabled(slide.isEnabled())
                .build();
    }

    private void validatePlacement(String placement) {
        if (placement == null || !ALLOWED_PLACEMENTS.contains(placement)) {
            throw new IllegalArgumentException("Ubicación no válida: " + placement);
        }
    }

    private String normalizeActionType(String actionType) {
        String normalized = actionType == null ? "none" : actionType.trim().toLowerCase();
        if (!ALLOWED_ACTION_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Tipo de acción no válido: " + actionType);
        }
        return normalized;
    }

    private String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

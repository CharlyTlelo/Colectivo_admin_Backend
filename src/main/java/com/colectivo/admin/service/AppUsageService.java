package com.colectivo.admin.service;

import com.colectivo.admin.dto.AppUsageSnapshotDto;
import com.colectivo.admin.dto.AppUsageUserDto;
import com.colectivo.admin.model.User;
import com.colectivo.admin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUsageService {

    private final UserRepository userRepository;

    public AppUsageSnapshotDto getSnapshot() {
        List<User> all = userRepository.findAll();
        long pwa = all.stream().filter(u -> "pwa".equals(u.getClientSurface())).count();
        long browser = all.stream().filter(u -> "browser".equals(u.getClientSurface())).count();
        long unknown = all.size() - pwa - browser;

        List<AppUsageUserDto> users = all.stream()
                .sorted(Comparator
                        .comparing(User::getClientSurfaceUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(u -> u.getName() != null ? u.getName() : "", String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();

        return new AppUsageSnapshotDto(pwa, browser, unknown, users);
    }

    private AppUsageUserDto toDto(User user) {
        return AppUsageUserDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .activeProfile(user.getActiveProfile())
                .clientSurface(user.getClientSurface())
                .clientSurfaceUpdatedAt(formatInstant(user.getClientSurfaceUpdatedAt()))
                .pwaInstalledAt(formatInstant(user.getPwaInstalledAt()))
                .build();
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}

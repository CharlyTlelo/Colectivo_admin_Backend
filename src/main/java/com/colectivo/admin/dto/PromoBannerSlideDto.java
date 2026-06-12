package com.colectivo.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoBannerSlideDto {

    private String id;
    private String imageUrl;
    private String actionType;
    private String linkUrl;
    private String internalRoute;
    private int sortOrder;
    private boolean enabled;
}

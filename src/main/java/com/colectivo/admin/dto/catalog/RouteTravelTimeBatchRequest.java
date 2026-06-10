package com.colectivo.admin.dto.catalog;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class RouteTravelTimeBatchRequest {
    @NotEmpty
    private List<String> originLocalityIds;

    @NotEmpty
    private List<String> destinationLocalityIds;
}

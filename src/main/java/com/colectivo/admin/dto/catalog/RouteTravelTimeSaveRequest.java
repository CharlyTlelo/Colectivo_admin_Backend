package com.colectivo.admin.dto.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Guarda en BD tiempos ya calculados (botón "Guardar registro"). Lleva los
 * minutos/distancias del cálculo previo para no volver a llamar a Google.
 */
@Data
public class RouteTravelTimeSaveRequest {

    @NotEmpty
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotBlank
        private String originLocalityId;
        @NotBlank
        private String destinationLocalityId;
        private int estimatedTravelMinutes;
        private double distanceKm;
        private int returnTravelMinutes;
        private double returnDistanceKm;
    }
}

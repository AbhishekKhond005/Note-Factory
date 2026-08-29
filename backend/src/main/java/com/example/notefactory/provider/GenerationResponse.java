package com.example.notefactory.provider;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerationResponse {
    private String text;
    private String providerName;
    private Long latencyMs;
    private boolean isQuotaError;
}

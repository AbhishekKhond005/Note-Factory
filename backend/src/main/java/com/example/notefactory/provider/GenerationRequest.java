package com.example.notefactory.provider;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerationRequest {
    private String prompt;
    private String modelHint;
    private Integer maxTokens;
    private Double temperature;
}

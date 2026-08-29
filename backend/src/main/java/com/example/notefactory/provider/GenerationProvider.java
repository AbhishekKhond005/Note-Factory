package com.example.notefactory.provider;

public interface GenerationProvider {
    GenerationResponse generate(GenerationRequest request);
    String getProviderName();
}

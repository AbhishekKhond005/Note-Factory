package com.example.notefactory.agent;

import com.example.notefactory.provider.GenerationProvider;

public abstract class GenerationAgent {
    protected final GenerationProvider provider;

    protected GenerationAgent(GenerationProvider provider) {
        this.provider = provider;
    }
}

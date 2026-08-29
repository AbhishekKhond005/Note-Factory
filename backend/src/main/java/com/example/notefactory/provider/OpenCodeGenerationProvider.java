package com.example.notefactory.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class OpenCodeGenerationProvider implements GenerationProvider {

    private static final int TIMEOUT_MINUTES = 5;

    @Override
    public String getProviderName() {
        return "opencode";
    }

    @Override
    public GenerationResponse generate(GenerationRequest request) {
        long start = System.currentTimeMillis();
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("opencode-work-");
            
            // Write prompt to a temp file
            Path promptFile = tempDir.resolve("prompt.txt");
            Files.writeString(promptFile, request.getPrompt());
            
            // First try native invocation
            ExecutionResult result = executeCommand(tempDir.toFile(), "opencode", "-f", promptFile.toString());
            
            // Check for quota error
            if (isQuotaError(result.getStderr())) {
                log.warn("Quota limit detected. Attempting docker fallback...");
                result = executeCommand(tempDir.toFile(), "docker", "run", "--rm", "-v", tempDir.toAbsolutePath() + ":/work", "-w", "/work", "opencode", "-f", "prompt.txt");
                if (isQuotaError(result.getStderr())) {
                    return GenerationResponse.builder()
                            .text("")
                            .providerName(getProviderName())
                            .latencyMs(System.currentTimeMillis() - start)
                            .isQuotaError(true)
                            .build();
                }
            }
            
            if (result.getExitCode() != 0) {
                throw new RuntimeException("opencode failed with exit code " + result.getExitCode() + "\n" + result.getStderr());
            }

            // Normalization logic: check if it wrote a markdown file in the temp dir instead of stdout
            String output = normalizeOutput(tempDir, result.getStdout());
            
            return GenerationResponse.builder()
                    .text(output)
                    .providerName(getProviderName())
                    .latencyMs(System.currentTimeMillis() - start)
                    .isQuotaError(false)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error during generation", e);
            throw new RuntimeException("Provider error: " + e.getMessage(), e);
        } finally {
            if (tempDir != null) {
                org.springframework.util.FileSystemUtils.deleteRecursively(tempDir.toFile());
            }
        }
    }

    private boolean isQuotaError(String stderr) {
        if (stderr == null) return false;
        String lower = stderr.toLowerCase();
        return lower.contains("429") || lower.contains("quota") || 
               lower.contains("rate limit") || lower.contains("payment required") || lower.contains("exhausted");
    }

    private String normalizeOutput(Path tempDir, String stdout) throws IOException {
        // Strip ANSI codes if any
        String cleanStdout = stdout.replaceAll("\\x1B\\[[0-9;]*[mK]", "");
        
        // Scan directory for a markdown file just in case it saved to disk
        File[] files = tempDir.toFile().listFiles((dir, name) -> name.endsWith(".md"));
        if (files != null && files.length > 0) {
            log.info("Found file written by opencode: {}", files[0].getName());
            return Files.readString(files[0].toPath());
        }
        
        // Remove markdown fences from raw output if they wrap the entire output
        cleanStdout = cleanStdout.trim();
        if (cleanStdout.startsWith("```markdown")) {
            cleanStdout = cleanStdout.substring("```markdown".length());
        } else if (cleanStdout.startsWith("```")) {
            cleanStdout = cleanStdout.substring(3);
        }
        if (cleanStdout.endsWith("```")) {
            cleanStdout = cleanStdout.substring(0, cleanStdout.length() - 3);
        }
        
        return cleanStdout.trim();
    }

    private ExecutionResult executeCommand(File workDir, String... cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workDir);
        Process process = pb.start();
        
        boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Command timed out: " + String.join(" ", cmd));
        }
        
        String stdout = new String(process.getInputStream().readAllBytes());
        String stderr = new String(process.getErrorStream().readAllBytes());
        return new ExecutionResult(process.exitValue(), stdout, stderr);
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ExecutionResult {
        private int exitCode;
        private String stdout;
        private String stderr;
    }
}

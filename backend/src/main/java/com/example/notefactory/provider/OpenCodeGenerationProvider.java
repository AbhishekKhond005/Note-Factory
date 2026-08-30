package com.example.notefactory.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Invokes the OpenCode CLI as a subprocess. To bypass per-session rate limits
 * that the native CLI hits, each generation can be run inside its own fresh
 * Docker container (one container per chapter invocation) with an isolated work
 * directory. Set {@code notefactory.provider.force-docker=true} to always use
 * the container path; otherwise docker is used as an automatic fallback when a
 * quota/rate-limit error is detected.
 */
@Slf4j
@Component
public class OpenCodeGenerationProvider implements GenerationProvider {

    private static final int TIMEOUT_MINUTES = 15;

    @Value("${notefactory.provider.model:opencode/big-pickle}")
    private String model;

    @Value("${notefactory.provider.force-docker:false}")
    private boolean forceDocker;

    @Value("${notefactory.provider.docker-image:opencode-runner:latest}")
    private String dockerImage;

    @Override
    public String getProviderName() {
        return "opencode-docker";
    }

    @Override
    public GenerationResponse generate(GenerationRequest request) {
        long start = System.currentTimeMillis();
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("opencode-chapter-");
            String prompt = request.getPrompt();

            ExecutionResult result;
            if (forceDocker) {
                result = runInDocker(tempDir, prompt);
            } else {
                result = runNative(tempDir, prompt);
                if (isQuotaError(result.getStderr()) || isQuotaError(result.getStdout())) {
                    log.warn("Quota/rate-limit detected. Retrying inside a fresh Docker container...");
                    result = runInDocker(tempDir, prompt);
                }
            }

            if (isQuotaError(result.getStderr()) || isQuotaError(result.getStdout())) {
                return GenerationResponse.builder()
                        .text("")
                        .providerName(getProviderName())
                        .latencyMs(System.currentTimeMillis() - start)
                        .isQuotaError(true)
                        .build();
            }

            if (result.getExitCode() != 0) {
                throw new RuntimeException("opencode failed with exit code " + result.getExitCode() + "\n" + result.getStderr());
            }

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

    private ExecutionResult runNative(Path tempDir, String prompt) throws IOException, InterruptedException {
        return executeCommand(tempDir.toFile(),
                "opencode", "run", "-m", model, "--format", "default", prompt);
    }

    private ExecutionResult runInDocker(Path tempDir, String prompt) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>();
        args.addAll(List.of("docker", "run", "--rm",
                "--entrypoint", "opencode",
                "-v", tempDir.toAbsolutePath() + ":/work",
                "-w", "/work"));

        // Pass proxy env through to the container if set.
        for (String p : new String[]{"HTTP_PROXY", "HTTPS_PROXY", "http_proxy", "https_proxy"}) {
            String val = System.getenv(p);
            if (val != null && !val.isEmpty()) {
                args.add("-e");
                args.add(p + "=" + val);
            }
        }

        args.add(dockerImage);
        args.add("run");
        args.add("-m");
        args.add(model);
        args.add("--format");
        args.add("default");
        args.add(prompt);

        return executeCommand(tempDir.toFile(), args.toArray(new String[0]));
    }

    private boolean isQuotaError(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return lower.contains("429") || lower.contains("quota") ||
                lower.contains("rate limit") || lower.contains("payment required") ||
                lower.contains("exhausted") || lower.contains("limit exceeded");
    }

    private String normalizeOutput(Path tempDir, String stdout) throws IOException {
        String cleanStdout = stdout.replaceAll("\u001B\\[[\\d;]*[a-zA-Z]", "");

        // If opencode wrote a Markdown file into its work dir, prefer that content.
        File[] files = tempDir.toFile().listFiles((dir, name) -> name.endsWith(".md"));
        if (files != null && files.length > 0) {
            // Deterministically pick the largest .md file (most likely the real notes).
            File best = null;
            for (File f : files) {
                if (best == null || f.length() > best.length()) best = f;
            }
            if (best != null && best.length() > 0) {
                log.info("Found file written by opencode: {}", best.getName());
                return Files.readString(best.toPath()).trim();
            }
        }

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

        File stdoutFile = new File(workDir, "opencode.stdout");
        File stderrFile = new File(workDir, "opencode.stderr");
        pb.redirectOutput(stdoutFile);
        pb.redirectError(stderrFile);
        pb.redirectInput(new File("/dev/null"));

        Process process = pb.start();
        boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Command timed out: " + String.join(" ", cmd));
        }

        String stdout = stdoutFile.exists() ? Files.readString(stdoutFile.toPath()) : "";
        String stderr = stderrFile.exists() ? Files.readString(stderrFile.toPath()) : "";
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

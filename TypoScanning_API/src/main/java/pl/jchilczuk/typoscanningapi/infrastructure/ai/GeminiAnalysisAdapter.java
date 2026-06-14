package pl.jchilczuk.typoscanningapi.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.jchilczuk.typoscanningapi.application.ports.models.AiAssessmentResult;
import pl.jchilczuk.typoscanningapi.application.ports.AiAnalyzer;
import pl.jchilczuk.typoscanningapi.domain.model.DomainVariantResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAnalysisAdapter implements AiAnalyzer {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final Semaphore rateLimitSemaphore = new Semaphore(1);

    public Map<String, AiAssessmentResult> analyzeBatch(String sourceDomain, List<DomainVariantResult> batch) {
        if (batch.isEmpty()) {
            return new HashMap<>();
        }

        try {
            rateLimitSemaphore.acquire();
            Thread.sleep(4000);

            String prompt = buildBatchPrompt(sourceDomain, batch);
            String requestBody = buildRequestBody(prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = sendWithRetry(request);

            if (response.statusCode() == 200) {
                return parseGeminiBatchResponse(response.body());
            } else {
                log.error("Gemini API error: HTTP {}. Body: {}", response.statusCode(), response.body());
                return fallbackMap(batch, "Błąd API Gemini: " + response.statusCode());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallbackMap(batch, "Interrupted waiting for API lock.");
        } catch (Exception e) {
            log.error("Failed to connect to Gemini API for batch of {} domains", batch.size(), e);
            return fallbackMap(batch, "Exception while AI analysis.");
        } finally {
            rateLimitSemaphore.release();
        }
    }

    private String buildBatchPrompt(String sourceDomain, List<DomainVariantResult> batch) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert Cybersecurity Threat Intelligence Analyst. ");
        prompt.append("Your task is to evaluate a batch of suspicious domain variants targeting the legitimate source domain: '")
                .append(sourceDomain).append("'.\n\n");

        prompt.append("CRITICAL RULES FOR CLASSIFICATION:\n");
        prompt.append("- Base your decision STRICTLY on the provided technical signals (HTTP reachability, MX records, TLS presence, redirects, login forms).\n");
        prompt.append("- Do NOT hallucinate external knowledge.\n");
        prompt.append("- Do NOT assume maliciousness solely because the domain name looks similar. Many variants are bought defensively by the original brand.\n\n");

        prompt.append("CLASSIFICATION DEFINITIONS:\n");
        prompt.append("- PHISHING_LIKE: Use if there is a 'Login Form Detected' on a typo-domain, OR if it has a high heuristic score with active web content not belonging to the brand, OR if the domain lacks a website but has an active 'MX Record' (indicating high risk of email spoofing / Business Email Compromise).\n");
        prompt.append("- PARKED_OR_FOR_SALE: Use if the domain resolves, but seems to lack active business logic (e.g., parked by registrar).\n");
        prompt.append("- LEGITIMATE: Use if the domain safely redirects back to the original Source Domain, indicating defensive registration by the brand.\n");
        prompt.append("- INACTIVE: Use if DNS resolves, but HTTP/HTTPS are unreachable and NO active MX records exist.\n");
        prompt.append("- UNCLEAR: Use only if evidence is highly contradictory.\n\n");

        prompt.append("Return strictly valid JSON containing an array of results. Use exactly this format:\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"domainName\": \"the-analyzed-domain.com\",\n");
        prompt.append("    \"classification\": \"PHISHING_LIKE | PARKED_OR_FOR_SALE | LEGITIMATE | INACTIVE | UNCLEAR\",\n");
        prompt.append("    \"suspicionLevel\": \"LOW | MEDIUM | HIGH\",\n");
        prompt.append("    \"confidence\": \"LOW | MEDIUM | HIGH\",\n");
        prompt.append("    \"recommendedAction\": \"NO_ACTION | MONITOR | MANUAL_REVIEW | REPORT\",\n");
        prompt.append("    \"explanation\": \"1-2 short sentences explaining the decision based ON THE PROVIDED SIGNALS ONLY\"\n");
        prompt.append("  }\n");
        prompt.append("]\n\n");

        prompt.append("--- EVIDENCE BATCH ---\n");

        for (DomainVariantResult result : batch) {
            prompt.append("Variant: ").append(result.getDomainName()).append("\n")
                    .append("- Type: ").append(result.getVariantType()).append("\n")
                    .append("- Heuristic Score: ").append(result.getHeuristicScore()).append("\n")
                    .append("- DNS Resolved: ").append(result.isDnsResolved())
                    .append(" (A: ").append(result.isHasARecord())
                    .append(", AAAA: ").append(result.isHasAaaaRecord()).append(")\n")
                    .append("- MX Record Active: ").append(result.isHasMxRecord()).append("\n")
                    .append("- TLS Certificate Present: ").append(result.isTlsCertificatePresent()).append("\n")
                    .append("- Reachable HTTP/HTTPS: ").append(result.isHttpReachable()).append("/").append(result.isHttpsReachable()).append("\n")
                    .append("- Redirect: ").append(result.isRedirectDetected()).append(" (Final URL: ").append(result.getFinalUrl() != null ? result.getFinalUrl() : "N/A").append(")\n")
                    .append("- Login Form Detected: ").append(result.isLoginFormDetected()).append("\n")
                    .append("- Page Title: ").append(result.getPageTitle() != null ? result.getPageTitle() : "None").append("\n")
                    .append("---\n");
        }

        return prompt.toString();
    }

    private String buildRequestBody(String prompt) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("temperature", 0.1);

        return root.toString();
    }

    private Map<String, AiAssessmentResult> parseGeminiBatchResponse(String responseBody) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        String aiContent = rootNode
                .path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text").asText();

        int startIndex = aiContent.indexOf('[');
        int endIndex = aiContent.lastIndexOf(']');

        Map<String, AiAssessmentResult> resultMap = new HashMap<>();

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            String jsonArrayString = aiContent.substring(startIndex, endIndex + 1);
            JsonNode arrayNode = objectMapper.readTree(jsonArrayString);

            for (JsonNode node : arrayNode) {
                String domainName = node.path("domainName").asText();
                AiAssessmentResult result = objectMapper.treeToValue(node, AiAssessmentResult.class);
                resultMap.put(domainName, result);
            }
            return resultMap;
        }

        log.warn("AI response did not contain valid JSON array. Raw output: {}", aiContent);
        throw new RuntimeException("Invalid JSON array from AI");
    }

    private Map<String, AiAssessmentResult> fallbackMap(List<DomainVariantResult> batch, String errorMsg) {
        Map<String, AiAssessmentResult> fallback = new HashMap<>();
        for (DomainVariantResult res : batch) {
            fallback.put(res.getDomainName(), new AiAssessmentResult(
                    "UNCLEAR", "UNCLEAR", "LOW", "MANUAL_REVIEW", errorMsg
            ));
        }
        return fallback;
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request) throws Exception {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 200) return response;
            if (status == 429 || status == 500 || status == 502 || status == 503 || status == 504) {
                log.warn("Gemini temporary error HTTP {} on attempt {}/{}. Body: {}", status, attempt, maxAttempts, response.body());
                if (attempt < maxAttempts) {
                    Thread.sleep(1000L * attempt * attempt);
                    continue;
                }
            }
            return response;
        }
        throw new IllegalStateException("Unexpected retry loop exit");
    }
}
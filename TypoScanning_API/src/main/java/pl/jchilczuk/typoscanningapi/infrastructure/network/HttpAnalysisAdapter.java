package pl.jchilczuk.typoscanningapi.infrastructure.network;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.jchilczuk.typoscanningapi.application.ports.HttpScanner;
import pl.jchilczuk.typoscanningapi.application.ports.models.HttpAnalysisResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for basic HTTP/HTTPS analysis of a domain.
 * It checks whether the domain is reachable over HTTP and HTTPS,
 * detects redirects, and extracts basic page information such as
 * title, HTML snippet, and potential login form indicators.
 */

@Service
public class HttpAnalysisAdapter implements HttpScanner {

    private final HttpClient httpClient;

    public HttpAnalysisAdapter() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Async("analysisTaskExecutor")
    public CompletableFuture<HttpAnalysisResult> analyzeAsync(String domainName) {
        HttpProbeResult httpResult = probeUrl("http://" + domainName);
        HttpProbeResult httpsResult = probeUrl("https://" + domainName);

        HttpProbeResult preferredResult = choosePreferredResult(httpResult, httpsResult);

        boolean redirectDetected = preferredResult != null && preferredResult.redirectDetected();
        String finalUrl = preferredResult != null ? preferredResult.finalUrl() : null;
        String pageTitle = preferredResult != null ? preferredResult.pageTitle() : null;
        String htmlSnippet = preferredResult != null ? preferredResult.htmlSnippet() : null;
        boolean loginFormDetected = preferredResult != null && preferredResult.loginFormDetected();

        return CompletableFuture.completedFuture(new HttpAnalysisResult(
                httpResult.reachable(),
                httpsResult.reachable(),
                redirectDetected,
                finalUrl,
                pageTitle,
                htmlSnippet,
                loginFormDetected
        ));
    }

    private HttpProbeResult probeUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            boolean reachable = statusCode >= 200 && statusCode < 400;

            if (!reachable) {
                return new HttpProbeResult(false, false, null, null, null, false);
            }

            String responseBody = response.body();
            String finalUrl = response.uri().toString();
            boolean redirectDetected = !finalUrl.equals(url);

            String pageTitle = extractPageTitle(responseBody);
            String htmlSnippet = extractHtmlSnippet(responseBody);
            boolean loginFormDetected = detectLoginForm(responseBody);

            return new HttpProbeResult(
                    true,
                    redirectDetected,
                    finalUrl,
                    pageTitle,
                    htmlSnippet,
                    loginFormDetected
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new HttpProbeResult(false, false, null, null, null, false);
        } catch (IllegalArgumentException | IOException e) {
            return new HttpProbeResult(false, false, null, null, null, false);
        }
    }

    private HttpProbeResult choosePreferredResult(HttpProbeResult httpResult, HttpProbeResult httpsResult) {
        if (httpsResult != null && httpsResult.reachable()) {
            return httpsResult;
        }
        if (httpResult != null && httpResult.reachable()) {
            return httpResult;
        }
        return null;
    }

    private String extractPageTitle(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }

        try {
            Document document = Jsoup.parse(html);
            String title = document.title();
            return !title.isBlank() ? title.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractHtmlSnippet(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }

        try {
            Document document = Jsoup.parse(html);
            document.body();
            String text = document.body().text();

            if (text.isBlank()) {
                return null;
            }

            String normalized = text.trim().replaceAll("\\s+", " ");
            int maxLength = 500;

            return normalized.length() <= maxLength
                    ? normalized
                    : normalized.substring(0, maxLength);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean detectLoginForm(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }

        try {
            Document document = Jsoup.parse(html);

            Elements passwordInputs = document.select("input[type=password]");
            if (!passwordInputs.isEmpty()) {
                return true;
            }

            Elements forms = document.select("form");
            if (forms.isEmpty()) {
                return false;
            }

            String lowerHtml = html.toLowerCase();

            return lowerHtml.contains("login")
                    || lowerHtml.contains("sign in")
                    || lowerHtml.contains("log in")
                    || lowerHtml.contains("password")
                    || lowerHtml.contains("konto")
                    || lowerHtml.contains("zaloguj");
        } catch (Exception e) {
            return false;
        }
    }

    private record HttpProbeResult(
            boolean reachable,
            boolean redirectDetected,
            String finalUrl,
            String pageTitle,
            String htmlSnippet,
            boolean loginFormDetected
    ) {
    }
}
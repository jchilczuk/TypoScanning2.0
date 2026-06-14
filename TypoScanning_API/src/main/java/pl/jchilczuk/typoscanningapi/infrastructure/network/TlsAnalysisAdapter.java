package pl.jchilczuk.typoscanningapi.infrastructure.network;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.jchilczuk.typoscanningapi.application.ports.TlsScanner;
import pl.jchilczuk.typoscanningapi.application.ports.models.TlsAnalysisResult;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for basic TLS analysis of a domain.
 * It attempts to establish an SSL/TLS connection on port 443
 * and checks whether the server presents a certificate.
 */

@Service
public class TlsAnalysisAdapter implements TlsScanner {

    @Async("analysisTaskExecutor")
    public CompletableFuture<TlsAnalysisResult> analyzeAsync(String domainName) {
        try {
            SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

            try (SSLSocket socket = (SSLSocket) socketFactory.createSocket(domainName, 443)) {
                socket.setSoTimeout(5000);
                socket.startHandshake();

                if (socket.getSession() != null && socket.getSession().getPeerCertificates().length > 0) {
                    return CompletableFuture.completedFuture(new TlsAnalysisResult(true));
                }
            }
            return CompletableFuture.completedFuture(new TlsAnalysisResult(false));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(new TlsAnalysisResult(false));
        }
    }
}
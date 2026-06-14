package pl.jchilczuk.typoscanningapi.infrastructure.network;

import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import pl.jchilczuk.typoscanningapi.application.ports.DnsScanner;
import pl.jchilczuk.typoscanningapi.application.ports.models.DnsAnalysisResult;

import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for basic DNS analysis of a domain.
 * It checks whether A, AAAA, and MX records are present and determines
 * whether the domain has active DNS configuration.
 */

@Service
public class DnsAnalysisAdapter implements DnsScanner {

    @PostConstruct
    public void init() {
        Lookup.setDefaultHostsFileParser(null);
    }

    @Async("analysisTaskExecutor")
    public CompletableFuture<DnsAnalysisResult> analyzeAsync(String domainName) {
        boolean hasARecord = hasRecord(domainName, Type.A);
        boolean hasAaaaRecord = hasRecord(domainName, Type.AAAA);
        boolean hasMxRecord = hasRecord(domainName, Type.MX);

        boolean dnsResolved = hasARecord || hasAaaaRecord || hasMxRecord;

        return CompletableFuture.completedFuture(new DnsAnalysisResult(
                dnsResolved,
                hasARecord,
                hasAaaaRecord,
                hasMxRecord
        ));
    }

    private boolean hasRecord(String domainName, int recordType) {
        try {
            Lookup lookup = new Lookup(domainName, recordType);
            Record[] records = lookup.run();

            return records != null && records.length > 0;
        } catch (TextParseException e) {
            return false;
        }
    }
}
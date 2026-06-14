package pl.jchilczuk.typoscanningapi.application.ports.models;

public record DnsAnalysisResult(
        boolean dnsResolved,
        boolean hasARecord,
        boolean hasAaaaRecord,
        boolean hasMxRecord
) {
}
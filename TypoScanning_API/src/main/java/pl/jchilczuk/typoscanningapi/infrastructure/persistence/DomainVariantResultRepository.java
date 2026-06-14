package pl.jchilczuk.typoscanningapi.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.jchilczuk.typoscanningapi.domain.model.DomainVariantResult;

import java.util.List;

public interface DomainVariantResultRepository extends JpaRepository<DomainVariantResult, Long> {
    List<DomainVariantResult> findByAnalysisJobId(Long analysisJobId);
}

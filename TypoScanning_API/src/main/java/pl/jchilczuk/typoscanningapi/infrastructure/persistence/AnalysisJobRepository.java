package pl.jchilczuk.typoscanningapi.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.jchilczuk.typoscanningapi.domain.model.AnalysisJob;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Long> {
}
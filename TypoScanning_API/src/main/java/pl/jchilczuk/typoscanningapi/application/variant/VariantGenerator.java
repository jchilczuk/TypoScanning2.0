package pl.jchilczuk.typoscanningapi.application.variant;

import java.util.List;

public interface VariantGenerator {
    List<GeneratedVariant> generate(String sourceDomain);
}

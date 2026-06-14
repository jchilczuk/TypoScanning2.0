package pl.jchilczuk.typoscanningapi.application.variant;

import pl.jchilczuk.typoscanningapi.domain.enums.VariantType;

public record GeneratedVariant(
        String domainName,
        VariantType variantType
) {
}

package pl.jchilczuk.typoscanningapi.application.variant;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import pl.jchilczuk.typoscanningapi.domain.enums.VariantType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class VariantGeneratorService implements VariantGenerator {

    @Value("classpath:variants/tlds.txt")
    private Resource tldsResource;

    @Value("classpath:variants/prefixes_suffixes.txt")
    private Resource affixesResource;

    @Value("classpath:variants/subdomains.txt")
    private Resource subdomainsResource;

    @Value("classpath:variants/homoglyphs.txt")
    private Resource homoglyphsResource;

    @Value("classpath:variants/keyboard_proximity.txt")
    private Resource keyboardProximityResource;

    private final List<String> tlds = new ArrayList<>();
    private final List<String> affixes = new ArrayList<>();
    private final List<String> subdomains = new ArrayList<>();
    private final Map<Character, List<String>> homoglyphs = new HashMap<>();
    private final Map<Character, List<Character>> keyboardProximity = new HashMap<>();

    @PostConstruct
    public void init() {
        loadListFromFile(tldsResource, tlds, "TLDs");
        loadListFromFile(affixesResource, affixes, "Prefixes/Suffixes");
        loadListFromFile(subdomainsResource, subdomains, "Subdomains");
        loadHomoglyphsFromFile(homoglyphsResource);
        loadKeyboardProximityFromFile(keyboardProximityResource);
    }

    private void loadListFromFile(Resource resource, List<String> targetList, String resourceName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    targetList.add(line);
                }
            }
            log.info("Loaded {} elements from {}", targetList.size(), resourceName);
        } catch (Exception e) {
            log.error("Failed to load {} from file", resourceName, e);
        }
    }

    private void loadHomoglyphsFromFile(Resource resource) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("#") && line.contains(",")) {
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        char originalChar = parts[0].trim().toLowerCase().charAt(0);
                        String replacement = parts[1].trim().toLowerCase();
                        homoglyphs.computeIfAbsent(originalChar, k -> new ArrayList<>()).add(replacement);
                        count++;
                    }
                }
            }
            log.info("Loaded {} homoglyph mappings", count);
        } catch (Exception e) {
            log.error("Failed to load homoglyphs from file", e);
        }
    }

    private void loadKeyboardProximityFromFile(Resource resource) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (!line.startsWith("#") && line.contains(":")) {
                    String[] mainParts = line.split(":", 2);
                    if (mainParts.length == 2) {
                        char originalChar = mainParts[0].trim().charAt(0);
                        String[] neighborsArray = mainParts[1].split(",");

                        List<Character> neighbors = new ArrayList<>();
                        for (String n : neighborsArray) {
                            String neighbor = n.trim();
                            if (!neighbor.isEmpty()) {
                                neighbors.add(neighbor.charAt(0));
                            }
                        }
                        keyboardProximity.put(originalChar, neighbors);
                        count++;
                    }
                }
            }
            log.info("Loaded {} keyboard proximity mappings", count);
        } catch (Exception e) {
            log.warn("Failed to load keyboard proximity from file, skipping.", e);
        }
    }

    @Override
    public List<GeneratedVariant> generate(String sourceDomain) {
        String normalized = normalizeDomain(sourceDomain);
        String baseName = extractBaseName(normalized);
        String originalTld = extractTld(normalized);

        Set<GeneratedVariant> variants = new LinkedHashSet<>();

        generateWrongTldVariants(baseName, variants);
        generateSubdomainVariants(baseName, originalTld, variants);
        generateComboVariants(baseName, originalTld, variants);
        generateSimpleTypoVariants(baseName, originalTld, variants);
        generateFatFingerVariants(baseName, originalTld, variants);
        generateHomoglyphVariants(baseName, originalTld, variants);

        return new ArrayList<>(variants);
    }

    private void generateWrongTldVariants(String baseName, Set<GeneratedVariant> variants) {
        for (String tld : tlds) {
            String extension = tld.startsWith(".") ? tld : "." + tld;
            variants.add(new GeneratedVariant(baseName + extension, VariantType.WRONG_TLD));
        }
    }

    private void generateSubdomainVariants(String baseName, String tld, Set<GeneratedVariant> variants) {
        for (String sub : subdomains) {
            // ex. www-domena.com
            variants.add(new GeneratedVariant(sub + "-" + baseName + tld, VariantType.COMBOSQUATTING));
            // ex. wwwdomena.com
            variants.add(new GeneratedVariant(sub + baseName + tld, VariantType.COMBOSQUATTING));
        }
    }

    private void generateComboVariants(String baseName, String tld, Set<GeneratedVariant> variants) {
        for (String affix : affixes) {
            // ex. login-domena.com
            variants.add(new GeneratedVariant(affix + "-" + baseName + tld, VariantType.COMBOSQUATTING));
            // ex. logindomena.com
            variants.add(new GeneratedVariant(affix + baseName + tld, VariantType.COMBOSQUATTING));

            // ex. domena-login.com
            variants.add(new GeneratedVariant(baseName + "-" + affix + tld, VariantType.COMBOSQUATTING));
            // ex. domenalogin.com
            variants.add(new GeneratedVariant(baseName + affix + tld, VariantType.COMBOSQUATTING));
        }
    }

    private void generateSimpleTypoVariants(String baseName, String tld, Set<GeneratedVariant> variants) {
        // ex. oogle.com
        if (baseName.length() > 1) {
            String withoutFirstChar = baseName.substring(1);
            variants.add(new GeneratedVariant(withoutFirstChar + tld, VariantType.TYPO));

            // ex. googl.com
            String withoutLastChar = baseName.substring(0, baseName.length() - 1);
            variants.add(new GeneratedVariant(withoutLastChar + tld, VariantType.TYPO));
        }

        for (int i = 0; i < baseName.length() - 1; i++) {
            char[] chars = baseName.toCharArray();
            char temp = chars[i];
            chars[i] = chars[i + 1];
            chars[i + 1] = temp;
            variants.add(new GeneratedVariant(new String(chars) + tld, VariantType.TYPO));
        }
    }

    private void generateFatFingerVariants(String baseName, String tld, Set<GeneratedVariant> variants) {
        for (int i = 0; i < baseName.length(); i++) {
            char c = baseName.charAt(i);
            if (keyboardProximity.containsKey(c)) {
                for (Character neighbor : keyboardProximity.get(c)) {
                    String typoName = baseName.substring(0, i) + neighbor + baseName.substring(i + 1);
                    variants.add(new GeneratedVariant(typoName + tld, VariantType.TYPO)); // lub nowy VariantType.FAT_FINGER
                }
            }
        }
    }

    private void generateHomoglyphVariants(String baseName, String tld, Set<GeneratedVariant> variants) {
        for (int i = 0; i < baseName.length(); i++) {
            char c = baseName.charAt(i);
            if (homoglyphs.containsKey(c)) {
                for (String replacement : homoglyphs.get(c)) {
                    String homoglyphName = baseName.substring(0, i) + replacement + baseName.substring(i + 1);
                    variants.add(new GeneratedVariant(homoglyphName + tld, VariantType.HOMOGLYPH));
                }
            }
        }
    }

    private String normalizeDomain(String sourceDomain) {
        return sourceDomain.trim().toLowerCase();
    }

    private String extractBaseName(String domain) {
        int dotIndex = domain.indexOf('.');
        return dotIndex > 0 ? domain.substring(0, dotIndex) : domain;
    }

    private String extractTld(String domain) {
        int dotIndex = domain.indexOf('.');
        return dotIndex > 0 ? domain.substring(dotIndex) : "";
    }
}
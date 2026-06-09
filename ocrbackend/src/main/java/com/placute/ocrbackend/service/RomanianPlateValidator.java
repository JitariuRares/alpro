package com.placute.ocrbackend.service;

import com.placute.ocrbackend.model.PlateType;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RomanianPlateValidator {

    private static final Set<String> VALID_COUNTY_CODES = Set.of(
            "AB", "AG", "AR", "B", "BC", "BH", "BN", "BR", "BT", "BV", "BZ",
            "CJ", "CL", "CS", "CT", "CV", "DB", "DJ", "GJ", "GL", "GR",
            "HD", "HR", "IF", "IL", "IS", "MH", "MM", "MS", "NT", "OT",
            "PH", "SB", "SJ", "SM", "SV", "TL", "TM", "TR", "VL", "VN", "VS"
    );

    private static final Pattern DIPLOMATIC_PATTERN = Pattern.compile("^(CD|TC|CO)\\d{6}$");
    private static final Pattern MAI_PATTERN = Pattern.compile("^MAI\\d{3,7}$");
    private static final Pattern MILITARY_PATTERN = Pattern.compile("^A\\d{3,7}$");
    private static final Pattern PROBE_PATTERN = Pattern.compile("^(B|[A-Z]{2})\\d{3}PROBE$");
    private static final Pattern TEMPORARY_PATTERN = Pattern.compile("^(B|[A-Z]{2})0\\d{2,5}$");
    private static final Pattern STANDARD_PATTERN = Pattern.compile("^(B|[A-Z]{2})\\d{2,3}[A-Z]{3}$");
    private static final Pattern LOCAL_PATTERN = Pattern.compile("^(?:[A-Z]{2}-[A-Z]\\d{2,5}|[A-Z]{4,12}\\d{2,5})$");

    private static final Pattern[] CANDIDATE_PATTERNS = {
            Pattern.compile("\\b(?:CD|TC|CO)\\s?\\d{3}\\s?\\d{3}\\b"),
            Pattern.compile("\\bMAI\\s?\\d{3,7}\\b"),
            Pattern.compile("\\bA\\s?\\d{3,7}\\b"),
            Pattern.compile("\\b(?:B|[A-Z]{2})\\s?\\d{3}\\s?PROBE\\b"),
            Pattern.compile("\\b(?:B|[A-Z]{2})\\s?0\\d{2,5}\\b"),
            Pattern.compile("\\b(?:B|[A-Z]{2})\\s?\\d{2,3}\\s?[A-Z]{3}\\b"),
            Pattern.compile("\\b(?:[A-Z]{2}-[A-Z]\\s?\\d{2,5}|[A-Z]{3,12}\\s?\\d{2,5})\\b")
    };

    public ValidationResult validate(String rawText) {
        String normalized = normalize(rawText);
        if (normalized == null) {
            return ValidationResult.unknown(null);
        }

        ValidationResult result = validateNormalized(normalized);
        if (result.plateType() != PlateType.UNKNOWN) {
            return result;
        }

        for (String candidate : candidates(rawText)) {
            result = validateNormalized(candidate);
            if (result.plateType() != PlateType.UNKNOWN) {
                return result;
            }
        }

        return ValidationResult.unknown(normalized);
    }

    public String extractNormalizedPlate(String rawText) {
        ValidationResult result = validate(rawText);
        return result.plateType() != PlateType.UNKNOWN ? result.normalizedPlate() : null;
    }

    private ValidationResult validateNormalized(String normalized) {
        Matcher diplomatic = DIPLOMATIC_PATTERN.matcher(normalized);
        if (diplomatic.matches()) {
            return new ValidationResult(normalized, PlateType.DIPLOMATIC);
        }

        if (MAI_PATTERN.matcher(normalized).matches()) {
            return new ValidationResult(normalized, PlateType.MAI);
        }

        if (MILITARY_PATTERN.matcher(normalized).matches()) {
            return new ValidationResult(normalized, PlateType.MILITARY);
        }

        Matcher probe = PROBE_PATTERN.matcher(normalized);
        if (probe.matches() && isValidCounty(probe.group(1))) {
            return new ValidationResult(normalized, PlateType.PROBE);
        }

        Matcher temporary = TEMPORARY_PATTERN.matcher(normalized);
        if (temporary.matches() && isValidCounty(temporary.group(1))) {
            return new ValidationResult(normalized, PlateType.TEMPORARY);
        }

        Matcher standard = STANDARD_PATTERN.matcher(normalized);
        if (standard.matches() && isValidCounty(standard.group(1))) {
            return new ValidationResult(normalized, PlateType.STANDARD);
        }

        if (LOCAL_PATTERN.matcher(normalized).matches() && !looksLikeMalformedOfficialPlate(normalized)) {
            return new ValidationResult(normalized, PlateType.LOCAL);
        }

        return ValidationResult.unknown(normalized);
    }

    private Set<String> candidates(String rawText) {
        String prepared = rawText == null
                ? ""
                : rawText.toUpperCase().replaceAll("[^A-Z0-9\\- ]", " ").replaceAll("\\s+", " ").trim();
        java.util.LinkedHashSet<String> matches = new java.util.LinkedHashSet<>();
        for (Pattern pattern : CANDIDATE_PATTERNS) {
            Matcher matcher = pattern.matcher(prepared);
            while (matcher.find()) {
                String normalized = normalize(matcher.group());
                if (normalized != null) {
                    matches.add(normalized);
                }
            }
        }
        return matches;
    }

    private String normalize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        String normalized = rawText.toUpperCase()
                .replaceAll("\\s+", "")
                .trim();
        return normalized.isBlank() ? null : normalized;
    }

    private boolean isValidCounty(String countyCode) {
        return countyCode != null && VALID_COUNTY_CODES.contains(countyCode);
    }

    private boolean looksLikeMalformedOfficialPlate(String normalized) {
        if (normalized == null) {
            return false;
        }
        if (normalized.startsWith("MAI") || normalized.startsWith("CD")
                || normalized.startsWith("TC") || normalized.startsWith("CO")) {
            return true;
        }
        return VALID_COUNTY_CODES.stream()
                .filter(code -> code.length() == 2)
                .anyMatch(code -> normalized.matches("^" + code + "[A-Z]{3}\\d{2,5}$"));
    }

    public record ValidationResult(String normalizedPlate, PlateType plateType) {
        static ValidationResult unknown(String normalizedPlate) {
            return new ValidationResult(normalizedPlate, PlateType.UNKNOWN);
        }
    }
}

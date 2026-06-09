package com.placute.ocrbackend;

import com.placute.ocrbackend.model.PlateType;
import com.placute.ocrbackend.service.RomanianPlateValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OcrServicePlateExtractionTest {

    private final RomanianPlateValidator validator = new RomanianPlateValidator();

    @ParameterizedTest(name = "{index}: {0} -> {1} ({2})")
    @CsvSource({
            "B 12 XYZ, B12XYZ, STANDARD",
            "B 123 XYZ, B123XYZ, STANDARD",
            "CJ 01 ABC, CJ01ABC, STANDARD",
            "CJ 123 ABC, CJ123ABC, STANDARD",
            "CT24AXK, CT24AXK, STANDARD",
            "SV 15 WDC, SV15WDC, STANDARD",
            "IF 99 ZZZ, IF99ZZZ, STANDARD",
            "TM 100 CAR, TM100CAR, STANDARD",
            "CD 123 101, CD123101, DIPLOMATIC",
            "TC 156 234, TC156234, DIPLOMATIC",
            "CO 205 113, CO205113, DIPLOMATIC",
            "CJ 012345, CJ012345, TEMPORARY",
            "B 0123, B0123, TEMPORARY",
            "IF 000999, IF000999, TEMPORARY",
            "CJ 101 PROBE, CJ101PROBE, PROBE",
            "B 234 PROBE, B234PROBE, PROBE",
            "A 1234, A1234, MILITARY",
            "A 1234567, A1234567, MILITARY",
            "MAI 12345, MAI12345, MAI",
            "MAI 1234567, MAI1234567, MAI",
            "CJ-N 1234, CJ-N1234, LOCAL",
            "CLUJ 1234, CLUJ1234, LOCAL"
    })
    void classifiesRomanianPlateFormats(String rawText, String normalizedPlate, PlateType plateType) {
        RomanianPlateValidator.ValidationResult result = validator.validate(rawText);

        assertThat(result.normalizedPlate()).isEqualTo(normalizedPlate);
        assertThat(result.plateType()).isEqualTo(plateType);
    }

    @ParameterizedTest(name = "{index}: rejects {0}")
    @CsvSource({
            "ZZ 123 ABC",
            "CJ 1 ABC",
            "B 1234 ABC",
            "CJ ABC 123",
            "CD 12 101",
            "MAI 12",
            "A 12",
            "CJ 12 PROBE",
            "XX 012345",
            "nu exista placuta aici"
    })
    void rejectsInvalidRomanianPlateFormats(String rawText) {
        RomanianPlateValidator.ValidationResult result = validator.validate(rawText);

        assertThat(result.plateType()).isEqualTo(PlateType.UNKNOWN);
    }

    @ParameterizedTest(name = "{index}: extracts {1} from OCR-like text")
    @CsvSource({
            "OCR result: CJ 123 ABC confidence 0.91, CJ123ABC, STANDARD",
            "detected text [CD 123 101] on blue plate, CD123101, DIPLOMATIC",
            "camera frame shows MAI 12345 near gate, MAI12345, MAI",
            "possible dirty plate B 234 PROBE in image, B234PROBE, PROBE"
    })
    void extractsCandidateFromLongOcrText(String rawText, String normalizedPlate, PlateType plateType) {
        RomanianPlateValidator.ValidationResult result = validator.validate(rawText);

        assertThat(result.normalizedPlate()).isEqualTo(normalizedPlate);
        assertThat(result.plateType()).isEqualTo(plateType);
    }
}

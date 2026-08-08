package com.kleos.transfers.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Display names and aliases for FIFA association codes.
 *
 * <p>Used to resolve free-text search queries like {@code Spain} or {@code FRA}
 * into nationality / country codes stored on Player and Club.
 */
public final class FootballCountryNames {

    private static final Map<String, String> CODE_TO_NAME;
    private static final Map<String, String> ALIAS_TO_CODE;

    static {
        Map<String, String> names = new LinkedHashMap<>();
        // UEFA (common)
        names.put("ALB", "Albania");
        names.put("AND", "Andorra");
        names.put("ARM", "Armenia");
        names.put("AUT", "Austria");
        names.put("AZE", "Azerbaijan");
        names.put("BEL", "Belgium");
        names.put("BIH", "Bosnia and Herzegovina");
        names.put("BLR", "Belarus");
        names.put("BUL", "Bulgaria");
        names.put("CRO", "Croatia");
        names.put("CYP", "Cyprus");
        names.put("CZE", "Czechia");
        names.put("DEN", "Denmark");
        names.put("ENG", "England");
        names.put("ESP", "Spain");
        names.put("EST", "Estonia");
        names.put("FAR", "Faroe Islands");
        names.put("FIN", "Finland");
        names.put("FRA", "France");
        names.put("GEO", "Georgia");
        names.put("GER", "Germany");
        names.put("GIB", "Gibraltar");
        names.put("GRE", "Greece");
        names.put("HUN", "Hungary");
        names.put("IRL", "Republic of Ireland");
        names.put("ISL", "Iceland");
        names.put("ISR", "Israel");
        names.put("ITA", "Italy");
        names.put("KAZ", "Kazakhstan");
        names.put("KOS", "Kosovo");
        names.put("LVA", "Latvia");
        names.put("LIE", "Liechtenstein");
        names.put("LTU", "Lithuania");
        names.put("LUX", "Luxembourg");
        names.put("MDA", "Moldova");
        names.put("MKD", "North Macedonia");
        names.put("MLT", "Malta");
        names.put("MNE", "Montenegro");
        names.put("NED", "Netherlands");
        names.put("NIR", "Northern Ireland");
        names.put("NOR", "Norway");
        names.put("POL", "Poland");
        names.put("POR", "Portugal");
        names.put("ROU", "Romania");
        names.put("RUS", "Russia");
        names.put("SCO", "Scotland");
        names.put("SMR", "San Marino");
        names.put("SRB", "Serbia");
        names.put("SUI", "Switzerland");
        names.put("SVK", "Slovakia");
        names.put("SVN", "Slovenia");
        names.put("SWE", "Sweden");
        names.put("TUR", "Türkiye");
        names.put("UKR", "Ukraine");
        names.put("WAL", "Wales");
        // CONMEBOL
        names.put("ARG", "Argentina");
        names.put("BOL", "Bolivia");
        names.put("BRA", "Brazil");
        names.put("CHI", "Chile");
        names.put("COL", "Colombia");
        names.put("ECU", "Ecuador");
        names.put("PAR", "Paraguay");
        names.put("PER", "Peru");
        names.put("URU", "Uruguay");
        names.put("VEN", "Venezuela");
        // CONCACAF (common)
        names.put("CAN", "Canada");
        names.put("CRC", "Costa Rica");
        names.put("CUB", "Cuba");
        names.put("JAM", "Jamaica");
        names.put("MEX", "Mexico");
        names.put("PAN", "Panama");
        names.put("USA", "United States");
        // CAF (common)
        names.put("ALG", "Algeria");
        names.put("ANG", "Angola");
        names.put("CIV", "Côte d'Ivoire");
        names.put("CMR", "Cameroon");
        names.put("COD", "DR Congo");
        names.put("CGO", "Congo");
        names.put("EGY", "Egypt");
        names.put("GHA", "Ghana");
        names.put("GUI", "Guinea");
        names.put("MAR", "Morocco");
        names.put("MLI", "Mali");
        names.put("NGA", "Nigeria");
        names.put("RSA", "South Africa");
        names.put("SEN", "Senegal");
        names.put("TUN", "Tunisia");
        // AFC / OFC (common)
        names.put("AUS", "Australia");
        names.put("CHN", "China");
        names.put("IRN", "Iran");
        names.put("IRQ", "Iraq");
        names.put("JPN", "Japan");
        names.put("KOR", "South Korea");
        names.put("KSA", "Saudi Arabia");
        names.put("QAT", "Qatar");
        names.put("UAE", "United Arab Emirates");
        names.put("UZB", "Uzbekistan");
        names.put("NZL", "New Zealand");
        CODE_TO_NAME = Collections.unmodifiableMap(names);

        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("turkey", "TUR");
        aliases.put("holland", "NED");
        aliases.put("ivory coast", "CIV");
        aliases.put("cote d'ivoire", "CIV");
        aliases.put("cote divoire", "CIV");
        aliases.put("usa", "USA");
        aliases.put("us", "USA");
        aliases.put("united states of america", "USA");
        aliases.put("korea", "KOR");
        aliases.put("republic of korea", "KOR");
        aliases.put("czech republic", "CZE");
        aliases.put("ireland", "IRL");
        aliases.put("macedonia", "MKD");
        aliases.put("bosnia", "BIH");
        ALIAS_TO_CODE = Collections.unmodifiableMap(aliases);
    }

    private FootballCountryNames() {
    }

    public static String displayName(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return CODE_TO_NAME.getOrDefault(normalized, normalized);
    }

    /**
     * Resolves a free-text query to FIFA codes that should match nationality/country search.
     *
     * <p>Matches exact codes, display-name substrings (length ≥ 3), and a small alias table.
     */
    public static Set<String> codesMatchingQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Set.of();
        }
        String trimmed = rawQuery.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);
        String lower = trimmed.toLowerCase(Locale.ROOT);
        Set<String> codes = new HashSet<>();

        if (upper.length() == 3 && FootballNationalityCodes.isValid(upper)) {
            codes.add(upper);
        }

        String aliasCode = ALIAS_TO_CODE.get(lower);
        if (aliasCode != null) {
            codes.add(aliasCode);
        }

        if (lower.length() >= 3) {
            for (Map.Entry<String, String> entry : CODE_TO_NAME.entrySet()) {
                String name = entry.getValue().toLowerCase(Locale.ROOT);
                if (name.contains(lower)) {
                    codes.add(entry.getKey());
                }
            }
        }

        return codes;
    }
}

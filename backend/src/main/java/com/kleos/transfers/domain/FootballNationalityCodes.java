package com.kleos.transfers.domain;

import java.util.Set;

/**
 * FIFA association nationality codes used for player identity.
 *
 * <p>These intentionally follow football convention (ENG, GER, NED) rather than
 * ISO 3166-1 alpha-3 (GBR, DEU, NLD), which does not match football data sources.
 */
public final class FootballNationalityCodes {

    public static final Set<String> CODES = Set.of(
            // UEFA
            "ALB", "AND", "ARM", "AUT", "AZE", "BEL", "BIH", "BLR", "BUL", "CRO",
            "CYP", "CZE", "DEN", "ENG", "ESP", "EST", "FAR", "FIN", "FRA", "GEO",
            "GER", "GIB", "GRE", "HUN", "IRL", "ISL", "ISR", "ITA", "KAZ", "KOS",
            "LVA", "LIE", "LTU", "LUX", "MDA", "MKD", "MLT", "MNE", "NED", "NIR",
            "NOR", "POL", "POR", "ROU", "RUS", "SCO", "SMR", "SRB", "SUI", "SVK",
            "SVN", "SWE", "TUR", "UKR", "WAL",
            // CONMEBOL
            "ARG", "BOL", "BRA", "CHI", "COL", "ECU", "PAR", "PER", "URU", "VEN",
            // CONCACAF
            "AIA", "ATG", "ARU", "BAH", "BER", "BLZ", "BRB", "CAN", "CAY", "CRC",
            "CUB", "CUW", "DMA", "DOM", "GRN", "GUA", "GUY", "HAI", "HON", "JAM",
            "LCA", "MEX", "MSR", "NCA", "PAN", "PUR", "SKN", "SLV", "SUR", "TRI",
            "TCA", "USA", "VIN", "VGB", "VIR",
            // CAF
            "ALG", "ANG", "BDI", "BEN", "BFA", "BOT", "CGO", "CHA", "CIV", "CMR",
            "COD", "COM", "CPV", "CTA", "DJI", "EGY", "EQG", "ERI", "ETH", "GAB",
            "GAM", "GHA", "GNB", "GUI", "KEN", "LBR", "LBY", "LES", "MAD", "MAR",
            "MAW", "MLI", "MOZ", "MTN", "MUR", "NAM", "NGA", "NIG", "RSA", "RWA",
            "SDN", "SEN", "SEY", "SLE", "SOM", "SSD", "STP", "SWZ", "TAN", "TOG",
            "TUN", "UGA", "ZAM", "ZIM",
            // AFC
            "AFG", "AUS", "BHR", "BAN", "BHU", "BRU", "CAM", "CHN", "GUM", "HKG",
            "IDN", "IND", "IRN", "IRQ", "JPN", "JOR", "KGZ", "KOR", "KSA", "KUW",
            "LAO", "LIB", "MAC", "MAS", "MDV", "MNG", "MYA", "NEP", "OMA", "PAK",
            "PHI", "PLE", "PRK", "QAT", "SIN", "SRI", "SYR", "THA", "TJK", "TKM",
            "TLS", "TPE", "UAE", "UZB", "VIE", "YEM",
            // OFC
            "ASA", "COK", "FIJ", "NCL", "NZL", "PNG", "SAM", "SOL", "TAH", "TGA",
            "VAN"
    );

    private FootballNationalityCodes() {
    }

    public static boolean isValid(String code) {
        return code != null && CODES.contains(code);
    }
}

package simulation;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class TeamColors {

    private static final Map<String, Color[]> TEAM_MAP = buildMap();

    private TeamColors() {}

    private static Map<String, Color[]> buildMap() {
        Map<String, Color[]> m = new HashMap<>();
        // ACC-inspired teams
        m.put("BC",    colors(0x98002E, 0xDBB03C)); // Boston College
        m.put("SCT",   colors(0x522886, 0x000000)); // South Carolina Tech
        m.put("DUKE",  colors(0x003366, 0xFFFFFF)); // Duke
        m.put("FSU",   colors(0x782F40, 0xCEB888)); // Florida State
        m.put("GT",    colors(0x00254C, 0xAAAAAA)); // Georgia Tech
        m.put("LOUI",  colors(0xAD1F2B, 0x000000)); // Louisville
        m.put("MIA",   colors(0x005030, 0xF47321)); // Miami
        m.put("UNC",   colors(0x7BAFD4, 0xFFFFFF)); // North Carolina
        m.put("NCST",  colors(0xCC0000, 0x000000)); // NC State
        m.put("PITT",  colors(0x003366, 0xCEC0A8)); // Pittsburgh
        m.put("SYR",   colors(0xD44500, 0x000E54)); // Syracuse
        m.put("WINS",  colors(0x000000, 0xCBB677)); // Wake Forest inspired
        m.put("VIR",   colors(0x232D4B, 0xE57200)); // Virginia
        m.put("VPOL",  colors(0x660000, 0xCC6600)); // Virginia Tech

        // Great Lakes (Big Ten inspired)
        m.put("ILL",   colors(0xE84A27, 0x13294B)); // Illinois
        m.put("IND",   colors(0x990000, 0xFFFFFF)); // Indiana
        m.put("IOW",   colors(0x000000, 0xFFCD00)); // Iowa
        m.put("MARY",  colors(0xE03A3E, 0xFFCD00)); // Maryland
        m.put("MIC",   colors(0x00274C, 0xFFCB05)); // Michigan
        m.put("MSU",   colors(0x18453B, 0xFFFFFF)); // Michigan State
        m.put("MIN",   colors(0x7A0019, 0xFFCC33)); // Minnesota
        m.put("NEB",   colors(0xE41C38, 0xF5F1E7)); // Nebraska
        m.put("CHI",   colors(0x4E2A84, 0xFFFFFF)); // Northwestern inspired
        m.put("OSU",   colors(0xBB0000, 0xFFFFFF)); // Ohio State
        m.put("PSU",   colors(0x003366, 0xFFFFFF)); // Penn State
        m.put("PUR",   colors(0xCEB888, 0x000000)); // Purdue
        m.put("NJ",    colors(0xCC0033, 0x000000)); // Rutgers inspired
        m.put("WIS",   colors(0xC5050C, 0xFFFFFF)); // Wisconsin

        // Southwest (Big 12 inspired)
        m.put("WACO",  colors(0x003015, 0xFFC60E)); // Baylor inspired
        m.put("ISU",   colors(0xCC0000, 0x003366)); // Iowa State
        m.put("TTEC",  colors(0xCC0000, 0x000000)); // Texas Tech
        m.put("KAN",   colors(0x0051BA, 0xE8000D)); // Kansas
        m.put("KSU",   colors(0x4B2E83, 0xFFFFFF)); // Kansas State
        m.put("OKL",   colors(0x841617, 0xFDF9D8)); // Oklahoma
        m.put("OKST",  colors(0xFF6600, 0x000000)); // Oklahoma State
        m.put("TEX",   colors(0xBF5700, 0xFFFFFF)); // Texas
        m.put("FTWO",  colors(0x4D4D4D, 0xFFFFFF)); // TCU inspired
        m.put("WVU",   colors(0x002855, 0xEAAA00)); // West Virginia

        // Pacific (Pac-12 inspired)
        m.put("ARIZ",  colors(0xCC0033, 0x003366)); // Arizona
        m.put("ASU",   colors(0x8C1D40, 0xFFC627)); // Arizona State
        m.put("CAL",   colors(0x003262, 0xFDB515)); // California
        m.put("COL",   colors(0xCFB87C, 0x000000)); // Colorado
        m.put("OREG",  colors(0x004F27, 0xFFE620)); // Oregon
        m.put("ORST",  colors(0xDC4405, 0x000000)); // Oregon State
        m.put("SF",    colors(0x00274C, 0x72A0C1)); // San Francisco
        m.put("LA",    colors(0xD4A017, 0x4B0082)); // Pasadena
        m.put("SCAL",  colors(0x990000, 0xFFCC00)); // Southern Cal
        m.put("WASH",  colors(0x4B2E83, 0x85754D)); // Washington
        m.put("WSU",   colors(0x981E32, 0xFFFFFF)); // Washington State
        m.put("UTAH",  colors(0xCC0000, 0x000000)); // Utah

        // Southern (SEC inspired)
        m.put("BAMA",  colors(0x9E1B32, 0xFFFFFF)); // Alabama
        m.put("ARK",   colors(0x9D2235, 0xFFFFFF)); // Arkansas
        m.put("AUB",   colors(0x03244D, 0xDD550C)); // Auburn
        m.put("FLOR",  colors(0x0021A5, 0xFA4616)); // Florida
        m.put("UGA",   colors(0xBA0C2F, 0x000000)); // Georgia
        m.put("UK",    colors(0x0032A0, 0xFFFFFF)); // Kentucky
        m.put("LSU",   colors(0x461D7C, 0xFDD023)); // LSU
        m.put("MIZ",   colors(0x000000, 0xF1B82D)); // Missouri
        m.put("MISS",  colors(0xCE1126, 0xFFFFFF)); // Ole Miss
        m.put("MSST",  colors(0x660000, 0xFFFFFF)); // Mississippi State
        m.put("SC",    colors(0x73000A, 0x000000)); // South Carolina
        m.put("TENN",  colors(0xFF8200, 0x58595B)); // Tennessee
        m.put("COLST", colors(0x500000, 0xFFFFFF)); // Texas A&M inspired
        m.put("NASH",  colors(0x866D4B, 0x000000)); // Vanderbilt inspired

        // Patriot (American Athletic inspired)
        m.put("UCF",   colors(0xBA9B36, 0x000000)); // UCF
        m.put("CINN",  colors(0x000000, 0xE00122)); // Cincinnati
        m.put("ECU",   colors(0x592A7E, 0x000000)); // East Carolina
        m.put("HOU",   colors(0xC8102E, 0xFFFFFF)); // Houston
        m.put("MEMP",  colors(0x003D7A, 0x898D8D)); // Memphis
        m.put("NAVY",  colors(0x000080, 0xCFB53B)); // Navy
        m.put("USF",   colors(0x006747, 0xCFC493)); // South Florida
        m.put("DAL",   colors(0x154733, 0xCC9933)); // SMU inspired
        m.put("PHL",   colors(0x860038, 0xFFFFFF)); // Temple inspired
        m.put("TUL",   colors(0x006747, 0x2D6CC0)); // Tulane
        m.put("TULS",  colors(0x00347A, 0xCC0000)); // Tulsa
        m.put("CONN",  colors(0x000E2F, 0xFFFFFF)); // Connecticut

        // Central (MAC inspired)
        m.put("AKR",   colors(0x00214E, 0x84754E)); // Akron
        m.put("MUNC",  colors(0xBA0C2F, 0xFFFFFF)); // Ball State inspired
        m.put("BG",    colors(0xFE5000, 0x4F2C1D)); // Bowling Green
        m.put("BUF",   colors(0x006699, 0xFFFFFF)); // Buffalo
        m.put("CMU",   colors(0x6A0032, 0xFFCC00)); // Central Michigan
        m.put("EMU",   colors(0x006633, 0xFFFFFF)); // Eastern Michigan
        m.put("KENT",  colors(0x002B5C, 0xCEB888)); // Kent State
        m.put("MiOH",  colors(0xAA0000, 0xFFFFFF)); // Miami OH
        m.put("NIU",   colors(0xCE1126, 0x000000)); // Northern Illinois
        m.put("OHIO",  colors(0x006A4E, 0xFFFFFF)); // Ohio
        m.put("TOL",   colors(0x003870, 0xFFC82E)); // Toledo
        m.put("WMU",   colors(0x4B0082, 0xCEB888)); // Western Michigan

        // Liberty (C-USA inspired)
        m.put("CHAR",  colors(0x005DAA, 0x00703C)); // Charlotte
        m.put("BOCA",  colors(0x005030, 0xFFFFFF)); // FAU inspired
        m.put("MINT",  colors(0x003366, 0xF47321)); // FIU inspired
        m.put("LPOL",  colors(0x002B5C, 0xBB0000)); // Louisiana Tech inspired
        m.put("HUNT",  colors(0x008E46, 0xFFFFFF)); // Marshall inspired
        m.put("MTSU",  colors(0x0066CC, 0xFFFFFF)); // Middle Tennessee
        m.put("NTEX",  colors(0x00853E, 0xFFFFFF)); // North Texas
        m.put("NORF",  colors(0x006A4E, 0xFF9911)); // Norfolk State inspired
        m.put("SHOU",  colors(0xC8102E, 0x003559)); // Houston inspired
        m.put("SMIS",  colors(0xFEDB00, 0x000000)); // Southern Miss
        m.put("UAB",   colors(0x006341, 0xCEB888)); // UAB
        m.put("UTEP",  colors(0xFF6600, 0x003366)); // UTEP
        m.put("UTSA",  colors(0x002B5C, 0xFF6600)); // UTSA
        m.put("WKU",   colors(0xBB0000, 0xFFFFFF)); // Western Kentucky

        // Mountain (Mountain West inspired)
        m.put("AF",    colors(0x003366, 0xFFFFFF)); // Air Force
        m.put("BOIS",  colors(0x003366, 0xBA9B36)); // Boise State
        m.put("CSU",   colors(0x1E4D2B, 0xCEB888)); // Colorado State
        m.put("FRES",  colors(0x002147, 0xDB4437)); // Fresno State
        m.put("HAW",   colors(0x024731, 0xFFFFFF)); // Hawaii
        m.put("NEV",   colors(0x003366, 0xFFFFFF)); // Nevada
        m.put("NMEX",  colors(0xBA0C2F, 0xFFFFFF)); // New Mexico
        m.put("SD",    colors(0xA5000B, 0xFFFFFF)); // San Diego State
        m.put("SJ",    colors(0x005B96, 0xFFCC00)); // San Jose State
        m.put("UNLV",  colors(0xCC0000, 0x000000)); // UNLV
        m.put("UTST",  colors(0x003366, 0x96C5E8)); // Utah State
        m.put("WYM",   colors(0x492F24, 0xFFC425)); // Wyoming

        // Sunshine (Sun Belt inspired)
        m.put("APP",   colors(0x000000, 0xFFCC00)); // Appalachian State
        m.put("AKS",   colors(0xCC0000, 0x000000)); // Arkansas State
        m.put("COC",   colors(0x006BA6, 0xCC9900)); // Coastal Carolina
        m.put("LMON",  colors(0x922247, 0xFFFFFF)); // Louisiana Monroe
        m.put("LALF",  colors(0xCE181E, 0xFFFFFF)); // Louisiana Lafayette
        m.put("TROY",  colors(0x85714D, 0xBA0C2F)); // Troy
        m.put("TXST",  colors(0x501214, 0xFFFFFF)); // Texas State
        m.put("SOGA",  colors(0x003366, 0xFFFFFF)); // Georgia Southern inspired
        m.put("GAST",  colors(0x003DA5, 0xFFFFFF)); // Georgia State
        m.put("SAL",   colors(0x00205B, 0xCC0000)); // South Alabama

        // Independent
        m.put("ARMY",  colors(0x000000, 0xCEB888)); // Army
        m.put("PROV",  colors(0x002E62, 0xFFFFFF)); // BYU inspired
        m.put("LYNC",  colors(0xCC0000, 0x000000)); // Liberty
        m.put("NMST",  colors(0x8C1D40, 0x000000)); // New Mexico State
        m.put("SB",    colors(0x002B5C, 0xCEB888)); // Notre Dame inspired
        m.put("MASS",  colors(0x881C1C, 0xFFFFFF)); // Umass

        return Collections.unmodifiableMap(m);
    }

    private static Color[] colors(int primaryRgb, int secondaryRgb) {
        return new Color[] { new Color(primaryRgb), new Color(secondaryRgb) };
    }

    public static Color primary(String abbr) {
        Color[] pair = TEAM_MAP.get(abbr);
        return pair != null ? pair[0] : new Color(100, 100, 100);
    }

    public static Color secondary(String abbr) {
        Color[] pair = TEAM_MAP.get(abbr);
        return pair != null ? pair[1] : new Color(180, 180, 180);
    }
}

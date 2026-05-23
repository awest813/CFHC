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
        m.put("BC",    colors(0x98002E, 0xDBB03C)); // Boston Eagles
        m.put("SCT",   colors(0x522886, 0x000000)); // South Carolina Tech Tigers
        m.put("DUKE",  colors(0x003366, 0xFFFFFF)); // Durham Blue Devils
        m.put("FSU",   colors(0x782F40, 0xCEB888)); // Florida State Seminoles
        m.put("GT",    colors(0x00254C, 0xAAAAAA)); // Georgia Tech Yellow Jackets
        m.put("LOUI",  colors(0xAD1F2B, 0x000000)); // Louisville Cardinals
        m.put("MIA",   colors(0x005030, 0xF47321)); // Miami Hurricanes
        m.put("UNC",   colors(0x7BAFD4, 0xFFFFFF)); // North Carolina Tar Heels
        m.put("NCST",  colors(0xCC0000, 0x000000)); // NC State Wolfpack
        m.put("PITT",  colors(0x003366, 0xCEC0A8)); // Pittsburgh Panthers
        m.put("SYR",   colors(0xD44500, 0x000E54)); // Syracuse Orange
        m.put("WINS",  colors(0x000000, 0xCBB677)); // Winston-Salem Demon Deacons
        m.put("VIR",   colors(0x232D4B, 0xE57200)); // Virginia Cavaliers
        m.put("VPOL",  colors(0x660000, 0xCC6600)); // Virginia Poly Hokies

        // Great Lakes (Big Ten inspired)
        m.put("ILL",   colors(0xE84A27, 0x13294B)); // Illinois Fighting Illini
        m.put("IND",   colors(0x990000, 0xFFFFFF)); // Indiana Hoosiers
        m.put("IOW",   colors(0x000000, 0xFFCD00)); // Iowa Hawkeyes
        m.put("MARY",  colors(0xE03A3E, 0xFFCD00)); // Maryland Terrapins
        m.put("MIC",   colors(0x00274C, 0xFFCB05)); // Michigan Wolverines
        m.put("MSU",   colors(0x18453B, 0xFFFFFF)); // Michigan State Spartans
        m.put("MIN",   colors(0x7A0019, 0xFFCC33)); // Minnesota Golden Gophers
        m.put("NEB",   colors(0xE41C38, 0xF5F1E7)); // Nebraska Cornhuskers
        m.put("CHI",   colors(0x4E2A84, 0xFFFFFF)); // Chicago Wildcats
        m.put("OSU",   colors(0xBB0000, 0xFFFFFF)); // Ohio State Buckeyes
        m.put("PSU",   colors(0x003366, 0xFFFFFF)); // Penn State Nittany Lions
        m.put("PUR",   colors(0xCEB888, 0x000000)); // Purdue Boilermakers
        m.put("NJ",    colors(0xCC0033, 0x000000)); // New Jersey Scarlet Knights
        m.put("WIS",   colors(0xC5050C, 0xFFFFFF)); // Wisconsin Badgers

        // Southwest (Big 12 inspired)
        m.put("WACO",  colors(0x003015, 0xFFC60E)); // Waco Bears
        m.put("ISU",   colors(0xCC0000, 0x003366)); // Iowa State Cyclones
        m.put("TTEC",  colors(0xCC0000, 0x000000)); // Texas Tech Red Raiders
        m.put("KAN",   colors(0x0051BA, 0xE8000D)); // Kansas Jayhawks
        m.put("KSU",   colors(0x4B2E83, 0xFFFFFF)); // Kansas State Wildcats
        m.put("OKL",   colors(0x841617, 0xFDF9D8)); // Oklahoma Sooners
        m.put("OKST",  colors(0xFF6600, 0x000000)); // Oklahoma State Cowboys
        m.put("TEX",   colors(0xBF5700, 0xFFFFFF)); // Texas Longhorns
        m.put("FTWO",  colors(0x4D4D4D, 0xFFFFFF)); // Ft Worth Horned Frogs
        m.put("WVU",   colors(0x002855, 0xEAAA00)); // West Virginia Mountaineers

        // Pacific (Pac-12 inspired)
        m.put("ARIZ",  colors(0xCC0033, 0x003366)); // Arizona Wildcats
        m.put("ASU",   colors(0x8C1D40, 0xFFC627)); // Arizona State Sun Devils
        m.put("CAL",   colors(0x003262, 0xFDB515)); // California Golden Bears
        m.put("COL",   colors(0xCFB87C, 0x000000)); // Colorado Buffaloes
        m.put("OREG",  colors(0x004F27, 0xFFE620)); // Oregon Ducks
        m.put("ORST",  colors(0xDC4405, 0x000000)); // Oregon State Beavers
        m.put("SF",    colors(0x00274C, 0x72A0C1)); // San Francisco Cardinal
        m.put("LA",    colors(0xD4A017, 0x4B0082)); // Pasadena Bruins
        m.put("SCAL",  colors(0x990000, 0xFFCC00)); // Southern Cal Trojans
        m.put("WASH",  colors(0x4B2E83, 0x85754D)); // Washington Huskies
        m.put("WSU",   colors(0x981E32, 0xFFFFFF)); // Washington State Cougars
        m.put("UTAH",  colors(0xCC0000, 0x000000)); // Utah Utes

        // Southern (SEC inspired)
        m.put("BAMA",  colors(0x9E1B32, 0xFFFFFF)); // Alabama Crimson Tide
        m.put("ARK",   colors(0x9D2235, 0xFFFFFF)); // Arkansas Razorbacks
        m.put("AUB",   colors(0x03244D, 0xDD550C)); // Auburn Tigers
        m.put("FLOR",  colors(0x0021A5, 0xFA4616)); // Florida Gators
        m.put("UGA",   colors(0xBA0C2F, 0x000000)); // Georgia Bulldogs
        m.put("UK",    colors(0x0032A0, 0xFFFFFF)); // Kentucky Wildcats
        m.put("LSU",   colors(0x461D7C, 0xFDD023)); // Louisiana State Tigers
        m.put("MIZ",   colors(0x000000, 0xF1B82D)); // Missouri Tigers
        m.put("MISS",  colors(0xCE1126, 0xFFFFFF)); // Ole Miss Rebels
        m.put("MSST",  colors(0x660000, 0xFFFFFF)); // Mississippi State Bulldogs
        m.put("SC",    colors(0x73000A, 0x000000)); // South Carolina Gamecocks
        m.put("TENN",  colors(0xFF8200, 0x58595B)); // Tennessee Volunteers
        m.put("COLST", colors(0x500000, 0xFFFFFF)); // College Station Aggies
        m.put("NASH",  colors(0x866D4B, 0x000000)); // Nashville Commodores

        // Patriot (American Athletic inspired)
        m.put("UCF",   colors(0xBA9B36, 0x000000)); // Central Florida Knights
        m.put("CINN",  colors(0x000000, 0xE00122)); // Cincinnati Bearcats
        m.put("ECU",   colors(0x592A7E, 0x000000)); // East Carolina Pirates
        m.put("HOU",   colors(0xC8102E, 0xFFFFFF)); // Houston Cougars
        m.put("MEMP",  colors(0x003D7A, 0x898D8D)); // Memphis Tigers
        m.put("NAVY",  colors(0x000080, 0xCFB53B)); // Navy Midshipmen
        m.put("USF",   colors(0x006747, 0xCFC493)); // South Florida Bulls
        m.put("DAL",   colors(0x154733, 0xCC9933)); // Dallas Mustangs
        m.put("PHL",   colors(0x860038, 0xFFFFFF)); // Philadelphia Owls
        m.put("TUL",   colors(0x006747, 0x2D6CC0)); // Tulane Green Wave
        m.put("TULS",  colors(0x00347A, 0xCC0000)); // Tulsa Golden Hurricane
        m.put("CONN",  colors(0x000E2F, 0xFFFFFF)); // Connecticut Huskies

        // Central (MAC inspired)
        m.put("AKR",   colors(0x00214E, 0x84754E)); // Akron Zips
        m.put("MUNC",  colors(0xBA0C2F, 0xFFFFFF)); // Muncie Cardinals
        m.put("BG",    colors(0xFE5000, 0x4F2C1D)); // Bowling Green Falcons
        m.put("BUF",   colors(0x006699, 0xFFFFFF)); // Buffalo Bulls
        m.put("CMU",   colors(0x6A0032, 0xFFCC00)); // Central Michigan Chippewas
        m.put("EMU",   colors(0x006633, 0xFFFFFF)); // Eastern Michigan Eagles
        m.put("KENT",  colors(0x002B5C, 0xCEB888)); // Kent Golden Flashes
        m.put("MiOH",  colors(0xAA0000, 0xFFFFFF)); // Miami OH RedHawks
        m.put("NIU",   colors(0xCE1126, 0x000000)); // Northern Illinois Huskies
        m.put("OHIO",  colors(0x006A4E, 0xFFFFFF)); // Ohio Bobcats
        m.put("TOL",   colors(0x003870, 0xFFC82E)); // Toledo Rockets
        m.put("WMU",   colors(0x4B0082, 0xCEB888)); // Western Michigan Broncos

        // Liberty (C-USA inspired)
        m.put("CHAR",  colors(0x005DAA, 0x00703C)); // Charlotte 49ers
        m.put("BOCA",  colors(0x005030, 0xFFFFFF)); // Boca Raton Owls
        m.put("MINT",  colors(0x003366, 0xF47321)); // Miami Intl Panthers
        m.put("LPOL",  colors(0x002B5C, 0xBB0000)); // Louisiana Poly Bulldogs
        m.put("HUNT",  colors(0x008E46, 0xFFFFFF)); // Huntington Thundering Herd
        m.put("MTSU",  colors(0x0066CC, 0xFFFFFF)); // Mid Tenn State Blue Raiders
        m.put("NTEX",  colors(0x00853E, 0xFFFFFF)); // North Texas Mean Green
        m.put("NORF",  colors(0x006A4E, 0xFF9911)); // Norfolk Monarchs
        m.put("SHOU",  colors(0xC8102E, 0x003559)); // South Houston Owls
        m.put("SMIS",  colors(0xFEDB00, 0x000000)); // South Mississippi Golden Eagles
        m.put("UAB",   colors(0x006341, 0xCEB888)); // Birmingham Blazers
        m.put("UTEP",  colors(0xFF6600, 0x003366)); // El Paso Miners
        m.put("UTSA",  colors(0x002B5C, 0xFF6600)); // San Antonio Roadrunners
        m.put("WKU",   colors(0xBB0000, 0xFFFFFF)); // Western Kentucky Hilltoppers

        // Mountain (Mountain West inspired)
        m.put("AF",    colors(0x003366, 0xFFFFFF)); // Air Force Falcons
        m.put("BOIS",  colors(0x003366, 0xBA9B36)); // Boise Broncos
        m.put("CSU",   colors(0x1E4D2B, 0xCEB888)); // Colorado State Rams
        m.put("FRES",  colors(0x002147, 0xDB4437)); // Fresno Bulldogs
        m.put("HAW",   colors(0x024731, 0xFFFFFF)); // Hawaii Rainbow Warriors
        m.put("NEV",   colors(0x003366, 0xFFFFFF)); // Nevada Wolf Pack
        m.put("NMEX",  colors(0xBA0C2F, 0xFFFFFF)); // New Mexico Lobos
        m.put("SD",    colors(0xA5000B, 0xFFFFFF)); // San Diego Aztecs
        m.put("SJ",    colors(0x005B96, 0xFFCC00)); // San Jose Spartans
        m.put("UNLV",  colors(0xCC0000, 0x000000)); // Las Vegas Rebels
        m.put("UTST",  colors(0x003366, 0x96C5E8)); // Utah State Aggies
        m.put("WYM",   colors(0x492F24, 0xFFC425)); // Wyoming Cowboys

        // Sunshine (Sun Belt inspired)
        m.put("APP",   colors(0x000000, 0xFFCC00)); // Appalachian Mountaineers
        m.put("AKS",   colors(0xCC0000, 0x000000)); // Arkansas State Red Wolves
        m.put("COA",   colors(0x006BA6, 0xCC9900)); // Coastal Chanticleers
        m.put("LMON",  colors(0x922247, 0xFFFFFF)); // Monroe Warhawks
        m.put("LALF",  colors(0xCE181E, 0xFFFFFF)); // Lafayette Ragin' Cajuns
        m.put("TROY",  colors(0x85714D, 0xBA0C2F)); // Troy Trojans
        m.put("TXST",  colors(0x501214, 0xFFFFFF)); // Texas State Bobcats
        m.put("SOGA",  colors(0x003366, 0xFFFFFF)); // South Georgia Eagles
        m.put("GAST",  colors(0x003DA5, 0xFFFFFF)); // Georgia State Panthers
        m.put("SAL",   colors(0x00205B, 0xCC0000)); // South Alabama Jaguars

        // Independent
        m.put("ARMY",  colors(0x000000, 0xCEB888)); // Army Black Knights
        m.put("PROV",  colors(0x002E62, 0xFFFFFF)); // Provo Cougars
        m.put("LYNC",  colors(0xCC0000, 0x000000)); // Lynchburg Flames
        m.put("NMST",  colors(0x8C1D40, 0x000000)); // New Mexico St Aggies
        m.put("SB",    colors(0x002B5C, 0xCEB888)); // South Bend Fighting Irish
        m.put("MASS",  colors(0x881C1C, 0xFFFFFF)); // Massachusetts Minutemen

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

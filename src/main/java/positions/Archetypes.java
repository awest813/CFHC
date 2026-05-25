package positions;

public final class Archetypes {

    private Archetypes() {}

    public static final String NONE = "";

    public static final String QB_POCKET = "POCKET_PASSER";
    public static final String QB_SCRAMBLER = "SCRAMBLER";
    public static final String QB_FIELD_GENERAL = "FIELD_GENERAL";
    public static final String QB_DUAL_THREAT = "DUAL_THREAT";

    public static final String RB_SPEED = "SPEED_BACK";
    public static final String RB_POWER = "POWER_BACK";
    public static final String RB_RECEIVING = "RECEIVING_BACK";

    public static final String WR_DEEP_THREAT = "DEEP_THREAT";
    public static final String WR_ROUTE_RUNNER = "ROUTE_RUNNER";
    public static final String WR_SLOT = "SLOT_RECEIVER";

    public static final String TE_BLOCKING = "BLOCKING_TE";
    public static final String TE_RECEIVING = "RECEIVING_TE";
    public static final String TE_HYBRID = "HYBRID_TE";

    public static final String OL_RUN_BLOCKER = "RUN_BLOCKER";
    public static final String OL_PASS_PROTECTOR = "PASS_PROTECTOR";
    public static final String OL_MAULER = "MAULER";

    public static final String DL_RUN_STOPPER = "DL_RUN_STOPPER";
    public static final String DL_PASS_RUSHER = "DL_PASS_RUSHER";
    public static final String DL_NOSE = "NOSE_TACKLE";

    public static final String LB_RUN_STOPPER = "LB_RUN_STOPPER";
    public static final String LB_COVERAGE = "COVERAGE_LB";
    public static final String LB_BLITZER = "BLITZER";

    public static final String CB_SHUTDOWN = "SHUTDOWN_CORNER";
    public static final String CB_SPEED = "SPEED_CB";
    public static final String CB_PHYSICAL = "PHYSICAL_CB";

    public static final String S_BALL_HAWK = "BALL_HAWK";
    public static final String S_RUN_SUPPORT = "RUN_SUPPORT";
    public static final String S_HYBRID = "HYBRID_S";

    public static final String K_POWER = "POWER_KICKER";
    public static final String K_ACCURATE = "ACCURATE_KICKER";

    private static final double[] BALANCED_MULT = {1.0, 1.0, 1.0, 1.0};
    private static final int[] MAX_CAPS = {99, 99, 99, 99};

    private static final double[][] QB_MULTS = {
        {1.2, 1.2, 0.8, 0.6},
        {0.8, 1.0, 1.3, 1.3},
        {1.0, 1.3, 1.0, 0.8},
        {1.0, 1.0, 1.1, 1.2}
    };
    private static final int[][] QB_CAPS = {
        {99, 99, 80, 75},
        {90, 95, 99, 99},
        {95, 99, 90, 80},
        {95, 95, 95, 95}
    };
    private static final String[] QB_TAGS = {QB_POCKET, QB_SCRAMBLER, QB_FIELD_GENERAL, QB_DUAL_THREAT};

    private static final double[][] RB_MULTS = {
        {1.3, 1.1, 0.7, 0.9},
        {0.8, 0.9, 1.3, 0.8},
        {0.9, 1.2, 0.8, 1.3}
    };
    private static final int[][] RB_CAPS = {
        {99, 95, 80, 85},
        {85, 85, 99, 80},
        {90, 95, 80, 99}
    };
    private static final String[] RB_TAGS = {RB_SPEED, RB_POWER, RB_RECEIVING};

    private static final double[][] WR_MULTS = {
        {1.3, 0.9, 0.9, 1.1},
        {0.9, 1.3, 1.1, 0.8},
        {0.9, 1.1, 1.3, 0.9}
    };
    private static final int[][] WR_CAPS = {
        {99, 90, 85, 95},
        {90, 99, 95, 85},
        {88, 95, 99, 88}
    };
    private static final String[] WR_TAGS = {WR_DEEP_THREAT, WR_ROUTE_RUNNER, WR_SLOT};

    private static final double[][] TE_MULTS = {
        {1.3, 0.8, 0.8, 0.8},
        {0.7, 1.3, 1.1, 1.0},
        {1.1, 1.1, 0.9, 0.9}
    };
    private static final int[][] TE_CAPS = {
        {99, 80, 75, 78},
        {80, 99, 90, 88},
        {92, 92, 85, 85}
    };
    private static final String[] TE_TAGS = {TE_BLOCKING, TE_RECEIVING, TE_HYBRID};

    private static final double[][] OL_MULTS = {
        {1.3, 0.8, 1.2, 0.8},
        {0.8, 1.3, 0.9, 1.1},
        {1.1, 0.9, 1.3, 0.8}
    };
    private static final int[][] OL_CAPS = {
        {99, 85, 99, 80},
        {85, 99, 90, 95},
        {95, 88, 99, 80}
    };
    private static final String[] OL_TAGS = {OL_RUN_BLOCKER, OL_PASS_PROTECTOR, OL_MAULER};

    private static final double[][] DL_MULTS = {
        {1.3, 1.1, 0.7, 1.2},
        {0.7, 0.9, 1.3, 1.0},
        {1.2, 0.8, 0.8, 1.3}
    };
    private static final int[][] DL_CAPS = {
        {99, 95, 80, 99},
        {80, 88, 99, 92},
        {99, 85, 82, 99}
    };
    private static final String[] DL_TAGS = {DL_RUN_STOPPER, DL_PASS_RUSHER, DL_NOSE};

    private static final double[][] LB_MULTS = {
        {1.3, 1.2, 0.7, 0.9},
        {0.9, 0.7, 1.3, 1.2},
        {0.9, 0.9, 0.9, 1.3}
    };
    private static final int[][] LB_CAPS = {
        {99, 99, 78, 85},
        {90, 80, 99, 95},
        {90, 88, 85, 99}
    };
    private static final String[] LB_TAGS = {LB_RUN_STOPPER, LB_COVERAGE, LB_BLITZER};

    private static final double[][] CB_MULTS = {
        {1.3, 1.1, 0.8, 1.1},
        {0.9, 1.3, 0.8, 1.0},
        {1.0, 0.8, 1.3, 0.9}
    };
    private static final int[][] CB_CAPS = {
        {99, 95, 80, 95},
        {90, 99, 80, 90},
        {95, 88, 99, 85}
    };
    private static final String[] CB_TAGS = {CB_SHUTDOWN, CB_SPEED, CB_PHYSICAL};

    private static final double[][] S_MULTS = {
        {0.9, 1.3, 1.1, 0.8},
        {1.3, 0.8, 0.9, 1.2},
        {1.1, 1.1, 1.0, 0.9}
    };
    private static final int[][] S_CAPS = {
        {90, 99, 95, 82},
        {99, 82, 88, 98},
        {95, 95, 92, 88}
    };
    private static final String[] S_TAGS = {S_BALL_HAWK, S_RUN_SUPPORT, S_HYBRID};

    private static final double[][] K_MULTS = {
        {1.3, 0.9, 0.9, 0.9},
        {0.9, 1.3, 1.1, 1.0}
    };
    private static final int[][] K_CAPS = {
        {99, 88, 85, 85},
        {90, 99, 95, 92}
    };
    private static final String[] K_TAGS = {K_POWER, K_ACCURATE};

    public static int[] getPrimaryAttributeIndices(String position, String archetypeTag) {
        double[] mults = getMultipliers(position, archetypeTag);
        if (mults == null || mults.length == 0) return new int[]{0};
        double max = 0;
        for (double m : mults) if (m > max) max = m;
        int count = 0;
        for (double m : mults) if (m == max) count++;
        int[] result = new int[count];
        int idx = 0;
        for (int i = 0; i < mults.length; i++) {
            if (mults[i] == max) result[idx++] = i;
        }
        return result;
    }

    public static double[] getMultipliers(String position, String archetypeTag) {
        double[][] mults = getMultTable(position);
        String[] tags = getTagTable(position);
        if (mults == null || tags == null || archetypeTag == null || archetypeTag.isEmpty()) {
            return BALANCED_MULT.clone();
        }
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].equals(archetypeTag)) {
                return mults[i].clone();
            }
        }
        return BALANCED_MULT.clone();
    }

    public static int[] getCaps(String position, String archetypeTag) {
        int[][] caps = getCapTable(position);
        String[] tags = getTagTable(position);
        if (caps == null || tags == null || archetypeTag == null || archetypeTag.isEmpty()) {
            return MAX_CAPS.clone();
        }
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].equals(archetypeTag)) {
                return caps[i].clone();
            }
        }
        return MAX_CAPS.clone();
    }

    public static String[] getArchetypesForPosition(String position) {
        String[] tags = getTagTable(position);
        return tags != null ? tags.clone() : new String[0];
    }

    public static String assignArchetype(String position, int a1, int a2, int a3, int a4) {
        switch (position) {
            case "QB": return assignQB(a1, a2, a3, a4);
            case "RB": return assignRB(a1, a2, a3, a4);
            case "WR": return assignWR(a1, a2, a3, a4);
            case "TE": return assignTE(a1, a2, a3, a4);
            case "OL": return assignOL(a1, a2, a3, a4);
            case "DL": return assignDL(a1, a2, a3, a4);
            case "LB": return assignLB(a1, a2, a3, a4);
            case "CB": return assignCB(a1, a2, a3, a4);
            case "S":  return assignS(a1, a2, a3, a4);
            case "K":  return assignK(a1, a2, a3, a4);
            default:   return NONE;
        }
    }

    public static String displayName(String tag) {
        if (tag == null || tag.isEmpty()) return "Balanced";
        return switch (tag) {
            case QB_POCKET -> "Pocket Passer";
            case QB_SCRAMBLER -> "Scrambler";
            case QB_FIELD_GENERAL -> "Field General";
            case QB_DUAL_THREAT -> "Dual-Threat";
            case RB_SPEED -> "Speed Back";
            case RB_POWER -> "Power Back";
            case RB_RECEIVING -> "Receiving Back";
            case WR_DEEP_THREAT -> "Deep Threat";
            case WR_ROUTE_RUNNER -> "Route Runner";
            case WR_SLOT -> "Slot Receiver";
            case TE_BLOCKING -> "Blocking TE";
            case TE_RECEIVING -> "Receiving TE";
            case TE_HYBRID -> "Hybrid TE";
            case OL_RUN_BLOCKER -> "Run Blocker";
            case OL_PASS_PROTECTOR -> "Pass Protector";
            case OL_MAULER -> "Mauler";
            case DL_RUN_STOPPER -> "Run Stopper";
            case DL_PASS_RUSHER -> "Pass Rusher";
            case DL_NOSE -> "Nose Tackle";
            case LB_RUN_STOPPER -> "Run Stopper";
            case LB_COVERAGE -> "Coverage LB";
            case LB_BLITZER -> "Blitzer";
            case CB_SHUTDOWN -> "Shutdown Corner";
            case CB_SPEED -> "Speed CB";
            case CB_PHYSICAL -> "Physical CB";
            case S_BALL_HAWK -> "Ball Hawk";
            case S_RUN_SUPPORT -> "Run Support";
            case S_HYBRID -> "Hybrid Safety";
            case K_POWER -> "Power Kicker";
            case K_ACCURATE -> "Accurate Kicker";
            default -> "Balanced";
        };
    }

    private static String assignQB(int passPow, int passAcc, int evasion, int speed) {
        if (evasion >= 75 && speed >= 80) {
            if (passPow >= 80 && passAcc >= 80) return QB_DUAL_THREAT;
            return QB_SCRAMBLER;
        }
        if (passAcc >= passPow && evasion < 70) return QB_FIELD_GENERAL;
        return QB_POCKET;
    }

    private static String assignRB(int speed, int evasion, int power, int catchRating) {
        if (catchRating >= speed && catchRating >= power) return RB_RECEIVING;
        if (power >= speed && power >= evasion) return RB_POWER;
        return RB_SPEED;
    }

    private static String assignWR(int speed, int catchRating, int evasion, int jump) {
        if (speed >= catchRating && speed >= evasion) return WR_DEEP_THREAT;
        if (catchRating >= speed && evasion >= jump) return WR_ROUTE_RUNNER;
        return WR_SLOT;
    }

    private static String assignTE(int runBlock, int catchRating, int evasion, int speed) {
        int offense = catchRating + evasion + speed;
        if (runBlock >= offense / 2) return TE_BLOCKING;
        if (catchRating >= runBlock) return TE_RECEIVING;
        return TE_HYBRID;
    }

    private static String assignOL(int runBlock, int passBlock, int strength, int vision) {
        if (runBlock >= passBlock && strength >= vision) return OL_MAULER;
        if (runBlock >= passBlock) return OL_RUN_BLOCKER;
        return OL_PASS_PROTECTOR;
    }

    private static String assignDL(int runStop, int tackle, int passRush, int strength) {
        if (passRush >= runStop && passRush >= tackle) return DL_PASS_RUSHER;
        if (strength >= passRush && strength >= runStop) return DL_NOSE;
        return DL_RUN_STOPPER;
    }

    private static String assignLB(int tackle, int runStop, int coverage, int speed) {
        if (coverage >= tackle && speed >= runStop) return LB_COVERAGE;
        if (speed >= tackle && speed >= runStop) return LB_BLITZER;
        return LB_RUN_STOPPER;
    }

    private static String assignCB(int coverage, int speed, int tackle, int jump) {
        if (tackle >= coverage && tackle >= speed) return CB_PHYSICAL;
        if (speed >= coverage) return CB_SPEED;
        return CB_SHUTDOWN;
    }

    private static String assignS(int tackle, int coverage, int speed, int runStop) {
        if (coverage >= tackle && coverage >= runStop) return S_BALL_HAWK;
        if (tackle >= coverage && runStop >= speed) return S_RUN_SUPPORT;
        return S_HYBRID;
    }

    private static String assignK(int kickPow, int kickAcc, int pressure, int form) {
        if (kickPow >= kickAcc) return K_POWER;
        return K_ACCURATE;
    }

    private static double[][] getMultTable(String position) {
        return switch (position) {
            case "QB" -> QB_MULTS;
            case "RB" -> RB_MULTS;
            case "WR" -> WR_MULTS;
            case "TE" -> TE_MULTS;
            case "OL" -> OL_MULTS;
            case "DL" -> DL_MULTS;
            case "LB" -> LB_MULTS;
            case "CB" -> CB_MULTS;
            case "S"  -> S_MULTS;
            case "K"  -> K_MULTS;
            default -> null;
        };
    }

    private static int[][] getCapTable(String position) {
        return switch (position) {
            case "QB" -> QB_CAPS;
            case "RB" -> RB_CAPS;
            case "WR" -> WR_CAPS;
            case "TE" -> TE_CAPS;
            case "OL" -> OL_CAPS;
            case "DL" -> DL_CAPS;
            case "LB" -> LB_CAPS;
            case "CB" -> CB_CAPS;
            case "S"  -> S_CAPS;
            case "K"  -> K_CAPS;
            default -> null;
        };
    }

    private static String[] getTagTable(String position) {
        return switch (position) {
            case "QB" -> QB_TAGS;
            case "RB" -> RB_TAGS;
            case "WR" -> WR_TAGS;
            case "TE" -> TE_TAGS;
            case "OL" -> OL_TAGS;
            case "DL" -> DL_TAGS;
            case "LB" -> LB_TAGS;
            case "CB" -> CB_TAGS;
            case "S"  -> S_TAGS;
            case "K"  -> K_TAGS;
            default -> null;
        };
    }
}

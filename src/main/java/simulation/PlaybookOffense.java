package simulation;

public class PlaybookOffense {

    public Team team;
    private int runPref;
    private int runProtection;
    private int runPotential;
    private int runUsage;
    private int passPref;
    private int passProtection;
    private int passPotential;
    private int passUsage;
    public final int numPlaybooks = 6;


    private String stratName;
    private String stratDescription;

    public PlaybookOffense(String name, String descrip, int rPref, int rProtection, int rPotential, int rUsage, int pPref, int pProtection, int pPotential, int pUsage) {
        stratName = name;
        stratDescription = descrip;
        runPref = rPref;  //Run Frequency  Based on runPref / (runPref+passPref)
        runProtection = rProtection; //Block Bonus
        runPotential = rPotential; //RB Hole opening bonus
        runUsage = rUsage; //Use TE to Block
        passPref = pPref;  //Pass Frequency
        passProtection = pProtection; //Block Bonus + accuracy
        passPotential = pPotential; //Big Play Potential
        passUsage = pUsage; //use TE more often in passing
    }

    public PlaybookOffense(int playbook) {
        if (playbook < 1 || playbook > 6) playbook = (int) (Math.random() * 6) + 1;

        if (playbook == 1) playBook1();
        else if (playbook == 2) playBook2();
        else if (playbook == 3) playBook3();
        else if (playbook == 4) playBook4();
        else if (playbook == 5) playBook5();
        else if (playbook == 6) playBook6();
        else playBook1();
    }

    public void playBook1() {
        stratName = "Multiple Pro";
        stratDescription = "Play a balanced, multiple offense that can shift between under-center looks, shotgun, run game, and play-action.";
        runPref = 1;
        runProtection = 0;
        runPotential = 0;
        runUsage = 1;
        passPref = 1;
        passProtection = 0;
        passPotential = 0;
        passUsage = 1;
    }

    public void playBook2() {
        stratName = "Power Spread";
        stratDescription = "Play a run-first spread built around physical blocking, tempo control, and play-action shots.";
        runPref = 2;
        runProtection = 1;
        runPotential = -1;
        runUsage = 1;
        passPref = 1;
        passProtection = 2;
        passPotential = 1;
        passUsage = 0;
    }

    public void playBook3() {
        stratName = "Quick Game";
        stratDescription = "Use high-percentage throws, spacing concepts, and timing routes to stay on schedule.";
        runPref = 2;
        runProtection = 0;
        runPotential = 1;
        runUsage = 0;
        passPref = 3;
        passProtection = 1;
        passPotential = -2;
        passUsage = 2;
    }

    public void playBook4() {
        stratName = "Air Raid";
        stratDescription = "Play a pass-heavy spread with wide formations and explosive potential, accepting protection risk.";
        runPref = 1;
        runProtection = -2;
        runPotential = 1;
        runUsage = 0;
        passPref = 2;
        passProtection = -2;
        passPotential = 1;
        passUsage = 1;
    }

    public void playBook5() {
        stratName = "Zone Read";
        stratDescription = "Feature QB run reads and option looks that stress linebackers and create rushing lanes.";
        runPref = 3;
        runProtection = -1;
        runPotential = 1;
        runUsage = 1;
        passPref = 2;
        passProtection = -1;
        passPotential = -1;
        passUsage = 0;
    }

    public void playBook6() {
        stratName = "Spread RPO";
        stratDescription = "Blend option runs with packaged throws, leaning pass-first while still punishing light boxes.";
        runPref = 2;
        runProtection = -1;
        runPotential = 1;
        runUsage = 1;
        passPref = 3;
        passProtection = -1;
        passPotential = -1;
        passUsage = 1;
    }


    public String getStratName() {
        return stratName;
    }

    public String getStratDescription() {
        return stratDescription;
    }

    public int getRunPref() {
        return runPref;
    }

    public int getRunProtection() {
        return runProtection;
    }

    public int getRunPotential() {
        return runPotential;
    }

    public int getRunUsage() {
        return runUsage;
    }

    public int getPassPref() {
        return passPref;
    }

    public int getPassProtection() {
        return passProtection;
    }

    public int getPassPotential() {
        return passPotential;
    }

    public int getPassUsage() {
        return passUsage;
    }

}

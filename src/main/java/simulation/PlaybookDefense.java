package simulation;

public class PlaybookDefense {
    public Team team;
    private int runPref;
    private int runStop;
    private int runCoverage;
    private int runSpy;
    private int passPref;
    private int passRush;
    private int passCoverage;
    private int passSpy;
    public final int numPlaybooks = 5;

    private String stratName;
    private String stratDescription;

    public PlaybookDefense(String name, String descrip, int rPref, int rProtection, int rPotential, int rUsage, int pPref, int pProtection, int pPotential, int pUsage) {
        stratName = name;
        stratDescription = descrip;
        runPref = rPref;
        runStop = rProtection; //Run Stop at Line Bonus
        runCoverage = rPotential; //Big Run Stop Bonus
        runSpy = rUsage; //Use LB/S to man cover RB
        passPref = pPref;
        passRush = pProtection; //Pass Rush Bonus
        passCoverage = pPotential; //Cover Deep
        passSpy = pUsage; //Use LB/S to Blitz
    }

    public PlaybookDefense(int playbook) {
        if (playbook < 1 || playbook > 5) playbook = (int) (Math.random() * 5) + 1;

        if (playbook == 1) playBook1();
        else if (playbook == 2) playBook2();
        else if (playbook == 3) playBook3();
        else if (playbook == 4) playBook4();
        else if (playbook == 5) playBook5();
        else playBook1();
    }

    public void playBook1() {
        stratName = "Multiple 4-2-5";
        stratDescription = "Play a balanced modern nickel defense that can fit the run without sacrificing coverage bodies.";
        runPref = 1;
        runStop = 0;
        runCoverage = 0;
        runSpy = 1;
        passPref = 1;
        passRush = 0;
        passCoverage = 0;
        passSpy = 1;
    }

    public void playBook2() {
        stratName = "Bear Front";
        stratDescription = "Load the box with a heavy front to squeeze the run game. Strong against rushing attacks, but vulnerable to explosive passes.";
        runPref = 2;
        runStop = 1;
        runCoverage = 2;
        runSpy = 1;
        passPref = 1;
        passRush = -1;
        passCoverage = -2;
        passSpy = 0;
    }

    public void playBook3() {
        stratName = "Zero Pressure";
        stratDescription = "Bring aggressive man pressure with no deep safety help. Creates disruption, but one missed coverage can turn into a big play.";
        runPref = 1;
        runStop = 0;
        runCoverage = 1;
        runSpy = 1;
        passPref = 1;
        passRush = 1;
        passCoverage = -1;
        passSpy = 1;
    }

    public void playBook4() {
        stratName = "Tampa 2";
        stratDescription = "Play a two-high zone shell with linebacker help underneath. Protects against quick passing, but can soften the run front.";
        runPref = 2;
        runStop = -1;
        runCoverage = 0;
        runSpy = 1;
        passPref = 3;
        passRush = 1;
        passCoverage = 1;
        passSpy = 0;
    }

    public void playBook5() {
        stratName = "Cover 3 Match";
        stratDescription = "Play a match-zone structure built to limit deep shots while conceding some space underneath and on the ground.";
        runPref = 3;
        runStop = -1;
        runCoverage = -2;
        runSpy = 1;
        passPref = 7;
        passRush = -1;
        passCoverage = 2;
        passSpy = 1;
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

    public int getRunStop() {
        return runStop;
    }

    public int getRunCoverage() {
        return runCoverage;
    }

    public int getRunSpy() {
        return runSpy;
    }

    public int getPassPref() {
        return passPref;
    }

    public int getPassRush() {
        return passRush;
    }

    public int getPassCoverage() {
        return passCoverage;
    }

    public int getPassSpy() {
        return passSpy;
    }

}

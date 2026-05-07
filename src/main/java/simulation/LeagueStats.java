package simulation;

import java.util.List;

public final class LeagueStats {

    private LeagueStats() {
    }

    public static int getAverageYards(List<Team> teamList) {
        int average = 0;
        for (int i = 0; i < teamList.size(); ++i) {
            average += teamList.get(i).getTeamYards();
        }
        return average / teamList.size();
    }

    public static int getAverageOffTalent(List<Team> teamList) {
        int average = 0;
        for (int i = 0; i < teamList.size(); ++i) {
            average += teamList.get(i).getOffTalent();
        }
        return average / teamList.size();
    }

    public static int getAverageDefTalent(List<Team> teamList) {
        int average = 0;
        for (int i = 0; i < teamList.size(); ++i) {
            average += teamList.get(i).getDefTalent();
        }
        return average / teamList.size();
    }

    public static int getAverageConfPrestige(List<Conference> conferences) {
        int avgPrestige = 0;
        int countC = 0;
        for (int i = 0; i < conferences.size(); ++i) {
            conferences.get(i).updateConfPrestige();
        }
        for (int i = 0; i < conferences.size(); ++i) {
            if (conferences.get(i).confTeams.size() > conferences.get(i).minConfTeams) {
                avgPrestige += conferences.get(i).confPrestige;
                countC++;
            }
        }
        if (countC > 0) return avgPrestige / countC;
        else return 0;
    }

    public static double getAverageTeamChemistry(List<Team> teamList) {
        double avg = 0;
        for (int i = 0; i < teamList.size(); ++i) {
            avg += teamList.get(i).getTeamChemistry();
        }
        return avg / teamList.size();
    }

    public static int getAvgCoachTal(List<Team> teamList, Team userTeam) {
        int avg = 0;
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i) != userTeam && teamList.get(i).getHeadCoach() != null)
                avg += teamList.get(i).getHeadCoach().ratTalent;
        }
        return avg / (teamList.size() - 1);
    }

    public static int getAvgCoachDis(List<Team> teamList, Team userTeam) {
        int avg = 0;
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i) != userTeam && teamList.get(i).getHeadCoach() != null)
                avg += teamList.get(i).getHeadCoach().ratDiscipline;
        }
        return avg / (teamList.size() - 1);
    }

    public static int getAvgCoachOff(List<Team> teamList, Team userTeam) {
        int avg = 0;
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i) != userTeam && teamList.get(i).getHeadCoach() != null)
                avg += teamList.get(i).getHeadCoach().ratOff;
        }
        return avg / (teamList.size() - 1);
    }

    public static int getAvgCoachDef(List<Team> teamList, Team userTeam) {
        int avg = 0;
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i) != userTeam && teamList.get(i).getHeadCoach() != null)
                avg += teamList.get(i).getHeadCoach().ratDef;
        }
        return avg / (teamList.size() - 1);
    }
}

package simulation;

import java.util.Random;

import staff.HeadCoach;
import staff.Staff;

public class TeamFinance {

    private final Team team;

    TeamFinance(Team team) {
        this.team = team;
    }

    public double getHomeGameRevenueMultiplier() {
        double m = 1.0 + team.NIL_HOME_REV_PER_TIER * team.nilCollectiveLevel;
        if (team.HC != null) {
            m += 0.01 * CoachSkills.getRank(team.HC.coachSkillRanksBits, CoachSkills.NIL_MARKETING);
        }
        return m;
    }

    public int getWeeklyCollectiveStipend() {
        int base = team.nilCollectiveLevel * team.NIL_WEEKLY_STIPEND_PER_TIER;
        if (team.HC != null) {
            base += team.HC.nilMarketingWeeklyStipend();
        }
        return base;
    }

    public void stabilizeDisciplineFromCoachSkills() {
        if (!team.userControlled || team.HC == null) {
            return;
        }
        int b = team.HC.disciplineCultureBonus();
        if (b > 0) {
            team.teamDisciplineScore = Math.min(100, team.teamDisciplineScore + b);
        }
    }

    private void refreshConfPrestige() {
        for (int i = 0; i < team.league.getConferences().size(); ++i) {
            team.league.getConferences().get(i).updateConfPrestige();
        }
    }

    private int applyConfBias(float level) {
        float confBias = team.confPrestige - team.league.getAverageConfPrestige();
        if (level < 4 && confBias < 0) level = 4;
        if (level > 6 && confBias < 0) level = 6;
        if (level < 7 && confBias > 0) level = 7;
        return Math.round(level);
    }

    public int getRecruitLevel() {
        refreshConfPrestige();
        float level = (team.league.getTeamList().size() - team.rankTeamPrestige) / (float)(team.league.getTeamList().size()/10.5);
        return applyConfBias(level);
    }

    public int getUserRecruitBudget() {
        refreshConfPrestige();
        float level = (team.league.getTeamList().size() - team.rankTeamPrestige) / (float)(team.league.getTeamList().size()/10);
        return (int) Math.round(applyConfBias(level) * 8.5);
    }

    public int getUserRecruitStars() {
        refreshConfPrestige();
        float level = (team.league.getTeamList().size() - team.rankTeamPrestige) / (float)(team.league.getTeamList().size()/10.5);
        return applyConfBias(level);
    }

    public int nudgeCpuFreshmanStarRating(int stars, int recruitChance) {
        if (recruitChance * Math.random() > Math.random() * 50) {
            stars += Math.random() * (team.maxStarRating - stars);
        } else {
            stars -= Math.random() * (stars);
        }
        return stars;
    }

    public void checkFacilitiesUpgradeBonus() {
        if(team.facilityUpgrade) {
            team.teamPrestige += team.teamFacilities;
            if(team.HC != null) team.HC.baselinePrestige += team.teamFacilities*.5;
        }
    }

    public int getMinCoachHireReq() {
        int req = (team.league.getTeamList().size() - team.rankTeamPrestige) / 2 + (int)Math.round(team.league.getTeamList().size()/3.6);
        if (req >= 87) req = 87;
        return req;
    }

    public void coachContracts(int totalPDiff) {
        int max = 78;
        int min = 60;
        Random rand = new Random();
        int retire = rand.nextInt((max - min) + 1) + min;
        int age = team.HC.age;
        int wins = team.HC.getWins();
        int losses = team.HC.getLosses();
        boolean proveIt = false;

        //RETIREMENT
        if (team.HC.age > retire && !team.userControlled) {
            team.retired = true;
            team.HC.retired = true;
            if(team.HC.getCumulativePrestige() >= 25) team.teamPrestige = (int)(team.teamPrestige*team.knockdownRet);
            else team.teamPrestige = (int)(team.teamPrestige*team.knockdownFired);
            team.league.addCoachFreeAgent(new HeadCoach(team.HC, team));
            String oldCoach = team.HC.name;
            team.fired = true;
            team.newCoachTeamChanges();
            team.league.addNewsStory(team.league.currentWeek + 1,team.name + " Coaching Retirement>" + oldCoach + " has announced his retirement at the age of " + age +
                    ". His former team, " + team.name + " have not announced a new successor to replace the retired coach. Head Coach " + oldCoach + " had a career record of " + wins + "-" + losses + ".");
            team.league.addNewsHeadline(team.name + " coach " + oldCoach + " has announced his retirement at age " + age + ".");
            team.HC = null;
        }

        if (!team.retired) {
            if (!team.userControlled && ((team.teamPrestige > (team.HC.baselinePrestige + 9) && team.rankTeamPrestige > (int) (team.league.countTeam * 0.35) && team.HC.age < 50) || (team.teamPrestige > (team.HC.baselinePrestige + 12) && team.confPrestige < team.league.confAvg && team.rankTeamPrestige < (int) (team.league.countTeam * 0.20) && team.HC.age < 48))) {
                team.league.addNewsStory(team.league.currentWeek + 1,"Head Coach Rumor Mill>After another successful season at " + team.name + ", " + age + " year old head coach " + team.HC.name + " has moved to the top of" +
                        " many of the schools looking for a replacement at that position. He has a career record of " + wins + "-" + losses + ". ");
                team.league.addNewsHeadline(team.name + " " + team.HC.position + " " + team.HC.name + " rumored for a bigger program?");
                if (Math.random() > 0.50) {
                    team.league.getCoachStarList().add(team.HC);
                }
            }
            //New Contracts or Firing
            if ((team.HC.contractYear) >= team.HC.contractLength || team.natChampWL.equals("NCW") || team.natChampWL.equals("NCL") || (team.HC.contractYear + 1 == team.HC.contractLength && Math.random() < 0.38) || (team.HC.contractYear + 2 == team.HC.contractLength && Math.random() < 0.23)) {
                if (totalPDiff > 15 || (team.natChampWL.equals("NCW"))) {
                    team.HC.contractLength = 7;
                    team.HC.contractYear = 0;
                    team.HC.baselinePrestige = (team.HC.baselinePrestige + 2 * team.teamPrestige) / 3;
                    team.newContract = true;
                    team.league.addNewsStory(team.league.currentWeek + 1,"Long-Term Extension!>" + team.name + " has extended their head coach, " + team.HC.name +
                            " for 7 additional seasons for his successful tenure at the university.");
                    team.league.addNewsHeadline(team.name + " has extended their head coach, " + team.HC.name + " for 7 additional seasons");
                } else if (totalPDiff > 10) {
                    team.HC.contractLength = 5;
                    team.HC.contractYear = 0;
                    team.HC.baselinePrestige = (team.HC.baselinePrestige + 2 * team.teamPrestige) / 3;
                    team.newContract = true;
                    team.league.addNewsStory(team.league.currentWeek + 1,"New 5-Year Contract Awarded!>" + team.name + " has extended their head coach, " + team.HC.name +
                            " for 5 additional seasons for his successful tenure at the university.");
                    team.league.addNewsHeadline(team.name + " has extended their head coach, " + team.HC.name + " for 5 additional seasons");
                } else if (totalPDiff > 7) {
                    team.HC.contractLength = 4;
                    team.HC.contractYear = 0;
                    team.HC.baselinePrestige = (team.HC.baselinePrestige + 2 * team.teamPrestige) / 3;
                    team.newContract = true;
                } else if (totalPDiff > 5 || (team.natChampWL.equals("NCL"))) {
                    if (!(team.natChampWL.equals("NCL") && team.HC.contractLength - team.HC.contractYear > 2)) {
                        team.HC.contractLength = 3;
                        team.HC.contractYear = 0;
                        team.HC.baselinePrestige = (team.HC.baselinePrestige + 2 * team.teamPrestige) / 3;
                        team.newContract = true;
                    }
                } else if (totalPDiff < 0 && (team.teamPrestige - team.teamPrestigeStart) > 2 || team.rankTeamPrestige > 15 && (team.teamPrestige - team.teamPrestigeStart) > 2) {
                    if (Math.random() > 0.40) {
                        team.HC.contractLength = 2;
                        team.HC.contractYear = 0;
                        team.league.addNewsStory(team.league.currentWeek + 1,"2-Year Prove-It Contract Given by " + team.name + ">" + team.name + " has an additional 2-year contract to " + team.HC.name +
                                " despite a disappointing tenure. He has a career record of " + wins + "-" + losses + ", however the recent success of the team this season has inspired some confidence with the head coach from the AD.");
                        team.newContract = true;
                        proveIt = true;
                    } else {
                        team.fired = true;
                        team.league.addNewsStory(team.league.currentWeek + 1,"Polarizing Head Coach Firing at " + team.name + ">" + team.strRankTeamRecord() + " has fired their head coach, " + team.HC.name +
                                " despite finally getting the team on the right track. The team struggled during his first few seasons at the school, but had shown some promise this season." +
                                " He has a career record of " + wins + "-" + losses + ".  The team is now searching for a new head coach.");
                        team.league.addNewsHeadline(team.name + " has fired Head Coach " + team.HC.name + ".");
                        team.newCoachTeamChanges();
                        if (!team.userControlled) {
                            team.league.addCoach(new HeadCoach(team.HC, team));
                            team.HC = null;
                        }
                    }
                } else if (!team.userControlled && (((!team.league.isCareerMode()) && totalPDiff < -2 && team.rankTeamPollScore > 15) || (team.rankTeamPollScore > 25 && totalPDiff < -1))) {
                    team.fired = true;
                    team.league.addNewsStory(team.league.currentWeek + 1,"Head Coach Firing at " + team.name + ">" + team.strRankTeamRecord() + " has fired their head coach, " + team.HC.name +
                            " after a disappointing tenure. He has a career record of " + wins + "-" + losses + ". The team is now searching for a new head coach.");
                    team.league.addNewsHeadline(team.name + " has fired Head Coach " + team.HC.name + ".");
                    team.newCoachTeamChanges();
                    team.league.addCoach(new HeadCoach(team.HC, team));
                    team.HC = null;
                } else if ((team.league.isCareerMode() && totalPDiff < -2 && team.rankTeamPollScore > 15) || (team.rankTeamPollScore > 25 && totalPDiff < -1)) {
                    team.fired = true;
                    team.league.addNewsStory(team.league.currentWeek + 1,"Head Coach Firing at " + team.name + ">" + team.strRankTeamRecord() + " has fired their head coach, " + team.HC.name +
                            " after a disappointing tenure. He has a career record of " + wins + "-" + losses + ".  The team is now searching for a new head coach.");
                    team.league.addNewsHeadline(team.name + " has fired Head Coach " + team.HC.name + ".");
                    team.newCoachTeamChanges();
                    if (!team.userControlled) {
                        team.league.addCoach(new HeadCoach(team.HC, team));
                        team.HC = null;
                    }

                } else {
                    team.HC.contractLength = 2;
                    team.HC.contractYear = 0;
                    team.HC.baselinePrestige = (3 * team.HC.baselinePrestige + team.teamPrestige) / 4;
                    team.newContract = true;
                }
            }
        }
        if (team.userControlled) {
            if (team.newContract && proveIt)
                team.contractString = "You have been given an additional " + team.HC.contractLength + "-year prove-it contract based on your team's recent momentum despite the uneven start to your tenure.";
            else if (team.newContract) {
                team.contractString = "Congratulations. You have been awarded a new " + team.HC.contractLength + "-year contract extension after this season's progress.";
            } else if (team.fired) {
                team.contractString = "Due to your performance as head coach, the Athletic Director has terminated your contract and you are no longer the head coach of this school.";
            } else {
                team.contractString = "You have " + (team.HC.contractLength - team.HC.contractYear)
                        + " years left on your contract. Current prestige: " + team.teamPrestige + ". Baseline prestige: " + team.HC.baselinePrestige +
                        ". Current status: " + team.HC.coachStatus() + ".";
            }
        }
    }

    public void coordinatorContracts(Staff coord) {
        final String pos = coord.position;
        int cpres = coord.baselinePrestige;
        int max = 78;
        int min = 60;
        Random rand = new Random();
        int retire = rand.nextInt((max - min) + 1) + min;
        int age = coord.age;

        //RETIREMENT
        if (coord.age > retire) {
            coord.retired = true;
            if(coord.getWins() > 0) team.league.addCoachFreeAgent(new HeadCoach(coord, team));
            String oldCoach = coord.name;
            team.league.addNewsStory(team.league.currentWeek + 1,team.name + " Coordinator Retirement>" + coord.position + " " + oldCoach + " has announced his retirement at the age of " + age +
                    ". His former team, " + team.name + " have not announced a new successor to replace the retired coordinator.");
            team.league.addNewsHeadline(team.name + " " + coord.position + " " + oldCoach + " has announced his retirement at age " + age + ".");
            if(pos.equals("OC")) team.OC = null;
            if(pos.equals("DC")) team.DC = null;
        } else if (!coord.retired) {
            int[] ovr = {1,1,1,1};

            if (coord.getStaffOverall(ovr) >= 75 || coord.baselinePrestige >= 5 || coord.baselinePrestige >= 2 && coord.getCumulativeCoord() >= 10) {
                if (Math.random() > 0.50) {
                    team.league.getCoachStarList().add(coord);
                    if(coord.getStaffOverall(ovr) >= 80) {
                        team.league.addNewsStory(team.league.currentWeek + 1,"Coordinator Advancement Rumor>After another successful season at " + team.name + ", " + age + " year " + coord.position + " " + coord.name + " has sparked interest at many of the schools looking for a replacement at Head Coach. He has a career record of " + team.wins + "-" + team.losses + ". ");
                        team.league.addNewsHeadline(team.name + " " + coord.position + " " + coord.name + " heading for a possible HC job?");
                    }
                }
            }

            if(team.userControlled) {
                //SKIP TO DIALOG

            } else {
                //New Contracts or Firing
                if (coord.contractYear >= coord.contractLength || (coord.contractYear + 1 == coord.contractLength && Math.random() < 0.38) || (coord.contractYear + 2 == coord.contractLength && Math.random() < 0.23)) {
                    if (cpres > 5) {
                        coord.contractLength = 4;
                        coord.contractYear = 0;
                        coord.baselinePrestige = 0;
                    } else if (cpres > 3) {
                        coord.contractLength = 3;
                        coord.contractYear = 0;
                        coord.baselinePrestige = 0;
                    } else if (cpres < 0) {
                        if (Math.random() > 0.50) {
                            coord.contractLength = 1;
                            coord.contractYear = 0;
                            coord.baselinePrestige = 0;
                        } else {
                            team.league.addNewsStory(team.league.currentWeek + 1,"Coordinator Firing at " + team.name + ">" + team.strRankTeamRecord() + " has fired their " + coord.position + ", " + coord.name +
                                    " after a disappointing tenure.");
                            team.league.addNewsHeadline(team.name + " has fired " + coord.position + " " + coord.name + ".");
                            team.league.addCoach(new HeadCoach(coord, team));
                            if(pos.equals("OC")) team.OC = null;
                            else if(pos.equals("DC")) team.DC = null;
                        }
                    } else if (cpres < -2 && team.rankTeamPollScore > 15 || team.rankTeamPollScore > 25 && cpres < -1) {
                        team.league.addNewsStory(team.league.currentWeek + 1,"Coordinator Firing at " + team.name + ">" + team.strRankTeamRecord() + " has fired their " + coord.position + ", " + coord.name +
                                " after a disappointing tenure.");
                        team.league.addNewsHeadline(team.name + " has fired " + coord.position + " " + coord.name + ".");
                        team.league.addCoach(new HeadCoach(coord, team));
                        if(pos.equals("OC")) team.OC = null;
                        else if(pos.equals("DC")) team.DC = null;

                    } else {
                        coord.contractLength = 2;
                        coord.contractYear = 0;
                        coord.baselinePrestige = 0;
                    }
                }
            }
        }

    }
}

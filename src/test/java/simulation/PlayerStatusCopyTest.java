package simulation;

import org.junit.Test;
import positions.Player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlayerStatusCopyTest {

    @Test
    public void weeksLabel_singularAndPlural() {
        assertEquals("1 wk", PlayerStatusCopy.weeksLabel(1));
        assertEquals("0 wks", PlayerStatusCopy.weeksLabel(0));
        assertEquals("3 wks", PlayerStatusCopy.weeksLabel(3));
    }

    @Test
    public void profileStatus_priorityOrder() {
        Player p = new Player();
        assertEquals("Active", PlayerStatusCopy.profileStatus(p));

        p.isInjured = true;
        assertEquals("Injured", PlayerStatusCopy.profileStatus(p));

        p.isMedicalRS = true;
        assertEquals("Medical Redshirt", PlayerStatusCopy.profileStatus(p));

        p.isRedshirt = true;
        assertEquals("Redshirt", PlayerStatusCopy.profileStatus(p));

        p.isTransfer = true;
        assertEquals("Transfer", PlayerStatusCopy.profileStatus(p));

        p.isSuspended = true;
        // Transfer still wins over suspension in profileStatus priority
        assertEquals("Transfer", PlayerStatusCopy.profileStatus(p));
    }

    @Test
    public void rosterTag_keepsUiColorSubstrings() {
        Player injured = new Player();
        injured.isInjured = true;
        injured.injury = new Injury(2, "Knee", injured);
        String injTag = PlayerStatusCopy.rosterTag(injured);
        assertTrue(injTag.contains("INJ"));
        assertTrue(injTag.contains("2 wks"));

        Player rs = new Player();
        rs.isRedshirt = true;
        assertTrue(PlayerStatusCopy.rosterTag(rs).contains("RS"));

        Player transfer = new Player();
        transfer.isTransfer = true;
        assertTrue(PlayerStatusCopy.rosterTag(transfer).contains("[T]"));

        Player suspended = new Player();
        suspended.isSuspended = true;
        suspended.weeksSuspended = 1;
        String sus = PlayerStatusCopy.rosterTag(suspended);
        assertTrue(sus.contains("Suspended"));
        assertTrue(sus.contains("1 wk"));
    }

    @Test
    public void injuryDetail_usesSharedWeeksLabel() {
        Player p = new Player();
        Injury one = new Injury(1, "Ankle", p);
        assertEquals("Ankle (1 wk)", PlayerStatusCopy.injuryDetail(one, false));
        assertEquals("Ankle (1 wk) Med RS", PlayerStatusCopy.injuryDetail(one, true));
        assertEquals("Ankle (1 wk)", one.toString());
    }
}

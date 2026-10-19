package simulation;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransferSummaryLineTest {

    @Test
    public void userBlock_splitsOutgoingAndIncoming() {
        String text = "Transfers Out:\n\n"
                + "QB Ada Lovelace, Fr  Ovr: 88 (State)\n\n"
                + "Transfers In:\n\n"
                + "WR Grace Hopper Jr Ovr: 91 (Pine Valley)\n";
        List<TransferSummaryLine> rows = TransferSummaryLine.parse(text);
        assertEquals(4, rows.size());
        assertTrue(rows.get(0).section);
        assertEquals("Transfers Out", rows.get(0).sectionTitle);
        assertEquals("QB", rows.get(1).position);
        assertEquals("Ada Lovelace", rows.get(1).name);
        assertEquals("Fr", rows.get(1).year);
        assertEquals("88", rows.get(1).overall);
        assertEquals("State", rows.get(1).destination);
        assertTrue(rows.get(2).section);
        assertEquals("WR", rows.get(3).position);
        assertEquals("Grace Hopper", rows.get(3).name);
        assertEquals("Jr", rows.get(3).year);
        assertEquals("Pine Valley", rows.get(3).destination);
        assertEquals(2, TransferSummaryLine.playerCount(rows));
    }

    @Test
    public void leagueLine_keepsDestinationAndFcs() {
        TransferSummaryLine placed = TransferSummaryLine.parse(
                "88 QB Jane Doe [transfer] Your Team (ST)").get(0);
        assertFalse(placed.section);
        assertEquals("88", placed.overall);
        assertEquals("QB", placed.position);
        assertEquals("Jane Doe", placed.name);
        assertEquals("Your Team (ST)", placed.destination);

        TransferSummaryLine fcs = TransferSummaryLine.parse("76 RB Sam Hill (FCS)").get(0);
        assertEquals("Sam Hill", fcs.name);
        assertEquals("FCS", fcs.destination);
    }
}

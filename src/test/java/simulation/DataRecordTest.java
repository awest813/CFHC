package simulation;

import org.junit.Test;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static org.junit.Assert.*;

public class DataRecordTest {

    @Test
    public void fromCsv_rejectsShortInput() {
        assertNull(DataRecord.fromCsv("only,three,fields"));
        assertNull(DataRecord.fromCsv("a,b"));
        assertNull(DataRecord.fromCsv(""));
        assertNull(DataRecord.fromCsv("one"));
    }

    @Test
    public void fromCsv_parsesValidInput() {
        DataRecord dr = DataRecord.fromCsv("Pass Yards,250.5,John Smith,2024");
        assertNotNull(dr);
        assertEquals("Pass Yards", dr.key());
        assertEquals(250.5f, dr.value(), 0.001f);
        assertEquals("John Smith", dr.holder());
        assertEquals(2024, dr.year());
    }

    @Test
    public void toCsv_roundTrips() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#.##", symbols);
        DataRecord original = new DataRecord("Rush Yards", 1200.0f, "Johnson%TST", 2023);
        String csv = original.toCsv(df);
        DataRecord parsed = DataRecord.fromCsv(csv);
        assertNotNull(parsed);
        assertEquals(original.key(), parsed.key());
        assertEquals(original.value(), parsed.value(), 0.01f);
        assertEquals(original.holder(), parsed.holder());
        assertEquals(original.year(), parsed.year());
    }

    @Test
    public void toCsv_roundTripsFloat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#.##", symbols);
        DataRecord original = new DataRecord("Comp Percent", 65.7f, "Smith%TST", 2024);
        String csv = original.toCsv(df);
        DataRecord parsed = DataRecord.fromCsv(csv);
        assertNotNull(parsed);
        assertEquals(original.key(), parsed.key());
        assertEquals(original.value(), parsed.value(), 0.01f);
        assertEquals(original.holder(), parsed.holder());
        assertEquals(original.year(), parsed.year());
    }

    @Test
    public void fromCsv_handlesCommaInHolder() {
        // split(",", 4) keeps at most 4 parts; holder text with a comma
        // pushes leftover text into parts[3] alongside the year,
        // so parseInt fails. This test documents that limitation.
        String csvWithCommaInHolder = "Key,100,Holder, Name,2025";
        try {
            DataRecord.fromCsv(csvWithCommaInHolder);
            fail("Expected NumberFormatException when holder contains comma");
        } catch (NumberFormatException e) {
            // Expected: parts[3] = " Name,2025" which is not a valid integer
            assertTrue(e.getMessage().contains("Name") || e.getMessage().contains("2025"));
        }
    }

    @Test
    public void fromCsv_parsesWithFourFieldSplit() {
        // Verify that the 4-field split correctly separates fields when
        // there are extra commas before the 4-threshold is reached
        DataRecord dr = DataRecord.fromCsv("Key,55.5,SimpleHolder,2023");
        assertNotNull(dr);
        assertEquals("Key", dr.key());
        assertEquals(55.5f, dr.value(), 0.001f);
        assertEquals("SimpleHolder", dr.holder());
        assertEquals(2023, dr.year());
    }
}

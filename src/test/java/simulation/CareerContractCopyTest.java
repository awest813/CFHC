package simulation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CareerContractCopyTest {

    @Test
    public void contractMessages_areConciseAndNonEmpty() {
        assertTrue(CareerContractCopy.proveItExtension(2).contains("Prove-it"));
        assertTrue(CareerContractCopy.contractExtension(4).contains("4 years"));
        assertTrue(CareerContractCopy.terminated().toLowerCase().contains("terminated"));
        assertEquals(
                "3 years left on your contract. Prestige 55 (baseline 50). Status: Secure.",
                CareerContractCopy.yearsRemaining(3, 55, 50, "Secure"));
        assertTrue(CareerContractCopy.seasonSummaryExtension(5).contains("5 years"));
        assertTrue(CareerContractCopy.seasonSummaryFired().toLowerCase().contains("fired"));
    }
}

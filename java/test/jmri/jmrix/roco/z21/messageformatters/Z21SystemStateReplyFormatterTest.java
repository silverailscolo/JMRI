package jmri.jmrix.roco.z21.messageformatters;

import jmri.jmrix.roco.z21.Z21Reply;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Z21SystemStateReplyFormatter class.
 *
 * @author Paul Bender Copyright (C) 2025
 */
public class Z21SystemStateReplyFormatterTest {

    @Test
    public void testFormatter(){
        Z21SystemStateReplyFormatter formatter = new Z21SystemStateReplyFormatter();
        byte msg[] = {(byte) 0x14, (byte) 0x00, (byte) 0x84, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        Z21Reply message = new Z21Reply(msg, 20);
        Assertions.assertTrue(formatter.handlesMessage(message));
        Assertions.assertEquals("Z21 System State:\n\tmain track current 0mA\n\tprogramming track current 255mA\n\tFiltered Main Track current 0mA\n\tInternal Temperature 0C\n\tSupply Voltage 0mV\n\tInternal Voltage 0mV\n\tState 0\n\tExtended State 0", formatter.formatMessage(message));
    }
}

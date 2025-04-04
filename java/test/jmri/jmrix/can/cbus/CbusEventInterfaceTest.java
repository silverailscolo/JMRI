package jmri.jmrix.can.cbus;

import jmri.util.JUnitUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Paul Bender Copyright (C) 2017
 * @author Steve Young Copyright (C) 2020
 */
public class CbusEventInterfaceTest {

    @Test
    public void testCTor() {

        TestInterface t = new TestInterface();
        assertNotNull(t);

        assertNull(t.checkEvent(t.getBeanOnMessage()));
        assertEquals( t.getBeanOffMessage(), t.checkEvent(t.getBeanOffMessage()) );

    }

    private static class TestInterface implements CbusEventInterface{

        @Override
        public jmri.jmrix.can.CanMessage getBeanOnMessage(){
            return CbusMessage.getRequestTrackOff(1);
        }

        @Override
        public jmri.jmrix.can.CanMessage getBeanOffMessage(){
            return new CbusEvent(0,0).getCanMessage(0, 123, 456, CbusEvent.EvState.ON);
        }

    }

    @BeforeEach
    public void setUp() {
        JUnitUtil.setUp();
    }

    @AfterEach
    public void tearDown() {
        JUnitUtil.tearDown();
    }

    // private final static Logger log = LoggerFactory.getLogger(CbusNodeSingleEventTableDataModelTest.class);

}

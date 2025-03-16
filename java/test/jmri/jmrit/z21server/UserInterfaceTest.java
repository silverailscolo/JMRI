package jmri.jmrit.z21server;

//import jmri.InstanceManager;
import jmri.util.JUnitUtil;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

/**
 * Test simple functioning of UserInterface
 *
 * @author Eckart Meyer (C) 2025
 */
@DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
public class UserInterfaceTest extends jmri.util.JmriJFrameTestBase {

    @Test
    public void testUserInterfaceCtor() {
        Assertions.assertNotNull( frame, "exists" );
    }

    @Override
    @BeforeEach
    public void setUp() {
        JUnitUtil.setUp();
        JUnitUtil.resetProfileManager();
        JUnitUtil.initRosterConfigManager();
        JUnitUtil.initInternalTurnoutManager();
        JUnitUtil.initInternalLightManager();
        JUnitUtil.initInternalSensorManager();
        JUnitUtil.initDebugThrottleManager();
//        JUnitUtil.initDefaultUserMessagePreferences();

        frame = new UserInterface();
    }

    @Override
    @AfterEach
    public void tearDown() {

        ((UserInterface)frame).disableServer();
        JUnitUtil.waitFor(() -> {
            return !((UserInterface)frame).isListen;
        },"Panel stops listening flag");

        JUnitUtil.clearShutDownManager();
        super.tearDown();

    }

}

package jmri.jmrit.vsdecoder;

import jmri.util.JUnitUtil;

import org.junit.Assert;
import org.junit.jupiter.api.*;

/**
 * Tests for the NotchTransition class
 *
 * @author Mark Underwood Copyright (C) 2011
 */
public class NotchTransitionTest {

    private NotchTransition uut = null;
    private final static String FILENAME = "java/test/jmri/jmrit/vsdecoder/test.wav";

    @Test
    public void testCreateFull() {
        Assert.assertEquals("sound name", "uname", uut.getName());
        Assert.assertEquals("file name", FILENAME, uut.getFileName());
        Assert.assertEquals("system name", "sysname", uut.getSystemName());
        Assert.assertEquals("user name", "uname", uut.getUserName());
        Assert.assertTrue("initialized", uut.isInitialized());
        Assert.assertFalse("is playing", uut.getSource().getState() == jmri.Audio.STATE_PLAYING);
    }

    @Test
    public void TestSetGet() {
        uut.setName("new name");
        Assert.assertEquals("set name", "new name", uut.getName());
    }

    @BeforeEach
    public void setUp() {
        JUnitUtil.setUp();
        uut = new NotchTransition(null, FILENAME, "sysname", "uname"); // BOUND_MODE
    }

    @AfterEach
    public void tearDown() {
        uut = null;

        // this created an audio manager, clean that up
        jmri.InstanceManager.getDefault(jmri.AudioManager.class).cleanup();

        jmri.util.JUnitAppender.suppressErrorMessage("Unhandled audio format type 0");
        JUnitUtil.tearDown();
    }
}

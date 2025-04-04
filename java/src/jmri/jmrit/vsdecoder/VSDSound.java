package jmri.jmrit.vsdecoder;

import java.awt.event.ActionListener;
import javax.swing.Timer;
import jmri.util.PhysicalLocation;
import org.jdom2.Element;

/**
 * Superclass for all Sound types.
 *
 * <hr>
 * This file is part of JMRI.
 * <p>
 * JMRI is free software; you can redistribute it and/or modify it under
 * the terms of version 2 of the GNU General Public License as published
 * by the Free Software Foundation. See the "COPYING" file for a copy
 * of this license.
 * <p>
 * JMRI is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * @author Mark Underwood Copyright (C) 2011
 * @author Klaus Killinger Copyright (C) 2025
 */
abstract public class VSDSound {

    final static String SrcSysNamePrefix = "IAS$VSD:";
    final static String BufSysNamePrefix = "IAB$VSD:";
    final static String SrcUserNamePrefix = "IVSDS_";
    final static String BufUserNamePrefix = "IVSDB_";

    final static float default_exponent = 1.0f;
    final static float default_gain = 0.8f;
    final static float default_reference_distance = 1.0f;
    final static float tunnel_volume = 0.5f;
    final static int default_sleep_interval = 50; // time in ms

    Timer t;

    boolean is_tunnel;
    String name;
    float gain; // this is the (fixed) gain relative to the other sounds in this Profile
    float volume; // this is the (active) volume level (product of fixed gain and volume slider).

    PhysicalLocation myposition;

    public VSDSound(String name) {
        this.name = name;
        gain = default_gain;
        t = null;
    }

    protected Timer newTimer(int time, boolean repeat, ActionListener al) {
        time = Math.max(1, time); // make sure the time is > zero
        t = new Timer(time, al);
        t.setInitialDelay(time);
        t.setRepeats(repeat);
        return t;
    }

    // Required methods - abstract because all subclasses MUST implement
    abstract public void play();

    abstract public void loop();

    abstract public void stop();

    abstract public void fadeIn();

    abstract public void fadeOut();

    abstract public void mute(boolean m);

    abstract public void setVolume(float g);

    abstract public void shutdown(); // called on window close.  Cease playing immediately.

    public void setPosition(PhysicalLocation p) {
        myposition = p;
    }

    public PhysicalLocation getPosition() {
        return myposition;
    }

    // Optional methods - overridden in subclasses where needed.  Do nothing otherwise
    public void changeNotch(int new_notch) {
    }

    public void changeThrottle(float t) {
    }

    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    public float getGain() {
        return gain;
    }

    public void setGain(float g) {
        gain = g;
    }

    int attachSourcesToEffects() {
        return 1;
    }

    int detachSourcesToEffects() {
        return 1;
    }

    void setTunnel(boolean t) {
        is_tunnel = t;
    }

    boolean getTunnel() {
        return is_tunnel;
    }

    boolean checkForFreeBuffer() {
        jmri.AudioManager am = jmri.InstanceManager.getDefault(jmri.AudioManager.class);
        if (am.getNamedBeanSet(jmri.Audio.BUFFER).size() < jmri.AudioManager.MAX_BUFFERS) {
            return true;
        } else {
            return false;
        }
    }

    public Element getXml() {
        Element me = new Element("Sound");

        me.setAttribute("name", name);
        me.setAttribute("type", "empty");
        return me;
    }

    public void setXml(Element e) {
        // Default: do nothing
    }

}

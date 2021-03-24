package jmri.managers;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jmri.Memory;
import jmri.implementation.DefaultMemory;
import jmri.jmrix.internal.InternalSystemConnectionMemo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide the concrete implementation for the Internal Memory Manager.
 *
 * Note that this should not enforce any particular system naming convention for Internal connection prefix.
 *
 * @author Bob Jacobsen Copyright (C) 2004
 */
public class DefaultMemoryManager extends AbstractMemoryManager {

    public DefaultMemoryManager(InternalSystemConnectionMemo memo) {
        super(memo);
    }

    @Override
    @Nonnull
    protected Memory createNewMemory(@Nonnull String systemName, @CheckForNull String userName) {
        // makeSystemName validates that systemName is correct
        return new DefaultMemory(validateSystemNameFormat(systemName), userName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Memory provideMemory(@Nonnull String systemName, String userName) {
        log.debug("provideMemory({})", systemName);
        Memory m;
        m = getByUserName(systemName);
        if (m != null) {
            return m;
        }
        m = getBySystemName(systemName);
        if (m != null) {
            return m;
        }
        // Memory does not exist, create a new one
        m = new DefaultMemory(validateSystemNameFormat(systemName), userName);
        // save in the maps
        register(m);

        // Keep track of the last created auto system name
        updateAutoNumber(systemName);

        return m;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Calls {@link #provideMemory(String, String)} with the result of
     * {@link #getAutoSystemName()} as the system name.
     */
    @Override
    @Nonnull
    public Memory newMemory(String userName) {
        return provideMemory(getAutoSystemName(), userName);
    }

    private final static Logger log = LoggerFactory.getLogger(DefaultMemoryManager.class);

}

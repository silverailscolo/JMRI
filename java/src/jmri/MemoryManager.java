package jmri;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Locate a Memory object representing some specific information.
 * <p>
 * Memory objects are obtained from a MemoryManager, which in turn is generally
 * located from the InstanceManager. A typical call sequence might be:
 * <pre>
 * Memory memory = InstanceManager.memoryManagerInstance().provideMemory("status");
 * </pre>
 * <p>
 * Each Memory has two names. The "user" name is entirely free form, and can
 * be used for any purpose. The "system" name is provided by the system-specific
 * implementations, if any, and provides a unique mapping to the layout control
 * system (for example LocoNet or NCE) and address within that system. Note that
 * most (all?) layout systems don't have anything corresponding to this, in
 * which case the "Internal" Memory objects are still available with names like
 * IM23.
 * <p>
 * Much of the book-keeping is implemented in the AbstractMemoryManager class,
 * which can form the basis for a system-specific implementation.
 *
 * <hr>
 * This file is part of JMRI.
 * <p>
 * JMRI is free software; you can redistribute it and/or modify it under the
 * terms of version 2 of the GNU General Public License as published by the Free
 * Software Foundation. See the "COPYING" file for a copy of this license.
 * <p>
 * JMRI is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * @author Bob Jacobsen Copyright (C) 2004
 * @see jmri.Memory
 * @see jmri.managers.AbstractMemoryManager
 * @see jmri.InstanceManager
 */
public interface MemoryManager extends ProvidingManager<Memory> {

    /**
     * Create a new Memory if it does not exist.
     *
     * @param systemName the system name for the Memory
     * @param userName   the user name for the Memory
     * @return null if a Memory with the same systemName or userName already
     *         exists or if there is trouble creating a new Memory
     */
    @Nonnull
    abstract Memory provideMemory(@Nonnull String systemName, String userName);

    @Nonnull
    abstract Memory provideMemory(@Nonnull String systemNamee);

    /**
     * Get an existing Turnout or return null if it doesn't exist. 
     * 
     * Locates via user name, then system name if needed.
     *
     * @param name User name or system name to match
     * @return null if no match found
     */
    @CheckForNull
    public Memory getMemory(@Nonnull String name);

    /**
     * Locate an existing Memory based on a system name. Returns null if no instance
     * already exists.
     *
     * @param systemName the system name
     * @return requested Memory object or null if none exists
     */
    @CheckForNull
    public Memory getBySystemName(@Nonnull String systemName);

    /**
     * Locate an existing Memory based on a user name. Returns null if no instance
     * already exists.
     *
     * @param userName the user name
     * @return requested Memory object or null if none exists
     */
    @CheckForNull
    public Memory getByUserName(@Nonnull String userName);

    /**
     * Return a Memory with the specified system and user names. Note that
     * two calls with the same arguments will get the same instance; there is
     * only one Memory object representing a given physical Memory and therefore
     * only one with a specific system or user name.
     * <p>
     * This will always return a valid object reference; a new object will be
     * created if necessary. In that case:
     * <ul>
     * <li>If a null reference is given for user name, no user name will be
     * associated with the Memory object created; a valid system name must be
     * provided
     * <li>If both names are provided, the system name defines the hardware
     * access of the desired Memory, and the user address is associated with it.
     * The system name must be valid.
     * </ul>
     * Note that it is possible to make an inconsistent request if both
     * addresses are provided, but the given values are associated with
     * different objects. This is a problem, and we don't have a good solution
     * except to issue warnings. This will mostly happen if you're creating
     * Memory objects when you should be looking them up.
     *
     * @param systemName the system name
     * @param userName   the user name
     * @return requested Memory object (never null)
     * @throws IllegalArgumentException if cannot create the Memory due to e.g.
     *                                  an illegal name or name that can't be
     *                                  parsed.
     */
    @Nonnull
    public Memory newMemory(@Nonnull String systemName, String userName);

    /**
     * For use with GUI, to allow the auto generation of systemNames, where
     * the user can optionally supply a username.
     * <p>
     * This will always return a valid object reference; a new object will be
     * created if necessary. (If a null reference is given for user name, no
     * user name will be associated with the Memory object created)
     *
     * Note that it is possible to make an inconsistent request if both
     * addresses are provided, but the given values are associated with
     * different objects. This is a problem, and we don't have a good solution
     * except to issue warnings. This will mostly happen if you're creating
     * Memory objects when you should be looking them up.
     *
     * @param userName the user name, can be null
     * @return requested Memory object (never null)
     * @throws IllegalArgumentException if cannot create the Memory due to e.g.
     *                                  an illegal name or name that can't be
     *                                  parsed.
     */
    @Nonnull
    public Memory newMemory(@Nullable String userName);

}

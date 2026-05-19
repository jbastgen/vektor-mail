package com.vektor.mail.core.plugin;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * Base class for all Vektor plugins. Extend this and annotate with
 * {@code @org.pf4j.Extension} on implementing classes.
 */
public abstract class VektorPlugin extends Plugin {

    protected VektorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
}

package io.openliberty.boost.utils;

import org.codehaus.mojo.pluginsupport.MojoSupport;

import io.openliberty.boost.BoostLoggerI;

public class BoostLogger extends MojoSupport implements BoostLoggerI {

    private static BoostLogger logger = null;

    public static BoostLogger getInstance() {
        if (logger == null) {
            logger = new BoostLogger();
        }
        return logger;
    }

    @Override
    public void debug(String msg) {
        getLog().debug(msg);
    }

    @Override
    public void debug(String msg, Throwable e) {
        getLog().debug(msg, e);
    }

    @Override
    public void debug(Throwable e) {
        getLog().debug(e);
    }

    @Override
    public void warn(String msg) {
        getLog().warn(msg);
    }

    @Override
    public void info(String msg) {
        getLog().info(msg);
    }

    @Override
    public void error(String msg) {
        getLog().error(msg);
    }

    @Override
    public boolean isDebugEnabled() {
        return getLog().isDebugEnabled();
    }

}

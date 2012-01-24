/*
 * #%L
 * Osm2garminAPI
 * %%
 * Copyright (C) 2011 Frantisek Mantlik <frantisek at mantlik.cz>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package org.mantlik.osm2garmin;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Properties;
import org.openide.util.RequestProcessor;

/**
 *
 * @author fm
 */
public abstract class ThreadProcessor implements Runnable, PropertyChangeListener {

    /**
     *
     */
    public static final int RUNNING = 1;
    /**
     *
     */
    public static final int COMPLETED = 2;
    /**
     *
     */
    public static final int ERROR = 3;
    /**
     *
     */
    public static final String BLANKLINE = "                                                                               \r";
    Properties parameters;
    String libdir;
    private int state = RUNNING;
    private String status = "";
    private float progress = 0;
    /**
     *
     */
    public Thread thread;
    public ClassLoader classLoader;
    /**
     *
     */
    public PropertyChangeSupport changeSupport;
    boolean commandline = true;
    RequestProcessor processor;
    long waitFrom = -1;

    /**
     *
     * @param parameters
     */
    public ThreadProcessor(Properties parameters) {
        changeSupport = new PropertyChangeSupport(this);
        this.parameters = parameters;
        if (parameters.containsKey("gui") && parameters.getProperty("gui").equals("1")) {
            commandline = false;
        }
        if (Osm2garmin.stop) {
            state = ERROR;
            status = "Interrupted.";
            return;
        }
        process();
    }

    private void process() {
        if (commandline) {
            thread = new Thread(this);
            classLoader = thread.getContextClassLoader();
            thread.start();
        } else {
            processor = new RequestProcessor(getClass().getSimpleName(), 1, true);
            processor.post(this);
        }
    }

    /**
     *
     * @return
     */
    public String getStatus() {
        return status + waitingString();
    }

    /**
     *
     * @return
     */
    public int getState() {
        return state;
    }

    /**
     *
     * @return
     */
    public float getProgress() {
        return progress;
    }

    /**
     *
     * @param progress
     */
    public void setProgress(float progress) {
        if (this.progress == progress) {
            return;
        }
        float oldProgress = this.progress;
        this.progress = progress;
        changeSupport.firePropertyChange("progress", oldProgress, progress);
    }

    /**
     *
     * @param state
     */
    public void setState(int state) {
        if (this.state == state) {
            return;
        }
        int oldState = this.state;
        this.state = state;
        changeSupport.firePropertyChange("state", oldState, state);
    }

    /**
     *
     * @param status
     */
    public void setStatus(String status) {
        if (this.status != null && this.status.equals(status)) {
            return;
        }
        String oldStatus = this.status;
        this.status = status;
        changeSupport.firePropertyChange("status", oldStatus, status);
    }

    /*
     * Returns true if completed. Prints lifecycle info.
     */
    /**
     *
     * @param processName
     * @return
     */
    public boolean lifeCycleCheck(String processName) {
        if (Osm2garmin.stop) {
            if (commandline) {
                System.out.print(BLANKLINE);
                System.out.println(getStatus());
                System.out.println(processName + " interrupted.");
            } else {
                if (processor != null) {
                    processor.shutdownNow();
                }
            }
            //throw new InterruptedException(processName + " interrupted.");
            setStatus(processName + " interrupted.");
            state = ERROR;
            return true;
        } else if (state == ERROR) {
            if (commandline) {
                System.out.print(BLANKLINE);
            }
            System.out.println(getStatus());
            System.out.println(processName + " failed.");
            //throw new ExecutionException(new Exception(processName + " failed."));
            return true;
        } else if (state == COMPLETED) {
            if (commandline) {
                System.out.print(BLANKLINE);
            }
            System.out.println(processName + " completed.");
            return true;
        } else {
            if (commandline) {
                System.out.print(BLANKLINE);
                System.out.print(getStatus() + "\r");
            //} else {
            //    getStatus();
            }
        }
        return false;
    }

    public String waitingString() {
        if (waitFrom < 0) {
            return "";
        }
        long sleep = System.currentTimeMillis() - this.waitFrom;
        // delay wait show
        if (sleep < Utilities.REFRESH_INTERVAL) {
            return "";
        }
        int sec = (int) ((sleep / 1000) % 60);
        int min = (int) ((sleep / 1000 / 60) % 60);
        int hour = (int) ((sleep / 1000 / 60 / 60) % 24);
        int day = (int) (sleep / 1000 / 60 / 60 / 24);
        String wait = "";
        if (day == 1) {
            wait = "1 day ";
        } else if (day > 1) {
            wait = day + " days ";
        }
        wait += hour + ":" + min + ":" + sec;
        return " (waiting " + wait + ")";
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        changeSupport.firePropertyChange(evt);
    }
}

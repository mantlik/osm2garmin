/*
 * #%L
 * Osm2garminAPI
 * %%
 * Copyright (C) 2011 - 2012 Frantisek Mantlik <frantisek at mantlik.cz>
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
/*
 * Copyright (C) 2012 Frantisek Mantlik <frantisek at mantlik.cz>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.mantlik.osm2garmin;

import java.util.ArrayList;
import java.util.Properties;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openide.util.Exceptions;

/**
 *
 * @author Frantisek Mantlik <frantisek at mantlik.cz>
 */
public class Osm2garminTest {

    public Osm2garminTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of runExternal method, of class Osm2garmin.
     * Trying to simulate problem described in Issue 18.
     *
     * @throws Exception
     */
    @Test(timeout=60000)
    public void testRunExternal() throws Exception {
        String testfile = getClass().getResource("25435000.osm.gz").getPath();
        String pbftestfile = getClass().getResource("63240001.osm.pbf").getPath();

        ArrayList<ExtprocessProcessor> processes = new ArrayList<ExtprocessProcessor>();
        for (int step = 0; step < 5; step++) {
            Properties props = new Properties();
            props.setProperty("basedir", "target/testwork/");
            processes.add(new ExtprocessProcessor(props,
                    "uk.me.parabola.splitter.Main", "main", "splitter", new String[]{testfile}));
            Thread.sleep(500);
            props = new Properties();
            props.setProperty("basedir", "target/testwork/");
            processes.add(new ExtprocessProcessor(props,
                    "uk.me.parabola.mkgmap.main.Main", "main", "mkgmap", new String[]{testfile}));
            Thread.sleep(500);
            props = new Properties();
            props.setProperty("basedir", "target/testwork/");
            processes.add(new ExtprocessProcessor(props,
                    "org.openstreetmap.osmosis.core.Osmosis", "run", "osmosis",
                    new String[]{"--rb", pbftestfile, "--wn"}));
            Thread.sleep(500);
        }
        boolean finished = false;
        while (!finished) {
            finished = true;
            Thread.sleep(1000);
            for (ExtprocessProcessor p : processes) {
                if (p.getState() == ExtprocessProcessor.RUNNING) {
                    System.out.print(processes.indexOf(p) + ": " + p.getStatus());
                    finished = false;
                } else if (p.getState() == ExtprocessProcessor.ERROR) {
                    System.out.println();
                    System.out.println(p.getStatus());
                    assertTrue("External process failed " + p, false);
                }
            }
            System.out.println();
        }
    }

    class ExtprocessProcessor extends ThreadProcessor {

        Properties properties;
        String extclass;
        String method;
        String library;
        String[] args;

        ExtprocessProcessor(Properties properties, String extclass, String method, String library,
                String[] args) {
            super(properties, false);
            this.properties = properties;
            this.extclass = extclass;
            this.method = method;
            this.library = library;
            this.args = args;
            start();
        }

        @Override
        public void run() {
            setStatus(library + " started.");
            try {
                Thread.sleep((int) (3000 * Math.random()));
            } catch (InterruptedException ex) {
                return;
            }
            setStatus(library + " running.");
            try {
                    Utilities.getInstance().runExternal(extclass, method, library, args, this);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
                setStatus(library + " error.");
                setState(ThreadProcessor.ERROR);
            }
            setStatus(library + " finished.");
            setState(ThreadProcessor.COMPLETED);
        }
    }
}

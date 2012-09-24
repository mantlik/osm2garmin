/*
 * #%L
 * Osm2garminGUI
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
package org.mantlik.osm2garminspi;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        String specVersion = "";
        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                String sv = manifest.getMainAttributes().getValue("OpenIDE-Module-Specification-Version");
                if (sv != null) {
                    specVersion = sv;
                }
            }
        } catch (IOException E) {
            // handle
        }
        System.setProperty("netbeans.buildnumber", specVersion);
    }
    private static boolean canClose = true;

    public static void setCanClose(boolean value) {
        canClose = value;
    }

    @Override
    public boolean closing() {
        if (!canClose) {
            if (DialogDisplayer.getDefault().notify(new NotifyDescriptor.Confirmation(
                    "Maps processing is running. Do you really want to cancel it?",
                    NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.QUESTION_MESSAGE)).equals(NotifyDescriptor.YES_OPTION)) {
                return super.closing();
            } else {
                return false;
            }
        }
        return super.closing();
    }
}

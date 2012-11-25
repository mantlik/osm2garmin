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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Options",
id = "org.mantlik.osm2garminspi.OptionsAction")
@ActionRegistration(iconBase = "org/mantlik/osm2garminspi/Globe.png",
displayName = "#CTL_OptionsAction")
@ActionReferences({
    @ActionReference(path = "Toolbars/File", position = 3333)
})
@Messages("CTL_OptionsAction=Options")
public final class OptionsAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        OptionsDisplayer.getDefault().open("Osm2garmin");
    }
}

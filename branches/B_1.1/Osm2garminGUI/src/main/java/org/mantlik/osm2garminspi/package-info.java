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
@ContainerRegistration(id = "Osm2garmin", categoryName = "#OptionsCategory_Name_Osm2garmin", 
        iconBase = "org/mantlik/osm2garminspi/Globe32.png", 
        keywords = "#OptionsCategory_Keywords_Osm2garmin", 
        keywordsCategory = "Osm2garmin",
        position = 10)
@Messages(value = {"OptionsCategory_Name_Osm2garmin=Osm2garmin", "OptionsCategory_Keywords_Osm2garmin=Osm2garmin converter"})
@HelpSetRegistration(
        helpSet="docs/osm2garmin-hs.xml", position=377)

package org.mantlik.osm2garminspi;

import org.netbeans.api.javahelp.HelpSetRegistration;
import org.netbeans.spi.options.OptionsPanelController.ContainerRegistration;
import org.openide.util.NbBundle.Messages;

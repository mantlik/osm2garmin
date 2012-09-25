/*
 * #%L
 * Osm2garmin-UI
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
 * Copyright (C) 2012 fm
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
package org.mantlik.osm2garminspi;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.Action;
import org.mantlik.osm2garmin.Osm2garmin;
import org.mantlik.osm2garmin.Utilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

@ActionID(
    category = "File",
id = "org.mantlik.osm2garminspi.EditArgFilesAction")
@ActionRegistration(
    iconBase = "org/mantlik/osm2garminspi/edit.png",
displayName = "#CTL_EditArgFilesAction")
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 2500, separatorAfter = 2550),
    @ActionReference(path = "Toolbars/File", position = 3433)
})
@Messages("CTL_EditArgFilesAction=Open/close argument files editors")
public final class EditArgFilesAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        String userdir = NbPreferences.forModule(Osm2garmin.class).get("userdir", "./");
        try {
            Utilities.checkArgFiles(userdir);
            for (String name : Utilities.ARGS_FILES) {
                FileObject fo = FileUtil.toFileObject(FileUtil.normalizeFile(new File(userdir, name)));
                DataObject dao = DataObject.find(fo);
                final Node node = dao.getNodeDelegate();
                TopComponent tc = null;
                for (TopComponent t : TopComponent.getRegistry().getOpened()) {
                    if (t.getName().equals(name)) {
                        tc = t;
                    }
                }
                if (tc != null) {
                    tc.close();
                } else {
                    Action a = node.getPreferredAction();
                    if (a instanceof ContextAwareAction) {
                        a = ((ContextAwareAction) a).createContextAwareInstance(node.getLookup());
                    }
                    if (a != null) {
                        a.actionPerformed(new ActionEvent(node, ActionEvent.ACTION_PERFORMED, "")); // NOI18N 
                    }
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}

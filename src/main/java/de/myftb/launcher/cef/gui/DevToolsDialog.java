/*
 * MyFTBLauncher
 * Copyright (C) 2023 MyFTB <https://myftb.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.myftb.launcher.cef.gui;

import java.awt.Dialog;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JDialog;

import org.cef.browser.CefBrowser;

public class DevToolsDialog extends JDialog {
    public static final long serialVersionUID = 7271475725186540702L;
    static DevToolsDialog devTools;
    private final CefBrowser browser;

    public DevToolsDialog(CefBrowser cefBrowser) {
        super((Dialog) null, "MyFTB Launcher - DevTools", false);
        if (DevToolsDialog.devTools != null) {
            DevToolsDialog.devTools.dispose();
        }
        DevToolsDialog.devTools = this;

        this.setSize(960, 576);
        this.browser = cefBrowser.getDevTools();
        this.add(this.browser.getUIComponent());
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(final ComponentEvent componentEvent) {
                DevToolsDialog.this.dispose();
            }
        });
    }
    
    @Override
    public void dispose() {
        this.browser.close(true);
        super.dispose();
    }

}
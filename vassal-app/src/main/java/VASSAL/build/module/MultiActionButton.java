/*
 *
 * Copyright (c) 2020 by Vassal developers
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */

package VASSAL.build.module;

import java.awt.Component;

import javax.swing.JMenuItem;

import VASSAL.build.GameModule;
import VASSAL.build.module.documentation.HelpFile;
import VASSAL.i18n.Resources;
import VASSAL.tools.RecursionLimitException;
import VASSAL.tools.RecursionLimiter;
import VASSAL.tools.RecursionLimiter.Loopable;

/**
 * Combines multiple buttons from the toolbar into a single button. Pushing the single button is equivalent to pushing
 * the other buttons in order.
 *
 * @author rkinney
 *
 */
public class MultiActionButton extends ToolbarMenu implements Loopable {

  public MultiActionButton() {
    super();
    setAttribute(BUTTON_TEXT, Resources.getString("Editor.MultiActionButton.component_type")); //$NON-NLS-1$
    setAttribute(TOOLTIP, Resources.getString("Editor.MultiActionButton.component_type")); //$NON-NLS-1$
    launch.putClientProperty(MENU_PROPERTY, null);
  }

  @Override
  public String[] getAttributeDescriptions() {
    return new String[] {
        Resources.getString(Resources.DESCRIPTION),
        Resources.getString(Resources.BUTTON_TEXT),
        Resources.getString(Resources.TOOLTIP_TEXT),
        Resources.getString(Resources.BUTTON_ICON),
        Resources.getString(Resources.HOTKEY_LABEL),
        Resources.getString("Editor.MultiActionButton.buttons") //$NON-NLS-1$
    };
  }

  @Override
  public void launch() {
    // Pause logging to accumulate commands generated by the
    // separate toolbar buttons.
    final GameModule mod = GameModule.getGameModule();
    final boolean loggingPaused = mod.pauseLogging();

    try {
      RecursionLimiter.startExecution(this);

      for (int i = 0, n = menu.getComponentCount(); i < n; ++i) {
        Component c = menu.getComponent(i);
        if (c instanceof JMenuItem) {
          ((JMenuItem)c).doClick();
        }
      }
    }
    catch (RecursionLimitException e) {
      RecursionLimiter.infiniteLoop(e);
    }
    finally {
      RecursionLimiter.endExecution();
      // If we are in control of logging, retrieve the accumulated Commands,
      // turn off pause and send the Commands to the log.
      if (loggingPaused) {
        mod.sendAndLog(mod.resumeLogging());
      }
    }
  }

  public static String getConfigureTypeName() {
    return Resources.getString("Editor.MultiActionButton.component_type"); //$NON-NLS-1$
  }

  @Override
  public HelpFile getHelpFile() {
    return HelpFile.getReferenceManualPage("MultiActionButton.html"); //$NON-NLS-1$
  }

  // Implement Loopable
  @Override
  public String getComponentName() {
    return getConfigureName();
  }

  @Override
  public String getComponentTypeName() {
    return getConfigureTypeName();
  }
}
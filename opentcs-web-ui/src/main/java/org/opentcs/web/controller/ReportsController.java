// SPDX-FileCopyrightText: DPW FMS Contributors
// SPDX-License-Identifier: MIT
package org.opentcs.web.controller;

/** Reporting view. Authentication is delegated to the deployment container/identity proxy. */
public class ReportsController
    extends
      PageController {
  public ReportsController() {
    super("reports.jsp");
  }
}

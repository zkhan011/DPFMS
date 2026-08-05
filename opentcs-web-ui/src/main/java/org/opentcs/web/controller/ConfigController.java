// SPDX-FileCopyrightText: DPW FMS Contributors
// SPDX-License-Identifier: MIT
package org.opentcs.web.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Renders the DPW FMS browser-side symbol and marker configuration page. */
public class ConfigController
    extends
      HttpServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException,
        IOException {
    request.getRequestDispatcher("/WEB-INF/jsp/config.jsp").forward(request, response);
  }
}

// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Base servlet for JSP pages.
 *
 * @author Zishan Khan
 */
public abstract class PageController
    extends
      HttpServlet {
  private final String view;

  protected PageController(String view) {
    this.view = view;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException,
        IOException {
    request.getRequestDispatcher("/WEB-INF/jsp/" + view).forward(request, response);
  }
}

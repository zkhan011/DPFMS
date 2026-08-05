// SPDX-FileCopyrightText: DPW FMS Contributors
// SPDX-License-Identifier: MIT
package org.opentcs.web.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Renders the kernel service-web-API console. */
public class KernelApiController
    extends
      HttpServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException,
        IOException {
    request.getRequestDispatcher("/WEB-INF/jsp/kernel-api.jsp").forward(request, response);
  }
}

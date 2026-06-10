<%-- SPDX-FileCopyrightText: Zishan Khan --%>
<%-- SPDX-License-Identifier: MIT --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>openTCS Web UI</title><link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
<script defer src="${pageContext.request.contextPath}/static/js/app.js"></script>
</head><body data-context="${pageContext.request.contextPath}"><header><strong>openTCS Web UI</strong><span id="connection" class="pill">Connecting…</span></header>
<nav><a href="${pageContext.request.contextPath}/dashboard">Dashboard</a><a href="${pageContext.request.contextPath}/plant-overview">Plant SVG View</a><a href="${pageContext.request.contextPath}/map-overview">Map View</a><a href="${pageContext.request.contextPath}/vehicles">Vehicles</a><a href="${pageContext.request.contextPath}/transport-orders">Transport Orders</a><a href="${pageContext.request.contextPath}/control-center">Control Center</a></nav><main>

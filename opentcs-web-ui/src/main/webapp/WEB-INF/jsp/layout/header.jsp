<%-- SPDX-FileCopyrightText: Zishan Khan --%>
<%-- SPDX-License-Identifier: MIT --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="theme-color" content="#08111f"><title>openTCS Fleet Console</title><link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
<script defer src="${pageContext.request.contextPath}/static/js/app.js"></script>
</head><body data-context="${pageContext.request.contextPath}"><header><a class="brand" href="${pageContext.request.contextPath}/dashboard"><span class="brand-mark">T</span><span><strong>openTCS</strong><small>Fleet Console</small></span></a><span id="connection" class="pill"><i></i> Connecting…</span></header>
<nav aria-label="Primary navigation"><a href="${pageContext.request.contextPath}/dashboard">Overview</a><a href="${pageContext.request.contextPath}/plant-overview">Plant</a><a href="${pageContext.request.contextPath}/map-overview">Map</a><a href="${pageContext.request.contextPath}/vehicles">Vehicles</a><a href="${pageContext.request.contextPath}/transport-orders">Orders</a><a href="${pageContext.request.contextPath}/control-center">Control center</a></nav><main>

<%-- SPDX-FileCopyrightText: Zishan Khan --%>
<%-- SPDX-License-Identifier: MIT --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="theme-color" content="#08111f"><title>DPW FMS — Fleet Operations</title><link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
<script defer src="${pageContext.request.contextPath}/static/js/app.js"></script>
</head><body data-context="${pageContext.request.contextPath}"><header><a class="brand" href="${pageContext.request.contextPath}/map-overview"><span class="brand-mark">D</span><span><strong>DPW FMS</strong><small>Fleet Management</small></span></a><span id="connection" class="pill"><i></i> Connecting…</span></header>
<nav aria-label="Primary navigation"><a href="${pageContext.request.contextPath}/map-overview">Live map</a><a href="${pageContext.request.contextPath}/dashboard">Overview</a><a href="${pageContext.request.contextPath}/plant-overview">Engineering view</a><a href="${pageContext.request.contextPath}/vehicles">Vehicles</a><a href="${pageContext.request.contextPath}/transport-orders">Orders</a><a href="${pageContext.request.contextPath}/control-center">Control center</a><a href="${pageContext.request.contextPath}/reports">Reports</a><a href="${pageContext.request.contextPath}/config">Config</a><a href="${pageContext.request.contextPath}/kernel-api">Kernel API</a></nav><main>

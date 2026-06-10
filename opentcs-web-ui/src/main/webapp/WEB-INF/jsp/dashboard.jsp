<%-- Web frontend component for openTCS. Author: Zishan Khan. --%>
<%@ include file="layout/header.jsp" %>
<h1>Dashboard</h1><section class="cards">
<article><h2>Kernel status</h2><b id="kernel-status">Loading…</b></article><article><h2>System mode</h2><b id="system-mode">Operating / API</b></article>
<article><h2>Vehicles</h2><b id="vehicle-count">—</b></article><article><h2>Active vehicles</h2><b id="active-count">—</b></article>
<article><h2>Transport orders</h2><b id="order-count">—</b></article><article><h2>Failed orders</h2><b id="failed-count">—</b></article></section>
<section class="panel"><h2>Recent warnings</h2><p id="status-message">Waiting for kernel status…</p></section>
<script>window.pageKind='dashboard';</script><%@ include file="layout/footer.jsp" %>

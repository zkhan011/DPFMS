<%-- Web frontend component for openTCS. Author: Zishan Khan. --%>
<%@ include file="layout/header.jsp" %>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" integrity="sha256-p4NxAoJBhIINfQ3ynhOZbKfMZrG99DvybYH5Z3i5A0o=" crossorigin="">
<div class="title-row"><h1>Map Overview</h1><a class="button" href="${pageContext.request.contextPath}/plant-overview">Switch to Plant SVG View</a></div>
<section class="toolbar"><span>Provider: <b id="map-provider">Loading…</b></span><span>Calibration: <b id="calibration">Loading…</b></span><label>Source <select id="route-source"></select></label><label>Destination <select id="route-destination"></select></label><button id="calculate-route">Calculate Route</button></section>
<p id="map-message" class="notice"></p><div id="map"></div><p id="route-result"></p>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>
<script defer src="${pageContext.request.contextPath}/static/js/map-overview.js"></script><%@ include file="layout/footer.jsp" %>

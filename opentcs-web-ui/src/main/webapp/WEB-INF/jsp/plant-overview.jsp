<%-- Web frontend component for openTCS. Author: Zishan Khan. --%>
<%@ include file="layout/header.jsp" %>
<div class="title-row"><h1>Plant Overview</h1><a class="button" href="${pageContext.request.contextPath}/map-overview">Switch to Map View</a></div>
<section class="toolbar"><label>Source <select id="route-source"></select></label><label>Destination <select id="route-destination"></select></label><button id="calculate-route">Calculate Route</button><span id="route-result"></span></section>
<section class="panel"><h2 id="model-name">Plant model</h2><div class="svg-wrap"><svg id="plant-svg" viewBox="0 0 1000 700" aria-label="Plant model visualization"></svg></div></section>
<script defer src="${pageContext.request.contextPath}/static/js/plant-overview.js"></script><%@ include file="layout/footer.jsp" %>

<%-- Web frontend component for openTCS. Author: Zishan Khan. --%>
<%@ include file="layout/header.jsp" %><h1>Vehicle Monitoring</h1><p class="notice">Actions use supported openTCS service web API operations. Enable/disable and integration-level controls are intentionally omitted here until explicitly selected.</p>
<div class="table-wrap"><table><thead><tr><th>Name</th><th>Position</th><th>State</th><th>Processing</th><th>Integration</th><th>Energy</th><th>Order</th><th>Last update</th><th>Actions</th></tr></thead><tbody id="vehicle-rows"></tbody></table></div>
<script defer src="${pageContext.request.contextPath}/static/js/vehicles.js"></script><%@ include file="layout/footer.jsp" %>

package com.ostarosto.app.feature.location

/**
 * A self-contained Leaflet + OpenStreetMap page (free tiles, no API key).
 * Loaded into a WebView via `rememberWebViewStateWithHTMLData`. The marker
 * starts at the map center and reports its position to Kotlin — on load, on
 * drag, and on tap-elsewhere-on-map — through the `PinMoved` JS bridge
 * message the library auto-injects as `window.kmpJsBridge`.
 */
fun leafletMapHtml(centerLat: Double, centerLng: Double, zoom: Int = 15): String = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<style>html, body, #map { height: 100%; margin: 0; padding: 0; }</style>
</head>
<body>
<div id="map"></div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script>
  var map = L.map('map').setView([$centerLat, $centerLng], $zoom);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; OpenStreetMap contributors'
  }).addTo(map);

  var marker = L.marker([$centerLat, $centerLng], { draggable: true }).addTo(map);

  function notifyNative(lat, lng) {
    if (window.kmpJsBridge) {
      window.kmpJsBridge.callNative("PinMoved", JSON.stringify({lat: lat, lng: lng}), function(response) {});
    }
  }

  marker.on('dragend', function () {
    var pos = marker.getLatLng();
    notifyNative(pos.lat, pos.lng);
  });

  map.on('click', function (e) {
    marker.setLatLng(e.latlng);
    notifyNative(e.latlng.lat, e.latlng.lng);
  });

  notifyNative($centerLat, $centerLng);
</script>
</body>
</html>
""".trimIndent()

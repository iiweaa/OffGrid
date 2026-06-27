package com.offgrid.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offgrid.app.link.location.GeoUtils
import com.offgrid.app.link.location.Location
import com.offgrid.app.link.node.NodeId
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PeerScreen(
    viewModel: PeerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.voiceState.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Peer Compass",
                style = MaterialTheme.typography.headlineMedium
            )

            val myLocation = state.myLocation
            if (myLocation == null) {
                Text(
                    text = "Waiting for GPS fix...",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Compass(myLocation, state.peerLocations)
                PeerList(myLocation, state.peerLocations)
            }
        }
    }
}

@Composable
private fun Compass(
    myLocation: Location,
    peerLocations: Map<NodeId, Location>
) {
    val textMeasurer = rememberTextMeasurer()
    val peers = peerLocations.mapNotNull { (nodeId, location) ->
        val distance = GeoUtils.distanceMeters(myLocation, location)
        val bearing = GeoUtils.bearingDegrees(myLocation, location)
        PeerSight(nodeId, distance, bearing)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.size(320.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val center = Offset(centerX, centerY)
            val radius = size.minDimension / 2 - 32.dp.toPx()

            // Compass circle.
            drawCircle(
                color = Color.Gray,
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            )

            // Center dot (self).
            drawCircle(
                color = primaryColor,
                radius = 8.dp.toPx(),
                center = center
            )

            // Cardinal labels.
            drawCardinalLabel("N", center, radius, 0.0, textMeasurer, onSurfaceColor)
            drawCardinalLabel("E", center, radius, 90.0, textMeasurer, onSurfaceColor)
            drawCardinalLabel("S", center, radius, 180.0, textMeasurer, onSurfaceColor)
            drawCardinalLabel("W", center, radius, 270.0, textMeasurer, onSurfaceColor)

            // Peer markers.
            peers.forEach { peer ->
                drawPeerMarker(center, radius, peer, textMeasurer, secondaryColor, onSurfaceColor)
            }
        }
    }
}

private data class PeerSight(
    val nodeId: NodeId,
    val distanceMeters: Double,
    val bearingDegrees: Double
)

private fun DrawScope.drawCardinalLabel(
    label: String,
    center: Offset,
    radius: Float,
    bearingDegrees: Double,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textColor: Color
) {
    val angle = Math.toRadians(bearingDegrees - 90.0)
    val labelRadius = radius + 18.dp.toPx()
    val position = Offset(
        center.x + labelRadius * cos(angle).toFloat(),
        center.y + labelRadius * sin(angle).toFloat()
    )
    val textLayout = textMeasurer.measure(label)
    drawText(
        textMeasurer = textMeasurer,
        text = label,
        topLeft = Offset(
            position.x - textLayout.size.width / 2,
            position.y - textLayout.size.height / 2
        ),
        style = TextStyle(color = textColor)
    )
}

private fun DrawScope.drawPeerMarker(
    center: Offset,
    radius: Float,
    peer: PeerSight,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    markerColor: Color,
    textColor: Color
) {
    val angle = Math.toRadians(peer.bearingDegrees - 90.0)
    val markerRadius = radius * 0.75f
    val position = Offset(
        center.x + markerRadius * cos(angle).toFloat(),
        center.y + markerRadius * sin(angle).toFloat()
    )

    // Line from center to marker.
    drawLine(
        color = markerColor,
        start = center,
        end = position,
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Marker dot.
    drawCircle(
        color = markerColor,
        radius = 8.dp.toPx(),
        center = position
    )

    // Label.
    val label = "${peer.nodeId.toHex().takeLast(8).uppercase()}\n${formatDistance(peer.distanceMeters)}"
    val textLayout = textMeasurer.measure(label)
    val labelOffset = Offset(
        position.x - textLayout.size.width / 2,
        position.y + 12.dp.toPx()
    )
    drawText(
        textMeasurer = textMeasurer,
        text = label,
        topLeft = labelOffset,
        style = TextStyle(color = textColor)
    )
}

@Composable
private fun PeerList(
    myLocation: Location,
    peerLocations: Map<NodeId, Location>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Peer List (${peerLocations.size})",
                style = MaterialTheme.typography.titleMedium
            )
            if (peerLocations.isEmpty()) {
                Text(
                    text = "No peer locations received yet",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                peerLocations.forEach { (nodeId, location) ->
                    val distance = GeoUtils.distanceMeters(myLocation, location)
                    val bearing = GeoUtils.bearingDegrees(myLocation, location)
                    Text(
                        text = "${nodeId.toHex().takeLast(8).uppercase()} — " +
                            "${formatDistance(distance)}, ${"%.0f".format(bearing)}°",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters < 1000.0) {
        "%.0f m".format(meters)
    } else {
        "%.1f km".format(meters / 1000.0)
    }
}

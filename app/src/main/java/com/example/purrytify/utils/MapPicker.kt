package com.example.purrytify.utils

import android.content.Context
import android.location.Geocoder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver

import java.util.Locale

@Composable
fun MapPicker(
    modifier: Modifier = Modifier,
    onLocationPicked: (countryCode: String) -> Unit
) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        Configuration.getInstance()
            .load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    AndroidView(
        modifier = modifier,
        factory = {
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(3.0)
                controller.setCenter(GeoPoint(0.0, 0.0))


                overlays.add(object : MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {

                        overlays.filterIsInstance<Marker>().forEach { overlays.remove(it) }
                        overlays.add(Marker(this@apply).apply {
                            position = p
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        })
                        invalidate()


                        val countryCode = Geocoder(ctx, Locale.getDefault())
                           .getFromLocation(p.latitude, p.longitude, 1)
                           ?.firstOrNull()
                           ?.countryCode
                            countryCode?.let(onLocationPicked)
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                }) {})
            }
        }
    )
}
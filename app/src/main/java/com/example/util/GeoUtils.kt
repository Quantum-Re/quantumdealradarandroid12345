package com.example.util

object GeoUtils {
    data class LatLng(val latitude: Double, val longitude: Double)

    fun getCoordinatesForLocation(location: String): LatLng? {
        val locUpper = location.uppercase()
        return when {
            locUpper.contains("MILANO") || locUpper.contains("(MI)") -> LatLng(45.4642, 9.1900)
            locUpper.contains("VERONA") || locUpper.contains("(VR)") -> LatLng(45.4384, 10.9916)
            locUpper.contains("TORINO") || locUpper.contains("(TO)") -> LatLng(45.0703, 7.6869)
            locUpper.contains("ROMA") || locUpper.contains("(RM)") -> LatLng(41.9028, 12.4964)
            locUpper.contains("BOLOGNA") || locUpper.contains("(BO)") -> LatLng(44.4949, 11.3426)
            locUpper.contains("SIENA") || locUpper.contains("(SI)") -> LatLng(43.3188, 11.3308)
            locUpper.contains("FIRENZE") || locUpper.contains("(FI)") -> LatLng(43.7696, 11.2558)
            locUpper.contains("MODENA") || locUpper.contains("(MO)") -> LatLng(44.6471, 10.9252)
            locUpper.contains("GENOVA") || locUpper.contains("(GE)") -> LatLng(44.4056, 8.9463)
            locUpper.contains("NAPOLI") || locUpper.contains("(NA)") -> LatLng(40.8518, 14.2681)
            locUpper.contains("VENEZIA") || locUpper.contains("(VE)") -> LatLng(45.4408, 12.3155)
            locUpper.contains("BARI") || locUpper.contains("(BA)") -> LatLng(41.1171, 16.8719)
            locUpper.contains("PALERMO") || locUpper.contains("(PA)") -> LatLng(38.1157, 13.3615)
            locUpper.contains("CATANIA") || locUpper.contains("(CT)") -> LatLng(37.5079, 15.0830)
            locUpper.contains("PADOVA") || locUpper.contains("(PD)") -> LatLng(45.4064, 11.8768)
            locUpper.contains("BRESCIA") || locUpper.contains("(BS)") -> LatLng(45.5416, 10.2118)
            locUpper.contains("MONZA") || locUpper.contains("(MB)") -> LatLng(45.5845, 9.2744)
            locUpper.contains("BERGAMO") || locUpper.contains("(BG)") -> LatLng(45.6983, 9.6773)
            else -> null
        }
    }
}

package com.mapbox.navigation.core.trip.model.eh

import androidx.annotation.StringDef

/**
 * OpenLRStandard
 */
object OpenLRStandard {
    /**
     * OpenLR standard developer by TomTom
     */
    const val TOM_TOM = "TOM_TOM"

    /**
     * Retention policy for the OpenLRStandard
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(TOM_TOM)
    annotation class Type
}

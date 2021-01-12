package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
class LaneTest : BuilderTest<Lane, Lane.Builder>() {

    override fun getImplementationClass(): KClass<Lane> =
        Lane::class

    override fun getFilledUpBuilder(): Lane.Builder {
        return Lane.Builder()
            .activeDirection("left")
            .allLanes(
                listOf(
                    LaneIndicator.Builder()
                        .directions(listOf())
                        .isActive(false)
                        .build()
                )
            )
    }

    @Test
    override fun trigger() {
        // see comments
    }
}

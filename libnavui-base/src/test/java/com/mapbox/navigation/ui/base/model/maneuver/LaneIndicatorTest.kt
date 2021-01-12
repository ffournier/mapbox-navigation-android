package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
class LaneIndicatorTest : BuilderTest<LaneIndicator, LaneIndicator.Builder>() {

    override fun getImplementationClass(): KClass<LaneIndicator> =
        LaneIndicator::class

    override fun getFilledUpBuilder(): LaneIndicator.Builder {
        return LaneIndicator.Builder()
            .directions(listOf("straight"))
            .isActive(true)
    }

    @Test
    override fun trigger() {
        // see comments
    }
}

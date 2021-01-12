package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
class DelimiterComponentNodeTest : BuilderTest<DelimiterComponentNode,
    DelimiterComponentNode.Builder>() {

    override fun getImplementationClass(): KClass<DelimiterComponentNode> =
        DelimiterComponentNode::class

    override fun getFilledUpBuilder(): DelimiterComponentNode.Builder {
        return DelimiterComponentNode.Builder()
            .text("/")
    }

    @Test
    override fun trigger() {
        // see comments
    }
}

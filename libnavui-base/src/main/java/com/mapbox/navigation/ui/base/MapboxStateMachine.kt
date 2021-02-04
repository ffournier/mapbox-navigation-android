package com.mapbox.navigation.ui.base

/**
 * Object representing a UI that subscribes to a [MapboxState] for rendering its UI.
 *
 * @param S Top class of the [MapboxState] that the [MapboxStateMachine] will be subscribing to.
 */
interface MapboxStateMachine<in S : MapboxState> {

    /**
     * Entry point for the [MapboxStateMachine] to render itself based on a [MapboxState].
     */
    fun apply(state: S)
}

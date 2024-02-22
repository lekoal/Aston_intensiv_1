package com.lekoal.astonintensiv1.model

import com.lekoal.astonintensiv1.R

object Tracks {
    fun get() = listOf(
        Song(
            title = "Metal Race",
            id = R.raw.metal_race
        ),
        Song(
            title = "Ditch Diggin",
            id = R.raw.ditch_diggin
        ),
        Song(
            title = "Marvin's Dance",
            id = R.raw.marvin_dance
        ),
        Song(
            title = "Next Funk",
            id = R.raw.next_funk
        )
    )
}
package com.foodtracker.app.ui.components

/** A corner of a connected button: fully round, or the small radius where buttons meet. */
enum class CornerKind { FULL, INNER }

data class ConnectedCorners(val topStart: CornerKind, val topEnd: CornerKind, val bottomEnd: CornerKind, val bottomStart: CornerKind)

/**
 * Corners of button [index] of [count] in a connected group: round on the group's outer edges,
 * small where it meets a neighbor; the selected button is a full pill.
 */
fun connectedCorners(index: Int, count: Int, selected: Boolean): ConnectedCorners {
    val start = if (selected || index == 0) CornerKind.FULL else CornerKind.INNER
    val end = if (selected || index == count - 1) CornerKind.FULL else CornerKind.INNER
    return ConnectedCorners(topStart = start, topEnd = end, bottomEnd = end, bottomStart = start)
}

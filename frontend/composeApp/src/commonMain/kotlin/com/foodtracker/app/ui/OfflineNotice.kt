package com.foodtracker.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foodtracker.app.ui.home.cacheAge
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.offline_not_loaded
import food_tracker_frontend.composeapp.generated.resources.offline_notice
import food_tracker_frontend.composeapp.generated.resources.offline_notice_other_day
import food_tracker_frontend.composeapp.generated.resources.offline_notice_today
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * "No connection · data from 14:32", with the date too when the copy isn't from [today]; just
 * "No connection" when what's shown never came from the backend ([refreshedAt] null).
 */
@Composable
fun offlineNotice(refreshedAt: Long?, today: LocalDate): String {
    if (refreshedAt == null) return stringResource(Res.string.offline_notice)
    val age = cacheAge(refreshedAt, today)
    val date = age.date
    return if (date == null) {
        stringResource(Res.string.offline_notice_today, age.time)
    } else {
        stringResource(Res.string.offline_notice_other_day, shortDate(date), age.time)
    }
}

/** What History and Summary say about a copy they show (offline-cache). */
sealed interface SnapshotNotice {
    /** No connection: the copy is from [refreshedAt]. */
    data class Offline(val refreshedAt: Long) : SnapshotNotice

    /** No connection and no copy of this period yet. */
    data object NotLoaded : SnapshotNotice

    /** The backend answered with an error ([text] already localized); the copy stays below it. */
    data class Error(val text: String) : SnapshotNotice
}

/** One line above the content, or in its place for [SnapshotNotice.NotLoaded]. */
@Composable
fun SnapshotNoticeText(notice: SnapshotNotice, today: LocalDate, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Text(
        when (notice) {
            is SnapshotNotice.Offline -> offlineNotice(notice.refreshedAt, today)
            SnapshotNotice.NotLoaded -> stringResource(Res.string.offline_not_loaded)
            is SnapshotNotice.Error -> notice.text
        },
        style = MaterialTheme.typography.bodyMedium,
        color = if (notice is SnapshotNotice.Error) colors.error else colors.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

## Why

Since #69, Home should open with the phone's copy of the day when the backend can't be reached, with "No connection · data from HH:mm". When the app starts from closed without a connection, Home sometimes shows the empty day instead. It shows 0 kcal, only the queued entries and "No connection" with no time, although today's copy is on the phone. The "Today's calories" widget reads the same copy and shows it, and the same happens on the build before #95.

Tracked as GitHub issue #97: https://github.com/crmartinez930130/food-tracker-ai/issues/97

## What Changes

- **The cause is a race in `DayContent`**, which is shared by Home and the day detail. When today's refresh fails offline before the copy has been read, it:
  1. checks that no day is shown yet;
  2. suspends to build the empty-day placeholder (reading the goal);
  3. assigns the placeholder without checking again.

  If the copy arrives during step 2, the placeholder overwrites it. The copy doesn't come again, since nothing changed in the database, so Home stays on the empty day. It shows up on a cold start, when the first database read is slowest.
- **The fix:** check again after the placeholder is built, so a copy that arrived meanwhile wins. The decision moves to a small function that a test can drive through that exact interleaving.
- History and Summary already read their copy before refreshing, so they don't have this race and don't change.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `offline-cache`: the offline notice requirement gains a scenario for starting from closed without a connection. The behavior it describes was already intended; this pins it down.

## Impact

- **Common (`composeApp`):**
  - `DayContent`'s offline branch;
  - a small helper next to it;
  - a test.
- **Backend:** no change.

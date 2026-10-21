## Context

`DayContent` runs two effects for the shown date:
- **The copy:** one collects `dayRepository.observe(date)`. Each copy it gets sets `shown`, `shownDate = date` and `showingPlaceholder = false`.
- **The refresh:** the other runs `dayRepository.refresh(date)`. On `Offline`, it does the following, where `offlinePlaceholder` suspends to read the cached goal:

```kotlin
if (result is RefreshResult.Offline && shownDate != date) {
    shown = dayRepository.offlinePlaceholder(date)
    shownDate = date
    showingPlaceholder = true
}
```

The check runs before that suspension, and the assignment after it. If the copy is collected during it, the assignment overwrites the copy. Room emits the copy only once while the rows don't change, so nothing corrects it later.

## Decisions

### D1: Re-check after building the placeholder
The placeholder is built first and applied only if no copy of the date arrived meanwhile. Recomposition reads the snapshot state, so the second check sees what the observer wrote during the suspension.

### D2: A small, testable helper
`placeholderIfStillMissing(isShown: () -> Boolean, build: suspend () -> CachedDay): CachedDay?`:
- returns the placeholder, or null when `isShown()` became true while building it;
- lives next to `DayContent`, in `ui/home`, in `composeApp`.

A unit test drives the race: a `build` that marks the copy as shown before returning must yield null. Two more tests cover the normal cases:
- no copy arrives, so the placeholder is used;
- the copy was already there.

`DayContent` calls the helper with `isShown = { shownDate == date }`.

*Alternative considered:* serializing both effects behind a mutex. Rejected because it's heavier, and the copy should never be blocked by a slow placeholder anyway.

## Risks / Trade-offs

- **[A copy that arrives after the placeholder is applied]** → Already handled: the observer replaces the placeholder whenever a copy comes, and sets `showingPlaceholder = false`. Only the opposite order was broken.

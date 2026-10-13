## Why

Since #43, Home shows only today. Past days need their own place: a History screen where the user sees at a glance how each day went against the goal, and opens any day to review or fix it with the same content and actions Home has. The reference is the "year in sunshine" calendar of Heliotrope and the list cards of Dollop and Tern (https://m3e.c5inco.dev/). Tracked as GitHub issue #48: https://github.com/crmartinez930130/food-tracker-ai/issues/48

## What Changes

- **New "History" tab:** the bottom navigation becomes Home · History · Summary.
- **Month calendar:**
  - Monday-first grid with the month and year, and arrows to the previous and next month;
  - the next arrow is disabled on the current month, so future months can't be reached;
  - each day is colored by how it went against the calorie goal: no meals, under (below 90%), on target (90% to 100%) or over (above 100%), with a legend;
  - today is outlined, and future days are dimmed and can't be tapped.
- **Days list below the calendar:**
  - only the shown month's days that have meals or water, newest first;
  - each day is a card with its date, kcal against the goal, a mini macro bar and its water.
- **Day detail:**
  - tapping a day in the calendar or the list opens a screen with that day's full content: calories with the percentage badge, macros, water and the meals grouped by type;
  - it reuses Home's day content, so favorite, delete, add a glass and undo work there, and water logged on a past day counts toward that day;
  - a back action, and system back, return to History on the same month;
  - opening the goal from the detail and going back returns to the detail.
- **Backend:** each entry of `GET /api/summary/range` also carries `mealCount` and `waterMl`. One request paints a whole month's calendar and list. It's an additive change: Summary ignores the new fields.
- Tapping a bar in Summary to open the detail belongs to #44. This change leaves the detail screen ready for it.

## Capabilities

### New Capabilities
- `meal-history`: the History tab with the calorie-status month calendar, the list of logged days and the day detail.

### Modified Capabilities
- `nutrition-summary`: each per-day range entry includes the day's meal count and water total.
- `app-appearance`: the bottom navigation has three destinations, Home, History and Summary.
- `nutrition-goals`: the goal screen can also be opened from a day detail, and going back returns where it was opened.

## Impact

- **Backend:**
  - `dailyTotalsBetween` adds a meal count;
  - a new `waterTotalsBetween` in the water repository;
  - `rangeSummary` merges both;
  - tests for the repository, the service and the route.
- **Frontend common:**
  - a new `ui/history/` with the screen, calendar, day cards and the pure month-grid and day-status logic, plus tests;
  - a new `DayDetailScreen`;
  - `DailyTotals` gains `mealCount` and `waterMl`;
  - `AppState`: `MainTab.HISTORY`, `Screen.DayDetail`, the History month, and the goal screen's return target;
  - `MainScreen` gets a third tab.
- **Resources:** a Material Symbols `calendar_month` icon, and the History strings in both languages.
- **Tests:** the backend tests above, the pure frontend logic and `AppStateTest` for the return targets.

# Forge Habits

Versioned independently of the launcher. Current: **1.1.0** (`BuildConfig.HABITS_VERSION`).

## 1.1.0

Keyboard-aware add/edit sheet and Pixel-like motion.

- The add/edit bottom sheet rises above the IME (`adjustResize` + `WindowCompat.setDecorFitsSystemWindows(false)` + `WindowInsets.ime.union(navigationBars)`). Name, counter, and Save/Delete stay visible; the emoji grid scrolls if the sheet is taller than the remaining space. New-habit autofocuses the name field and shows the keyboard. IME Done saves when the name is valid.
- Tasteful 150–300 ms spring motion (widget toggle, strikethrough, progress ring, manage-list `animateItem`, drag lift, edit sheet, stats stagger, Habits↔Stats shared-axis). Press scale ~0.96. Respects **Remove animations** (`animator duration scale` 0).

## 1.0.0

Daily done/not-done habits. A habit is a name (max 40 characters) and an emoji. No reminders, counts, colours, or schedules.

### Day boundary
Local midnight on the device (`TimeZone.getDefault()`, `yyyy-MM-dd`). Toggles are stored per that date. After midnight, today's switches start unchecked; earlier days stay in history.

### Delete
The habit is marked `deletedOn = today` and dropped from the widget, manage list, and per-habit stats. Completions are kept. **Stats for dates before `deletedOn` still count that habit** (so deleting today does not rewrite yesterday). Dates on or after `deletedOn` ignore it.

### Add mid-period
A habit is included only on dates `createdOn <= date`. Earlier days in the week/month are not treated as missed for that habit, and they do not change those days' overall ratio.

### Streak
A day counts toward a streak when **every habit that existed that day** was done. Days with no habits are skipped (neither a success nor a break). Current streak walks backward from today: if today is incomplete it starts at yesterday. Best streak is the longest such run in history.

### Week
Monday–Sunday. Percent is the average of daily completion ratios from Monday through today (future days excluded). Comparison is vs the previous Monday–Sunday, shown in percentage points.

### Month
Heatmap of each day's completion ratio in the current month. Future days are empty. Legend is five steps, less → more.

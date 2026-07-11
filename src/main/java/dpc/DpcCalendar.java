package dpc;

import java.util.Locale;

public final class DpcCalendar {
	public static final long TICKS_PER_YEAR = 20_000_000L;
	public static final long TICKS_PER_MONTH = 2_000_000L;
	public static final long TICKS_PER_WEEK = 400_000L;
	public static final long TICKS_PER_DAY = 80_000L;
	public static final long TICKS_PER_HOUR = 4_000L;
	public static final long TICKS_PER_MINUTE = 40L;

	public static final int MONTHS_PER_YEAR = 10;
	public static final int WEEKS_PER_MONTH = 5;
	public static final int DAYS_PER_WEEK = 5;
	public static final int HOURS_PER_DAY = 20;
	public static final int MINUTES_PER_HOUR = 100;

	private static final String[] MONTH_NAMES = {
			"Mud", "Bud", "Bro", "Ore", "Fry", "Lag", "Hog", "Boo", "Brr", "Fin"
	};
	private static final String[] DAY_NAMES = {
			"Duh", "Dig", "Doh", "Dam", "Ded"
	};

	private DpcCalendar() {
	}

	public static DateTime fromGameTime(long gameTime) {
		long safeGameTime = Math.max(0L, gameTime);
		long year = safeGameTime / TICKS_PER_YEAR + 1L;
		long ticksWithinYear = safeGameTime % TICKS_PER_YEAR;

		int monthIndex = (int) (ticksWithinYear / TICKS_PER_MONTH);
		long ticksWithinMonth = ticksWithinYear % TICKS_PER_MONTH;
		int dayOfMonth = (int) (ticksWithinMonth / TICKS_PER_DAY) + 1;
		int weekOfMonth = (dayOfMonth - 1) / DAYS_PER_WEEK + 1;

		long totalDays = safeGameTime / TICKS_PER_DAY;
		int dayIndex = (int) (totalDays % DAYS_PER_WEEK);
		long ticksWithinDay = safeGameTime % TICKS_PER_DAY;
		int hour = (int) (ticksWithinDay / TICKS_PER_HOUR);
		long ticksWithinHour = ticksWithinDay % TICKS_PER_HOUR;
		int minute = (int) (ticksWithinHour / TICKS_PER_MINUTE);
		int tickWithinMinute = (int) (ticksWithinHour % TICKS_PER_MINUTE);

		return new DateTime(
				year,
				monthIndex + 1,
				MONTH_NAMES[monthIndex],
				weekOfMonth,
				dayOfMonth,
				dayIndex + 1,
				DAY_NAMES[dayIndex],
				hour,
				minute,
				tickWithinMinute
		);
	}

	public static String formatDisplay(long gameTime) {
		return fromGameTime(gameTime).formatDisplay();
	}

	public record DateTime(
			long year,
			int month,
			String monthName,
			int weekOfMonth,
			int dayOfMonth,
			int dayOfWeek,
			String dayName,
			int hour,
			int minute,
			int tickWithinMinute
	) {
		public String formatDisplay() {
			return String.format(
					Locale.ROOT,
					"Y%d, %s-%02d, %02d:%02d",
					this.year,
					this.monthName,
					this.dayOfMonth,
					this.hour,
					this.minute
			);
		}
	}
}

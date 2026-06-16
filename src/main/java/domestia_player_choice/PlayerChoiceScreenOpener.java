package domestia_player_choice;

public final class PlayerChoiceScreenOpener {
	private static Runnable opener;

	private PlayerChoiceScreenOpener() {
	}

	public static void setOpener(Runnable screenOpener) {
		opener = screenOpener;
	}

	public static void open() {
		if (opener != null) {
			opener.run();
		}
	}
}

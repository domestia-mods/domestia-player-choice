package dpc;

public final class DpcScreenOpener {
	private static Runnable opener;

	private DpcScreenOpener() {
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

package carnero.movement.common.remotelog;

/**
 * @author carnero
 */
public enum FailureLevel {

    verbose(5, "verbose"),
    debug(10, "debug"),
    information(15, "information"),
    warning(20, "warning"),
    error(25, "error"),
    terribleFailure(30, "failure"),
    crash(1000, "crash");
    //
    private final int mLevel;
    private final String mLabel;

    private FailureLevel(int level, String label) {
        mLevel = level;
        mLabel = label;
    }

    @Override
    public String toString() {
        return mLabel;
    }

    public int toInt() {
        return mLevel;
    }

    public static FailureLevel fromInt(int level) {
        for (FailureLevel failureLevel : FailureLevel.values()) {
            if (level == failureLevel.toInt()) {
                return failureLevel;
            }
        }

        return verbose;
    }
}
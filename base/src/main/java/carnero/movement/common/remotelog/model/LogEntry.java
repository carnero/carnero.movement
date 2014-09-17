package carnero.movement.common.remotelog.model;

import carnero.movement.common.remotelog.FailureLevel;

/**
 * @author carnero
 */
public class LogEntry {

    public long time;
    public FailureLevel level;
    public String tag;
    public String message;

    public LogEntry(long time, FailureLevel level, String tag, String message) {
        this.time = time;
        this.level = level;
        this.tag = tag;
        this.message = message;
    }
}

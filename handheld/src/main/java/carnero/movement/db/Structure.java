package carnero.movement.db;

public class Structure {

    public static final String name = "cc.movement";
    public static final int version = 4;

    public static class Table {

        public static class History {

            public static final String name = "history";

            public static final String ID = "_id"; // integer
            public static final String TIME = "time"; // integer / When data was stored, not obtained
            public static final String STEPS = "steps"; // integer
            public static final String DISTANCE = "distance"; // real
            public static final String LATITUDE = "latitude"; // real
            public static final String LONGITUDE = "longitude"; // real
            public static final String ACCURACY = "accuracy"; // real

            public static final String[] projectionData = new String[]{
                TIME, STEPS, DISTANCE
            };
            public static final String[] projectionLocation = new String[]{
                TIME, LATITUDE, LONGITUDE, ACCURACY
            };
            public static final String[] projectionFull = new String[]{
                ID, TIME, STEPS, DISTANCE, LATITUDE, LONGITUDE, ACCURACY
            };
        }

        public static class Activities {

            public static final String name = "activities";

            public static final String ID = "_id"; // integer
            public static final String TYPE = "type"; // integer
            public static final String START = "act_start"; // integer, ns
            public static final String END = "act_end"; // integer, ns

            public static final String[] projectionFull = new String[]{
                ID, TYPE, START, END
            };
        }

        public static class Checkins {

            public static final String name = "checkins";

            public static final String ID = "_id"; // integer
            public static final String CHECKIN_ID = "checkin_id"; // string
            public static final String CREATED = "created"; // integer
            public static final String LATITUDE = "latitude"; // real
            public static final String LONGITUDE = "longitude"; // real
            public static final String NAME = "name"; // text
            public static final String SHOUT = "shout"; // text
            public static final String ICON_PREFIX = "icon_prefix"; // text
            public static final String ICON_SUFFIX = "icon_suffix"; // text

            public static final String[] projectionFull = new String[]{
                ID, CREATED, LATITUDE, LONGITUDE,
                NAME, SHOUT, ICON_PREFIX, ICON_SUFFIX
            };
        }
    }

    public static String getHistoryStructure() {
        StringBuilder sql = new StringBuilder();
        sql.append("create table ");
        sql.append(Table.History.name);
        sql.append(" (");
        sql.append(Table.History.ID);
        sql.append(" integer primary key autoincrement,");
        sql.append(Table.History.TIME);
        sql.append(" integer not null,");
        sql.append(Table.History.STEPS);
        sql.append(" integer not null default 0,");
        sql.append(Table.History.DISTANCE);
        sql.append(" real not null default 0,");
        sql.append(Table.History.LATITUDE);
        sql.append(" real,");
        sql.append(Table.History.LONGITUDE);
        sql.append(" real,");
        sql.append(Table.History.ACCURACY);
        sql.append(" real");
        sql.append(")");

        return sql.toString();
    }

    public static String[] getHistoryIndexes() {
        return new String[]{
            "create index if not exists idx_time on " + Table.History.name + " (" + Table.History.TIME + ")"
        };
    }

    public static String getActivitiesStructure() {
        StringBuilder sql = new StringBuilder();
        sql.append("create table ");
        sql.append(Table.Activities.name);
        sql.append(" (");
        sql.append(Table.Activities.ID);
        sql.append(" integer primary key autoincrement,");
        sql.append(Table.Activities.TYPE);
        sql.append(" integer not null,");
        sql.append(Table.Activities.START);
        sql.append(" integer,");
        sql.append(Table.Activities.END);
        sql.append(" integer");
        sql.append(")");

        return sql.toString();
    }

    public static String[] getActivitiesIndexes() {
        return new String[]{
            "create index if not exists idx_start_end on " + Table.Activities.name
                + " (" + Table.Activities.START + ", " + Table.Activities.END + ")"
        };
    }

    public static String getCheckinsStructure() {
        StringBuilder sql = new StringBuilder();
        sql.append("create table ");
        sql.append(Table.Checkins.name);
        sql.append(" (");
        sql.append(Table.Checkins.ID);
        sql.append(" integer primary key autoincrement,");
        sql.append(Table.Checkins.CHECKIN_ID);
        sql.append(" text unique not null,");
        sql.append(Table.Checkins.CREATED);
        sql.append(" integer not null,");
        sql.append(Table.Checkins.LATITUDE);
        sql.append(" real not null,");
        sql.append(Table.Checkins.LONGITUDE);
        sql.append(" real not null,");
        sql.append(Table.Checkins.NAME);
        sql.append(" text, ");
        sql.append(Table.Checkins.SHOUT);
        sql.append(" text, ");
        sql.append(Table.Checkins.ICON_PREFIX);
        sql.append(" text, ");
        sql.append(Table.Checkins.ICON_SUFFIX);
        sql.append(" text");
        sql.append(")");

        return sql.toString();
    }

    public static String[] getCheckinsIndexes() {
        return new String[]{
            "create index if not exists idx_created on " + Table.Checkins.name + " (" + Table.Checkins.CREATED + ")"
        };
    }
}

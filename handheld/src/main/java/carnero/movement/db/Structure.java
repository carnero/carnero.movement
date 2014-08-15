package carnero.movement.db;

public class Structure {

	public static final String name = "cc.movement";
	public static final int version = 1;

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

			public static final String[] projection = new String[] {
					ID, TIME, STEPS, DISTANCE, LATITUDE, LONGITUDE, ACCURACY
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

	public static String[] getStructureIndexes() {
		return new String[] {
				"create index if not exists idx_time on " + Table.History.name + " (" + Table.History.TIME + ")"
		};
	}
}

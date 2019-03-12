package click.dummer.spotifine;

import android.provider.BaseColumns;

public class EntryContract {

    public static final String AUTHORITY = "click.dummer.spotifine.contentprovider";

    public static class DbEntry implements BaseColumns {
        public static final String TABLE_NAME = "entries";

        public static final String COLUMN_Title = "e_title";
        public static final String COLUMN_Artist = "e_artist";
        public static final String COLUMN_Url = "e_url";
        public static final String COLUMN_Duration = "e_duration";
        public static final String COLUMN_Randomness = "e_randomness";
    }

    // Useful SQL query parts
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String LONG_TYPE = " LONG";
    private static final String DOUBLE_TYPE = " DOUBLE";
    private static final String COMMA_SEP = ",";

    public static final String DEFAULT_SORTORDER = DbEntry.COLUMN_Randomness +" DESC";

    // Useful SQL queries
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + DbEntry.TABLE_NAME + " (" +
                    DbEntry._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    DbEntry.COLUMN_Title + TEXT_TYPE + COMMA_SEP +
                    DbEntry.COLUMN_Artist + TEXT_TYPE + COMMA_SEP +
                    DbEntry.COLUMN_Url + TEXT_TYPE + COMMA_SEP +
                    DbEntry.COLUMN_Randomness + DOUBLE_TYPE + COMMA_SEP +
                    DbEntry.COLUMN_Duration + LONG_TYPE + " )";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + DbEntry.TABLE_NAME;

    public static final String[] projection = {
            DbEntry._ID,
            DbEntry.COLUMN_Title,
            DbEntry.COLUMN_Artist,
            DbEntry.COLUMN_Url,
            DbEntry.COLUMN_Randomness,
            DbEntry.COLUMN_Duration
    };
}

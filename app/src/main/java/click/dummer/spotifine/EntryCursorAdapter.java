package click.dummer.spotifine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class EntryCursorAdapter extends CursorAdapter {
    MainActivity activity;

    public EntryCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        activity = (MainActivity) context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.entry_line, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView ta = (TextView) view.findViewById(R.id.line_artist);
        TextView tb = (TextView) view.findViewById(R.id.line_title);
        TextView td = (TextView) view.findViewById(R.id.line_dur);
        ProgressBar pb = (ProgressBar) view.findViewById(R.id.progressBar);

        int randomness = (int) Math.round(
                cursor.getDouble(cursor.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Randomness)) * 10.0
        );

        ta.setText(cursor.getString(cursor.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Artist)));
        int mins = (int) Math.floor(cursor.getLong(cursor.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Duration))/60000);
        int secs = Math.round(cursor.getLong(cursor.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Duration))/1000 - 60*mins);
        String nu = "";
        if (secs < 10) nu = "0";
        td.setText(String.valueOf(mins) + ":" + nu + String.valueOf(secs));

        pb.setProgress(randomness/10);
        tb.setText(cursor.getString(cursor.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Title)) + "\n" + cursor.getString(cursor.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Url)));

        if (cursor.getPosition()%2==0) {
            view.setBackgroundColor(ContextCompat.getColor(
                    activity.getApplicationContext(),
                    R.color.evenCol
            ));
        } else {
            view.setBackgroundColor(ContextCompat.getColor(
                    activity.getApplicationContext(),
                    R.color.oddCol
            ));
        }
    }
}
package click.dummer.spotifine;

import android.app.AlarmManager;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.PlayerState;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    final String SOME_ACTION = "click.dummer.spotifine.MainActivity.AlarmReceiver";
    final String PROJECT_LINK = "https://github.com/no-go/SpotiFine";
    final double MOD_UP = 1.2;
    final double MOD_DOWN = 0.8;
    final double MOD_FLOOR = 10.0;

    private String CLIENT_ID = "7319233048a44d439c82571a1c903d01";
    private static final String REDIRECT_URI = "comspotifytestsdk://callback";

    private SpotifyAppRemote mSpotifyAppRemote;
    private ListView entryList;
    private EntryCursorAdapter entryCursorAdapter;

    private AlarmManager alarmManager;
    private AlarmReceiver alarmReceiver = null;
    private PendingIntent pendingIntent;

    public class Entry {
        public String title;
        public String url;
        public long duration;
    }
    private ArrayList<Entry> magicList;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_project:
                Intent intentProj = new Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_LINK));
                startActivity(intentProj);
                break;
            case R.id.action_random:
                nextPress(getApplicationContext());
                break;
            case R.id.action_add:
                readIt();
                break;
            case R.id.action_pause:
                pausePress();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.popup, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) entryCursorAdapter.getItem(info.position);

        double randomness = Math.round(
                cursor.getDouble(cursor.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Randomness)) * 10.0
        )/10.0;

        int j = item.getItemId();
        if (j == R.id.action_del) {
            getContentResolver().delete(
                    EntryContentProvider.CONTENT_URI,
                    EntryContract.DbEntry.COLUMN_Url + "=?",
                    new String[]{
                            cursor.getString(cursor.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Url))
                    }
            );
            return true;
        } else if (j == R.id.action_loosy) {
            ContentValues values = new ContentValues();
            randomness = randomness * MOD_DOWN;
            if (randomness < MOD_FLOOR) randomness = MOD_FLOOR;
            values.put(EntryContract.DbEntry.COLUMN_Randomness, randomness);
            getContentResolver().update(
                    EntryContentProvider.CONTENT_URI,
                    values,
                    EntryContract.DbEntry.COLUMN_Url + "=?",
                    new String[]{
                            cursor.getString(cursor.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Url))
                    }
            );
            return true;
        } else if (j == R.id.action_horny) {
            ContentValues values = new ContentValues();
            randomness = randomness * MOD_UP;
            if (randomness > 100.0) randomness = 100.0;
            values.put(EntryContract.DbEntry.COLUMN_Randomness, randomness);
            getContentResolver().update(
                    EntryContentProvider.CONTENT_URI,
                    values,
                    EntryContract.DbEntry.COLUMN_Url + "=?",
                    new String[]{
                            cursor.getString(cursor.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Url))
                    }
            );
            return true;
        } else if (j == R.id.action_play) {
            if (alarmReceiver != null) unregisterReceiver(alarmReceiver);
            alarmReceiver = new AlarmReceiver();
            IntentFilter intentFilter = new IntentFilter(SOME_ACTION);
            registerReceiver(alarmReceiver, intentFilter);

            Intent myIntent = new Intent(SOME_ACTION);
            pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, myIntent, 0);
            long dur = cursor.getLong(cursor.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Duration));
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + dur,
                    pendingIntent
            );

            playPress(cursor.getString(cursor.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Url)));
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            if (inTimeSpan(6, 18)) {
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            recreate();
        }
        setContentView(R.layout.activity_main);

        entryList = (ListView) findViewById(R.id.listView);
        registerForContextMenu(entryList);
        getLoaderManager().initLoader(0, null, this);
        entryCursorAdapter = new EntryCursorAdapter(this,null, 0);
        entryList.setAdapter(entryCursorAdapter);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);

        SpotifyAppRemote.setDebugMode(true);
    }

    public void playPress(String url) {
        mSpotifyAppRemote.getPlayerApi().play(url);
    }

    public void pausePress() {
        if (alarmReceiver != null) {
            unregisterReceiver(alarmReceiver);
            alarmReceiver =null;
        }
        mSpotifyAppRemote.getPlayerApi().pause();
    }

    public void nextPress(Context context) {
        Cursor c = getContentResolver().query(
                EntryContentProvider.CONTENT_URI,
                EntryContract.projection,
                "",
                null,
                EntryContract.DEFAULT_SORTORDER
        );

        magicList = new ArrayList<>();
        while (c.moveToNext()) {
            Entry e = new Entry();
            e.title = c.getString(c.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Title));
            e.duration = c.getLong(c.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Duration));
            e.url = c.getString(c.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Url));
            int randomness = (int) c.getDouble(c.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Randomness));
            for (int i=0 ; i<randomness; i++) {
                magicList.add(e);
            }
        }
        int id = (int) (Math.random() * magicList.size());

        if (alarmReceiver != null) unregisterReceiver(alarmReceiver);
        alarmReceiver = new AlarmReceiver();
        IntentFilter intentFilter = new IntentFilter(SOME_ACTION);
        registerReceiver(alarmReceiver, intentFilter);

        Intent myIntent = new Intent(SOME_ACTION);
        pendingIntent = PendingIntent.getBroadcast(context, 0, myIntent, 0);

        alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + magicList.get(id).duration,
                pendingIntent
        );
        //Log.d("xxxxxxxxxxxx", String.valueOf(magicList.get(id).duration));
        Toast.makeText(context, magicList.get(id).title, Toast.LENGTH_LONG).show();
        playPress(magicList.get(id).url);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SpotifyAppRemote.connect(
                getApplication(),
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build(),
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        Toast.makeText(MainActivity.this, "you fail", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    private void readIt() {
        if (mSpotifyAppRemote == null) return;
        mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(new CallResult.ResultCallback<PlayerState>() {
            @Override
            public void onResult(PlayerState playerState) {
                if (playerState == null) return;
                if (playerState.track == null) return;

                modRand(MOD_DOWN);

                ContentValues values = new ContentValues();
                values.put(EntryContract.DbEntry.COLUMN_Title, playerState.track.name);
                values.put(EntryContract.DbEntry.COLUMN_Artist, playerState.track.artist.name);
                values.put(EntryContract.DbEntry.COLUMN_Url, playerState.track.uri);
                values.put(EntryContract.DbEntry.COLUMN_Duration, playerState.track.duration);
                values.put(EntryContract.DbEntry.COLUMN_Randomness, 100.0);
                getContentResolver().insert(EntryContentProvider.CONTENT_URI, values);
            }
        });
    }

    // @todo SQL is a bi bad. should be 1 statement
    private void modRand(double mod) {
        double randomness;
        Cursor c = getContentResolver().query(
                EntryContentProvider.CONTENT_URI,
                EntryContract.projection,
                "",
                null,
                EntryContract.DEFAULT_SORTORDER
        );

        while (c.moveToNext()) {
            ContentValues values = new ContentValues();
            randomness = c.getDouble(c.getColumnIndexOrThrow(EntryContract.DbEntry.COLUMN_Randomness));
            randomness = randomness * mod;
            if (randomness > 100.0) randomness = 100.0;
            if (randomness < MOD_FLOOR) randomness = MOD_FLOOR;
            values.put(EntryContract.DbEntry.COLUMN_Randomness, randomness);
            getContentResolver().update(
                    EntryContentProvider.CONTENT_URI,
                    values,
                    EntryContract.DbEntry._ID + "=" + c.getInt(c.getColumnIndexOrThrow(EntryContract.DbEntry._ID)),
                    null
            );
        }

    }

    @Override
    public android.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                this,
                EntryContentProvider.CONTENT_URI,
                EntryContract.projection,
                null,
                null,
                EntryContract.DEFAULT_SORTORDER
        );
    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data) {
        if (entryCursorAdapter != null) {
            entryCursorAdapter.swapCursor(data);
            entryList.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> loader) {
        if (entryCursorAdapter != null) {
            entryCursorAdapter.swapCursor(null);
            entryList.setVisibility(View.VISIBLE);
        }
    }

    public class AlarmReceiver extends WakefulBroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            nextPress(context);
        }
    }

    public static boolean inTimeSpan(int startH, int stopH) {
        int nowH = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (startH == stopH && startH == nowH) return true;
        if (startH > stopH && (nowH <= stopH || nowH >= startH)) return true;
        if (startH < stopH && nowH >= startH && nowH <= stopH) return true;
        return false;
    }
}

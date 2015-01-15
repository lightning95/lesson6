package ru.ifmo.md.lesson6;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

public class ChannelsActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        MyContentObserver.Callbacks, ShakeEventManager.ShakeListener, MyResultReceiver.Receiver {
    private static final int LOADER_CHANNELS = 1;
    private CursorAdapter cursorAdapter;
    private EditText editText;
    private MyContentObserver myContentObserver;
    private MyResultReceiver myResultReceiver;
    private ShakeEventManager shakeEventManager;
    private ProgressBar progressBar;

    public final static int TITLE_COLOR = Color.BLUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_channels);

        editText = (EditText) findViewById(R.id.editText);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                int actionAdd = getApplicationContext().getResources().getInteger(R.integer.actionAdd);
                if (actionId == actionAdd) {
                    String potentialUrl = editText.getText().toString();
                    try {
                        new URL(potentialUrl);
                    } catch (MalformedURLException e) {
                        Toast.makeText(getApplicationContext(), "Bad link address", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    addChannel(potentialUrl);
                    return true;
                }
                return false;
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        cursorAdapter = new CursorAdapter(this, null, true) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
                return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, viewGroup, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                ((TextView) view.findViewById(android.R.id.text1)).setTextColor(TITLE_COLOR);
                ((TextView) view.findViewById(android.R.id.text1)).setText(cursor.getString(
                        cursor.getColumnIndex(DbContract.Channels.CHANNEL_TITLE)));

                ((TextView) view.findViewById(android.R.id.text2)).setText(
                        cursor.getString(cursor.getColumnIndex(DbContract.Channels.CHANNEL_LINK)));
            }
        };

        setListAdapter(cursorAdapter);
        registerForContextMenu(findViewById(android.R.id.list));
        getLoaderManager().initLoader(LOADER_CHANNELS, null, this);

        shakeEventManager = new ShakeEventManager();
        shakeEventManager.setListener(this);
        shakeEventManager.init(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        shakeEventManager.register();

        myResultReceiver = new MyResultReceiver(new Handler());
        myResultReceiver.setReceiver(this);
        if (myContentObserver == null) {
            myContentObserver = new MyContentObserver(this);
        }
        getContentResolver().registerContentObserver(
                DbContract.Channels.CONTENT_URI, true, myContentObserver);
        getLoaderManager().initLoader(LOADER_CHANNELS, null, this).forceLoad();
    }

    @Override
    public void onPause() {
        super.onPause();
        shakeEventManager.deregister();

        myResultReceiver.setReceiver(null);
        getContentResolver().unregisterContentObserver(myContentObserver);
        if (myContentObserver != null) {
            myContentObserver = null;
        }
    }

    private void addChannel(String url) {
        MyLoaderService.startActionAddChannel(getApplicationContext(), url, myResultReceiver);
    }

    private void refreshAllChannels() {
        MyLoaderService.startActionLoadAll(getApplicationContext(), myResultReceiver);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onListItemClick(ListView lv, View v, int position, long id) {
        Cursor cursor = (Cursor) cursorAdapter.getItem(position);
        long channelId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

        Intent intent = new Intent(this, ChannelActivity.class);
        intent.putExtra(ChannelActivity.EXTRA_CHANNEL_ID, channelId);
        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.channels_context, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo acmi =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int i = item.getItemId();
        if (i == R.id.delete) {
            Cursor cursor = (Cursor) getListAdapter().getItem(acmi.position);
            final String channelId = cursor.getString(cursor.getColumnIndex(DbContract.Channels._ID));
            Log.d("TAG", "delete channel #" + channelId);
            getContentResolver().delete(DbContract.Channels.buildChannelUri(channelId), null, null);
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_channels, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh_all) {
            refreshAllChannels();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(this, DbContract.Channels.CONTENT_URI, DbContract.Channels.ALL_COLUMNS,
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        cursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    @Override
    public void onChannelsObserverFired() {
        getLoaderManager().initLoader(LOADER_CHANNELS, null, this).forceLoad();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle data) {
        progressBar.setVisibility(View.GONE);

        if (resultCode == MyLoaderService.RESULT_FAIL) {
            Toast.makeText(this, "Error while update, try again.", Toast.LENGTH_SHORT).show();
        } else if (resultCode == MyLoaderService.RESULT_BAD_CHANNEL) {
            Toast.makeText(this, "Invalid RSS url", Toast.LENGTH_SHORT).show();
        } else if (resultCode == MyLoaderService.RESULT_OK) {
            int newPosts = data.getInt(MyLoaderService.EXTRA_NEW_POSTS, 0);
            if (newPosts > 0) {
                Toast.makeText(this, "You have " + newPosts + " new posts", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == MyLoaderService.RESULT_NO_INTERNET) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        } else if (resultCode == MyLoaderService.RESULT_ALREADY_EXISTS) {
//            false
            Toast.makeText(this, "Such feed already exists", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onShake() {
        refreshAllChannels();
    }
}

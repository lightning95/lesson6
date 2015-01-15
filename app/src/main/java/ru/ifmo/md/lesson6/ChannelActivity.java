package ru.ifmo.md.lesson6;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ChannelActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        MyContentObserver.Callbacks, MyResultReceiver.Receiver, ShakeEventManager.ShakeListener {

    public static final String EXTRA_CHANNEL_ID = "ru.ifmo.md.lesson6.extra.CHANNEL_ID";
    private static final int LOADER_POSTS = 1;
    private static final int LOADER_CHANNEL_INFO = 2;

    private CursorAdapter cursorAdapter;
    private long channelId;
    private MyContentObserver myContentObserver;
    private MyResultReceiver myResultReceiver;
    private ProgressBar progressBar;
    private ShakeEventManager shakeEventManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);

        Intent intent = getIntent();
        channelId = intent.getLongExtra(EXTRA_CHANNEL_ID, -1);
        if (channelId == -1) {
            finish();
        }

        progressBar = (ProgressBar) findViewById(R.id.progressBar2);

        cursorAdapter = new CursorAdapter(this, null, true) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
                return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2,
                        viewGroup, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                ((TextView) view.findViewById(android.R.id.text1)).setTextColor(ChannelsActivity.TITLE_COLOR);
                final String title = cursor.getString(cursor.getColumnIndex(DbContract.Posts.POST_TITLE));
                ((TextView) view.findViewById(android.R.id.text1)).setText(title);

                String desc = cursor.getString(cursor.getColumnIndex(DbContract.Posts.POST_DESCRIPTION));
                if (desc == null) {
                    desc = "";
                }
                ((TextView) view.findViewById(android.R.id.text2)).setText(Html.fromHtml(desc));
            }
        };

        setListAdapter(cursorAdapter);

        registerForContextMenu(findViewById(android.R.id.list));

        getLoaderManager().initLoader(LOADER_POSTS, null, this);
        getLoaderManager().initLoader(LOADER_CHANNEL_INFO, null, this);

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
                DbContract.Posts.CONTENT_URI, true, myContentObserver);
        getLoaderManager().initLoader(LOADER_POSTS, null, this).forceLoad();
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

    @Override
    protected void onListItemClick(ListView lv, View v, int position, long id) {
        Cursor cursor = (Cursor) cursorAdapter.getItem(position);
        final String url = cursor.getString(cursor.getColumnIndex(DbContract.Posts.POST_LINK));
        final String title = cursor.getString(cursor.getColumnIndex(DbContract.Posts.POST_TITLE));

        Intent intent = new Intent(this, PostActivity.class);
        intent.putExtra(PostActivity.EXTRA_URL, url);
        intent.putExtra(PostActivity.EXTRA_TITLE, title);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_channel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            refreshChannel();
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        if (id == LOADER_POSTS) {
            return new CursorLoader(this, DbContract.Posts.buildPostsUri(Long.toString(channelId)),
                    DbContract.Posts.ALL_COLUMNS, null, null, DbContract.Posts._ID + " ASC");
        } else if (id == LOADER_CHANNEL_INFO) {
            return new CursorLoader(this, DbContract.Channels.buildChannelUri(Long.toString(channelId)),
                    DbContract.Channels.ALL_COLUMNS,null, null, null);
        } else {
            throw new UnsupportedOperationException("Unknown loader: " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        int i = cursorLoader.getId();
        if (i == LOADER_POSTS) {
            cursorAdapter.swapCursor(cursor);
        } else if (i == LOADER_CHANNEL_INFO) {
            cursor.moveToFirst();
            final String title = cursor.getString(cursor.getColumnIndex(DbContract.Channels.CHANNEL_TITLE));
            setTitle(title);
        } else {
            throw new UnsupportedOperationException("Unknown loader: " + cursorLoader.getId());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    @Override
    public void onChannelsObserverFired() {
        getLoaderManager().initLoader(LOADER_POSTS, null, this).forceLoad();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle data) {
        progressBar.setVisibility(View.GONE);

        if (resultCode == MyLoaderService.RESULT_FAIL) {
            Toast.makeText(this, "Error while update, try again.", Toast.LENGTH_SHORT).show();
        } else if (resultCode == MyLoaderService.RESULT_OK) {
            int newPosts = data.getInt(MyLoaderService.EXTRA_NEW_POSTS, 0);
            if (newPosts > 0) {
                Toast.makeText(this, "You have " + newPosts + " new posts", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == MyLoaderService.RESULT_NO_INTERNET) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onShake() {
        refreshChannel();
    }

    private void refreshChannel() {
        progressBar.setVisibility(View.VISIBLE);
        MyLoaderService.startActionLoadOne(this, channelId, myResultReceiver);
    }
}
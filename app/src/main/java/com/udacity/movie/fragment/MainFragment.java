package com.udacity.movie.fragment;


import android.database.Cursor;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.udacity.movie.MyApplication;
import com.udacity.movie.R;
import com.udacity.movie.adapter.MoviesGridAdapter;
import com.udacity.movie.data.MovieContract.MovieEntry;
import com.udacity.movie.model.MovieInfo;
import com.udacity.movie.sync.MovieSyncAdapter;
import com.udacity.movie.utils.NetWorkUtils;
import com.udacity.movie.utils.Utility;

/**
 */
public class MainFragment extends Fragment implements LoaderCallbacks<Cursor>{

    private final String TAG = this.getClass().getSimpleName();
    private static final int MOVIE_LOADER_ID = 0;

    // 记住滚动位置
    private static final String SELECTED_KEY = "selected_position";
    private int mPosition = GridView.INVALID_POSITION;

    private GridView gridView;
    private View rootView;

    private MoviesGridAdapter moviesAdapter;

    private TextView tvEmptyMessage;

    public interface MovieItemClickCallback {
        void onItemSelected(MovieInfo movieInfo);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @RequiresApi(api = VERSION_CODES.HONEYCOMB)
    @Override
    public void onResume() {
        super.onResume();

        MyApplication.favoredMovieId = Utility.getFavoredMoviesPreference(getActivity());
        getLoaderManager().restartLoader(MOVIE_LOADER_ID, null, this);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);
        gridView = (GridView) rootView.findViewById(R.id.grid_for_movies);
        tvEmptyMessage = (TextView) rootView.findViewById(R.id.tv_empty_message);

        moviesAdapter = new MoviesGridAdapter(getActivity(), null, 0);
        gridView.setAdapter(moviesAdapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = VERSION_CODES.HONEYCOMB)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);

                MovieInfo movieInfo;
                if (cursor != null) {
                    movieInfo = MovieInfo.getMovieInfo(cursor);

                    ((MovieItemClickCallback)getActivity())
                            .onItemSelected(movieInfo);
                }

                mPosition = position;
            }
        });

        if (null != savedInstanceState &&
                savedInstanceState.containsKey(SELECTED_KEY)) {
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        return rootView;
    }

    private void setEmptyMessage (String message) {
        tvEmptyMessage.setVisibility(View.VISIBLE);
        tvEmptyMessage.setText(message);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(MOVIE_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != GridView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_main_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mPosition = GridView.SCROLLBAR_POSITION_DEFAULT;
        switch (item.getItemId()) {
            case R.id.action_most_popular:
                MyApplication.uri = MovieEntry.CONTENT_URI;
                Utility.putSortOrderPreference(getActivity(), MovieEntry.COLUMN_POPUlARITY);
                onSortOrderChanged();
                break;
            case R.id.action_top_rated:
                MyApplication.uri = MovieEntry.CONTENT_URI;
                Utility.putSortOrderPreference(getActivity(), MovieEntry.COLUMN_VOTE_AVERAGE);
                onSortOrderChanged();
                break;
            case R.id.action_favored:
                MyApplication.uri = MovieEntry.buildMovieFavored();
                getLoaderManager().restartLoader(MOVIE_LOADER_ID, null, this);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void onSortOrderChanged () {
        updateMovie();
        getLoaderManager().restartLoader(MOVIE_LOADER_ID, null, this);
    }

    private void updateMovie ( ) {
        MovieSyncAdapter.syncImmediately(getActivity());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = Utility.getPreferredSortOrder(getActivity());
        String orderKey = Utility.getOrderUrlKey(getActivity());
        String[] selection = new String[] {orderKey};
        return new CursorLoader(
                getActivity(),
                MyApplication.uri,
                null,
                null,
                selection,
                sortOrder + " DESC"
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (!NetWorkUtils.isNetWorkAvailable(getActivity()) && data.getCount() == 0) {
            setEmptyMessage(getString(R.string.no_internet_connection));
            return;
        } else if (MyApplication.uri.equals(MovieEntry.buildMovieFavored()) && data.getCount() == 0) {
            setEmptyMessage(getString(R.string.no_favored_movies));
            return;
        } else {
            tvEmptyMessage.setVisibility(View.GONE);
        }

        moviesAdapter.swapCursor(data);
        if (mPosition != GridView.INVALID_POSITION) {
            gridView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.e(TAG, "onLoaderReset");
        moviesAdapter.swapCursor(null);
    }

}

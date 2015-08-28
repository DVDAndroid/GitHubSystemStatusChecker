package com.dvd.android.githubsystemstatuschecker;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	private static List<Status> statusList = new ArrayList<>();
	private RecyclerView mRecyclerView;
	private SwipeRefreshLayout mSwipeRefresh;
	private StatusAdapter adapter;
	private View mainView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mainView = findViewById(R.id.mainView);
		setSupportActionBar((Toolbar) findViewById(R.id.toolBar));

		mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
		mSwipeRefresh = (SwipeRefreshLayout) findViewById(
				R.id.swiperefreshlayout);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

		adapter = new StatusAdapter(this, statusList);
		mSwipeRefresh.setColorSchemeResources(R.color.colorPrimary);
		mSwipeRefresh.setOnRefreshListener(
				new SwipeRefreshLayout.OnRefreshListener() {
					@Override
					public void onRefresh() {
						refresh();
					}
				});

		refresh();
	}

	private void refresh() {
		statusList.clear();
		String urlApi = "https://status.github.com/api/messages.json";
		new DownloadTask(this, urlApi).execute();
	}

	static class Status {

		protected String message;
		protected String date;
		protected StatusID status;

		public Status(String message, String date, String status) {
			this.message = message;
			this.date = date;
			this.status = StatusID.valueOf("STATUS_" + status.toUpperCase());
		}

		public String getMessage() {
			return message;
		}

		public String getDate() {
			return date;
		}

		public StatusID getStatus() {
			return status;
		}

		public enum StatusID {
			STATUS_GOOD, STATUS_MINOR, STATUS_MAJOR
		}

	}

	public class StatusAdapter
			extends RecyclerView.Adapter<StatusAdapter.StatusViewHolder> {

		private final AppCompatActivity activity;

		public StatusAdapter(AppCompatActivity activity,
				List<Status> statusList) {
			this.activity = activity;
			MainActivity.statusList = statusList;
		}

		@Override
		public int getItemCount() {
			return statusList.size();
		}

		@Override
		public StatusViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
			View v = LayoutInflater.from(viewGroup.getContext())
					.inflate(R.layout.card_row, viewGroup, false);
			return new StatusViewHolder(v);
		}

		@Override
		public void onBindViewHolder(StatusViewHolder statusViewHolder, int i) {
			Status status = statusList.get(i);
			int color = 0;
			float textSize = 0;

			switch (status.getStatus()) {
				case STATUS_MAJOR:
					color = R.color.error;
					textSize = 35;
					break;
				case STATUS_MINOR:
					color = R.color.warning;
					textSize = 30;
					break;
				case STATUS_GOOD:
					color = R.color.good;
					textSize = 25;
					break;
			}

			statusViewHolder.cardView.setCardBackgroundColor(getColor(color));
			statusViewHolder.message.setText(status.getMessage());
			statusViewHolder.message.setTextSize(textSize);
			statusViewHolder.date.setText(
					String.format(getString(R.string.date), status.getDate()));

			if (i == 0) {
				assert activity.getSupportActionBar() != null;
				activity.getSupportActionBar().setBackgroundDrawable(
						new ColorDrawable(getColor(color)));

				if (Build.VERSION.SDK_INT >= 21) {
					activity.getWindow().setStatusBarColor(
							darkenColor(getColor(color), 0.85f));
				}
			}
		}

		public int darkenColor(int color, float factor) {
			float[] hsv = new float[3];
			Color.colorToHSV(color, hsv);
			hsv[2] *= factor;
			return Color.HSVToColor(hsv);
		}

		@Override
		public void onAttachedToRecyclerView(RecyclerView recyclerView) {
			super.onAttachedToRecyclerView(recyclerView);
		}

		public class StatusViewHolder extends RecyclerView.ViewHolder {
			CardView cardView;
			TextView message;
			TextView date;

			StatusViewHolder(View itemView) {
				super(itemView);
				cardView = (CardView) itemView.findViewById(R.id.card);
				message = (TextView) itemView.findViewById(R.id.message_status);
				date = (TextView) itemView.findViewById(R.id.date_status);
			}
		}

	}

	private class DownloadTask extends AsyncTask<Void, Integer, Boolean> {

		private final String url;
		private final Context context;
		private ProgressDialog progressDialog;

		public DownloadTask(Context context, String url) {
			this.context = context;
			this.url = url;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			progressDialog = ProgressDialog.show(context, null,
					getString(R.string.loading), true);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				BufferedReader bufferReader = new BufferedReader(
						new InputStreamReader(new URL(url).openStream()));
				String stringBuffer;
				String json = "";
				while ((stringBuffer = bufferReader.readLine()) != null) {
					json += stringBuffer;
				}
				bufferReader.close();

				JSONArray array = new JSONArray(json);
				final int n = array.length();
				for (int i = 0; i < n; ++i) {
					final JSONObject status = array.getJSONObject(i);

					String message = status.getString("body");
					String date = status.getString("created_on");
					String state = status.getString("status");

					statusList
							.add(new MainActivity.Status(message, date, state));
				}
			} catch (IOException e) {
				return false;
			} catch (JSONException e) {
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);

			progressDialog.dismiss();

			if (mSwipeRefresh.isRefreshing()) {
				mSwipeRefresh.setRefreshing(false);
			}

			if (result) {
				mRecyclerView.setAdapter(adapter);
			} else {
				assert getSupportActionBar() != null;
				getSupportActionBar().setBackgroundDrawable(
						new ColorDrawable(getColor(R.color.colorPrimary)));

				if (Build.VERSION.SDK_INT >= 21)
					getWindow().setStatusBarColor(
							getColor(R.color.colorPrimaryDark));

				Snackbar.make(mainView, R.string.no_internet,
						Snackbar.LENGTH_LONG)
						.setAction(R.string.retry, new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								refresh();
							}
						}).show();
			}
		}
	}
}
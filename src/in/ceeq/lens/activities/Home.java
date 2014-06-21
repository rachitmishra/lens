package in.ceeq.lens.activities;

import in.ceeq.lens.R;
import in.ceeq.lens.commons.Constants;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class Home extends Activity {

	private FragmentManager fragmentManager;
	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;

	private CharSequence drawerTitle;
	private CharSequence title;

	private boolean exit = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.title = drawerTitle = getTitle();
		setupActionbar();
		setupDrawer();
	}

	public void setupActionbar() {
		getActionBar().setDisplayShowTitleEnabled(false);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
	}

	public void setupDrawer() {

		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_navigation_drawer,
				R.string.open_drawer, R.string.close_drawer) {

			public void onDrawerClosed(View view) {
				getActionBar().setTitle(title);
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(drawerTitle);
			}

			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
				super.onDrawerSlide(drawerView, slideOffset);
				drawerLayout.bringChildToFront(drawerView);
				drawerLayout.requestLayout();
			}

		};
		drawerLayout.setDrawerListener(mDrawerToggle);

	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
	    super.onPostCreate(savedInstanceState);
	    mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_home, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		// checkPlayServices();
	}

	@Override
	public void onBackPressed() {
		if (exit)
			Home.this.finish();
		else {
			Toast.makeText(this, "Press Back again to Exit.", Toast.LENGTH_SHORT).show();
			exit = true;
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					exit = false;
				}
			}, 3 * 1000);

		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		Intent captureIntent = new Intent(this, Editer.class);
		switch (item.getItemId()) {
		case R.id.action_capture:
			captureIntent.setAction(Constants.ACTION_IMAGE_CAPTURE);
			startActivity(captureIntent);
			break;
		case R.id.action_select:
			captureIntent.setAction(Constants.ACTION_IMAGE_SELECT);
			startActivity(captureIntent);
			break;

		}
		return false;
	}
}

package com.notxx.icon;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import top.trumeet.common.cache.IconCache;
import top.trumeet.common.utils.ImgUtils;

public class MainActivity extends Activity {
	private class PackagesAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
		private final IconCache cache = IconCache.getInstance();
		private final PackageManager manager;
		private final List<PackageInfo> packages;
		private final Context context;

		PackagesAdapter(PackageManager manager, List<PackageInfo> packages, Context context) {
			this.manager = manager;
			this.packages = packages;
			this.context = context;
		}

		public void onItemClick(AdapterView parent, View v, int position, long id) {
			PackageInfo info = getItem(position);
			// Log.d("SmallIcon", "item click");
			Notification.Builder n = new Notification.Builder(MainActivity.this, CHANNEL_ID)
					.setSmallIcon(R.drawable.default_notification_icon)
					.setContentTitle(manager.getApplicationLabel(info.applicationInfo))
					.setContentText(info.packageName);
			Icon iconCache = cache.getIconCache(MainActivity.this, info.packageName);
			if (iconCache != null) { n.setSmallIcon(iconCache); }
			int color = cache.getAppColor(MainActivity.this, info.packageName);
			if (color != -1) {
				n.setColor(color);
				n.setColorized(false);
			}
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			notificationManager.notify(notificationId++, n.build());
		}

		@Override
		public int getCount() {
			return packages.size();
		}

		@Override
		public PackageInfo getItem(int position) {
			return packages.get(position);
		}

		@Override
		public long getItemId(int position) {
			return packages.get(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = null;

			// 如果convertView不为空则复用
			if (convertView == null) {
				view = View.inflate(MainActivity.this, R.layout.app_info, null);
			}else {
				view = convertView;
			}

			PackageInfo info = getItem(position);
			// applicationLabel
			TextView appName = (TextView) view.findViewById(R.id.appName);
			appName.setText(manager.getApplicationLabel(info.applicationInfo));
			// applicationIcon
			ImageView appIcon = (ImageView) view.findViewById(R.id.appIcon);
			appIcon.setImageDrawable(manager.getApplicationIcon(info.applicationInfo));
			// applicationLogo
			ImageView appLogo = (ImageView) view.findViewById(R.id.appLogo);
			appLogo.setImageDrawable(manager.getApplicationLogo(info.applicationInfo));
			// embedIcon
			ImageView embedIcon = (ImageView) view.findViewById(R.id.embed);
			// mipushIcon
			ImageView mipushIcon = (ImageView) view.findViewById(R.id.mipush);
			// raw
			ImageView raw = (ImageView) view.findViewById(R.id.raw);
			// white
			ImageView white = (ImageView) view.findViewById(R.id.white);
			// gen
			ImageView gen = (ImageView) view.findViewById(R.id.gen);
			try {
				final Context appContext = createPackageContext(info.packageName, 0);
				int iconId = 0, colorId = 0;
				// embedIcon
				final String key = info.packageName.toLowerCase().replaceAll("\\.", "_");
				if (context != null && (iconId  = context.getResources().getIdentifier(key, "drawable", RES_PACKAGE)) != 0) // has icon
					embedIcon.setImageIcon(Icon.createWithResource(RES_PACKAGE, iconId));
				else
					embedIcon.setImageIcon(null);
				if (context != null && (colorId  = context.getResources().getIdentifier(key, "string", RES_PACKAGE)) != 0) // has icon
					embedIcon.setColorFilter(Color.parseColor(context.getResources().getString(colorId)));
				// mipushIcon
				if ((iconId  = appContext.getResources().getIdentifier(NOTIFICATION_SMALL_ICON, "drawable", info.packageName)) != 0) // has icon
					mipushIcon.setImageIcon(Icon.createWithResource(info.packageName, iconId));
				else if ((iconId = appContext.getResources().getIdentifier(NOTIFICATION_ICON, "drawable", info.packageName)) != 0)
					mipushIcon.setImageIcon(Icon.createWithResource(info.packageName, iconId));
				else
					mipushIcon.setImageIcon(null);
				mipushIcon.setColorFilter(cache.getAppColor(appContext, info.packageName, (ctx, b) -> SmallIconDecoratorBase.getBackgroundColor(b)));
				// raw & white
				Bitmap rawIcon = cache.getIconForeground(appContext, info.packageName);
				if (rawIcon != null) {
					raw.setImageBitmap(rawIcon);
					// white.setImageBitmap(IconCache.whitenBitmap(MainActivity.this, rawIcon));
					white.setImageBitmap(IconCache.alphaize(rawIcon));
				} else {
					raw.setImageIcon(null);
					white.setImageIcon(null);
				}
				// gen
				Icon iconCache = cache.getIconCache(MainActivity.this, info.packageName);
				if (iconCache != null) {
					gen.setImageIcon(iconCache);
					gen.setColorFilter(cache.getAppColor(appContext, info.packageName, (ctx, b) -> SmallIconDecoratorBase.getBackgroundColor(b)));
				} else {
					gen.setImageIcon(null);
				}
			} catch (IllegalArgumentException | PackageManager.NameNotFoundException ign) { Log.d("inspect", "ex " + info.packageName);}
			return view;
		}
	}

	private static final String NOTIFICATION_ICON = "mipush_notification";
	private static final String NOTIFICATION_SMALL_ICON = "mipush_small_notification";
	private static final String RES_PACKAGE = "com.notxx.icon.res";
	private static final String CHANNEL_ID = "test_channel";

	private ListView listView;
	private int notificationId;

	private Context createPackageContext(String packageName) {
		try {
			return createPackageContext(packageName, 0);
		} catch (IllegalArgumentException | PackageManager.NameNotFoundException ign) {
			Log.d("inspect", "ex " + packageName);
			return null;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		createNotificationChannel();

		listView = new ListView(this);
		final PackageManager manager = getPackageManager();
		final List<PackageInfo> packages = manager.getInstalledPackages(0);
		final Context context = createPackageContext(RES_PACKAGE);
		PackagesAdapter adapter = new PackagesAdapter(manager, packages, context);
		listView.setOnItemClickListener(adapter);
		listView.setAdapter(adapter);
		setContentView(listView);
	}

	private void createNotificationChannel() {
		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = getString(R.string.test_channel_name);
			String description = getString(R.string.test_channel_desc);
			int importance = NotificationManager.IMPORTANCE_DEFAULT;
			// Log.d("SmallIcon", "new NC " + name + description);
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
			channel.setDescription(description);
			// Register the channel with the system; you can't change the importance
			// or other notification behaviors after this
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			// Log.d("SmallIcon", "create NC " + channel);
			notificationManager.createNotificationChannel(channel);
		}
	}
}
package com.andrusiv.glass.bash;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.glass.location.GlassLocationManager;
import com.google.glass.timeline.TimelineHelper;
import com.google.glass.timeline.TimelineProvider;
import com.google.glass.util.SettingsSecure;
import com.google.googlex.glass.common.proto.MenuItem;
import com.google.googlex.glass.common.proto.MenuValue;
import com.google.googlex.glass.common.proto.TimelineItem;

public class GlassService extends Service {

	private static final String BASH_URL = "http://api.ukrbash.org/1/pictures.getRandom.xml?limit=1&client=60e5ba6ea1e26a2c";
	private static final String TRANSLATE_URL = "http://translate.google.com/translate_a/t?client=x&text={XXX}&hl=en&sl=uk&tl=en";

	public static final String SERVICE_BROADCAST = "com.andrusiv.glass.bash.RANDOM";
	public static final String SERVICE_CARD = "home_card";
	public static final String SERVICE_COMMAND = "command";
	public static final String COMMAND_GET_RANDOM = "random";

	public static final String TAG = "UB-GS";
	private static final String SERVICE_ITEM_URL = "intent_url";
	private static final String SERVICE_ITEM_TEXT = "intent_text";

	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		super.onStartCommand(intent, flags, startid);
		Log.d(TAG, "GlassService onStartCommand() " + intent);
		
		GlassLocationManager.init(this);
		String homeCardId = PreferenceManager.getDefaultSharedPreferences(this).getString(SERVICE_CARD, null);

		TimelineHelper tlHelper = new TimelineHelper();
		ContentResolver cr = getContentResolver();
		if (homeCardId != null) {
			// find and delete previous home card
			TimelineItem timelineItem = tlHelper.queryTimelineItem(cr,
					homeCardId);
			if (timelineItem != null && !timelineItem.getIsDeleted())
				tlHelper.deleteTimelineItem(this, timelineItem);
		}

		requestAndUpdateTimeline(cr, tlHelper);

		return START_NOT_STICKY;
	}

	private void requestAndUpdateTimeline(final ContentResolver cr, final TimelineHelper tlHelper) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				UkrBashPicture picture = UkrBashPicture.NO_PICTURE;
				try {
					picture = requestAndParse();
				} catch (Exception e) {
					Log.w(TAG, "Something bad happened when requesting/parsing: " + e);
				}

				// create new home card
				String id = UUID.randomUUID().toString();
				MenuItem delOption = MenuItem.newBuilder()
						.setAction(MenuItem.Action.DELETE).build();
				MenuItem readAloud = MenuItem.newBuilder()
						.setAction(MenuItem.Action.READ_ALOUD).build();
//				MenuItem share = MenuItem.newBuilder()
//						.setAction(MenuItem.Action.SHARE).build();
				MenuItem customOption = MenuItem.newBuilder()
						.addValue(MenuValue.newBuilder()
								.setDisplayName("Random Picture")
								.build())
						.setAction(MenuItem.Action.BROADCAST)
						.setBroadcastAction(SERVICE_BROADCAST).build();

				TimelineItem.Builder builder = tlHelper
						.createTimelineItemBuilder(GlassService.this, new SettingsSecure(cr));
				TimelineItem item = builder
						.setId(id)
						.setHtml(picture.toHtml())
						.setText(picture.translation)
						.setIsPinned(true)
						.addMenuItem(customOption)
						.addMenuItem(readAloud)
//						.addMenuItem(share)
						.addMenuItem(delOption)
						.build();

				cr.insert(TimelineProvider.TIMELINE_URI,
						TimelineHelper.toContentValues(item));
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(GlassService.this);
				preferences.edit().putString(SERVICE_CARD, id).commit();
			}
		};
		Executors.newSingleThreadExecutor().execute(r);
	}

	private UkrBashPicture requestAndParse() throws Exception {
		HttpGet uri = new HttpGet(BASH_URL);

		DefaultHttpClient client = new DefaultHttpClient();
		HttpResponse resp = client.execute(uri);

		StatusLine status = resp.getStatusLine();
		if (status.getStatusCode() != 200) {
			Log.d(TAG,
					"HTTP error, invalid server status code: "
							+ resp.getStatusLine());
		}

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(resp.getEntity().getContent());

		NodeList randomPicture = doc.getFirstChild().getFirstChild()
				.getChildNodes();

		String imageUrl = "";
		String itemText = "";
		for (int i = 0; i < randomPicture.getLength(); i++) {
			Node item = randomPicture.item(i);
			if ("title".equals(item.getNodeName())) {
				itemText = item.getTextContent();
			}
			if ("image".equals(item.getNodeName())) {
				imageUrl = item.getTextContent();
			}
		}

		String translation = "";
		try {
			translation = getTranslation(itemText);
		} catch (Exception e) {
			Log.w(TAG, "errors when getting translation for read aloud: " + e);
		}

		return new UkrBashPicture(imageUrl, itemText, translation);
	}

	private static String getTranslation(String itemText) throws Exception {
		String from = "uk";
		String to = "en";
		String text = "http://translate.google.com/translate_a/t?"
				+ "client=o&text=" + URLEncoder.encode(itemText, "UTF-8")
				+ "&hl=en&sl=" + from + "&tl=" + to + "";
		URL url = new URL(text);
		URLConnection conn = url.openConnection();
		// Look like faking the request coming from Web browser solve 403 error
		conn.setRequestProperty(
				"User-Agent",
				"Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 (.NET CLR 3.5.30729)");
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		String json = in.readLine();
		String trans = ((JSONObject) (new JSONObject(json)
				.getJSONArray("sentences").get(0)))
				.getString("trans");
		in.close();
		return trans;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
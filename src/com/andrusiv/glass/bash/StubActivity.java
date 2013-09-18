package com.andrusiv.glass.bash;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class StubActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startService(new Intent(getApplicationContext(), GlassService.class));
		finish();
	}
}

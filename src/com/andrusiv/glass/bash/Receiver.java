package com.andrusiv.glass.bash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Boot receiver and "Update" command receiver
 * @author Ostap.Andrusiv
 *
 */
public class Receiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
        Log.d(GlassService.TAG, "Receiver: handle action: "+intent.getAction());
        Intent i = new Intent(context, GlassService.class);
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            context.startService(i);
        }
        if (GlassService.SERVICE_BROADCAST.equals(intent.getAction())) {
            i.putExtra(GlassService.SERVICE_COMMAND, GlassService.COMMAND_GET_RANDOM);
            context.startService(i);
        }
	}

}

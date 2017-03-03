package com.fourthyearproject.shane.cycled;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Created by hp on 22/02/2017.
 */


class BluetoothResultReceiver extends ResultReceiver
{
    private MapsActivity maps;

    public BluetoothResultReceiver(Handler handler, MapsActivity m) {
        super(handler);
        maps = m;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        maps.onReceiveBluetoothUpdate(resultData);
    }
}

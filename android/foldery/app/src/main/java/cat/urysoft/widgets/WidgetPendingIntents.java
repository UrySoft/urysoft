package cat.urysoft.widgets;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.os.Bundle;

/** Explicit activities opened by the user's taps in the home-screen widget. */
final class WidgetPendingIntents {
    private WidgetPendingIntents(){}
    static PendingIntent activity(Context c,int request,Intent intent,int flags){
        Bundle options=null;
        if(Build.VERSION.SDK_INT>=35)options=ActivityOptions.makeBasic()
            .setPendingIntentCreatorBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED).toBundle();
        return PendingIntent.getActivity(c,request,intent,flags,options);
    }
}

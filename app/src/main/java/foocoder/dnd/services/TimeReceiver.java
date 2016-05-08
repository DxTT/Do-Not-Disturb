package foocoder.dnd.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;

import org.joda.time.DateTime;

import javax.inject.Inject;

import foocoder.dnd.domain.Schedule;
import foocoder.dnd.domain.interactor.DefaultSubscriber;
import foocoder.dnd.domain.interactor.ScheduleCase;
import foocoder.dnd.presentation.App;
import foocoder.dnd.utils.AlarmUtil;
import foocoder.dnd.utils.SharedPreferenceUtil;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import static android.app.AlarmManager.RTC_WAKEUP;
import static foocoder.dnd.presentation.App.AUTO_TIME_SCHEDULE;

public class TimeReceiver extends BroadcastReceiver {

    @Inject
    AlarmManager alarmManager;

    @Inject
    AudioManager audioManager;

    @Inject
    SharedPreferenceUtil spUtil;

    @Inject
    ScheduleCase<Schedule> getSchedule;

    @Inject
    Schedule scheduleForOp;

    private Schedule schedule;

    private int daysTillNext;

    @Override
    public void onReceive(Context context, Intent intent) {
        App.getContext().getApplicationComponent().inject(this);
        Bundle extras = intent.getExtras();
        Subscription subscription = Subscriptions.empty();

        if (AUTO_TIME_SCHEDULE.equals(intent.getAction())) {
            if (extras.getBoolean("notify", false)) {
                scheduleForOp._id = spUtil.getRunningId();
                AlarmUtil.setSilent(true, schedule);
                AlarmUtil.startSchedule(schedule);
            } else {
                int _id = extras.getInt("id", 0);
                boolean sound_enable = extras.getBoolean("sound_enable", false);
                boolean cross_night = extras.getBoolean("cross_night", false);
                scheduleForOp._id = _id % 2 == 0 ? _id : _id - 1;

                Intent newIntent = new Intent(context, TimeReceiver.class).setAction(AUTO_TIME_SCHEDULE);
                newIntent.putExtras(extras);
                PendingIntent pendingIntent = AlarmUtil.getPendingIntent(context, _id, newIntent);

                subscription = getSchedule.execute(new DefaultSubscriber<Schedule>() {
                    @Override
                    public void onNext(Schedule result) {
                        schedule = result;
                        if (_id % 2 != 0) {
                            daysTillNext = AlarmUtil.daysTillNext(schedule, cross_night);
                        } else {
                            daysTillNext = AlarmUtil.daysTillNext(schedule);
                        }
                        DateTime trigger = DateTime.now().plusDays(daysTillNext);

                        alarmManager.setExact(RTC_WAKEUP, trigger.getMillis(), pendingIntent);

                        AlarmUtil.setSilent(sound_enable, schedule);
                    }
                });
            }
        } else {
            if (audioManager.getRingerMode() > 1) {
                if (spUtil.isUsable()) {
                    spUtil.setRunningId(-1);
                    spUtil.enable(false);
                    context.stopService(new Intent(context, ListenerService.class));
                }
            }
        }
//        subscription.unsubscribe();
    }
}

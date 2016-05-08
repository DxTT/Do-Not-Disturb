package foocoder.dnd.presentation.presenter;

import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import foocoder.dnd.domain.Schedule;
import foocoder.dnd.domain.interactor.DefaultSubscriber;
import foocoder.dnd.domain.interactor.ScheduleCase;
import foocoder.dnd.presentation.internal.di.PerActivity;
import foocoder.dnd.presentation.view.MainSettingView;
import foocoder.dnd.utils.AlarmUtil;
import foocoder.dnd.utils.SharedPreferenceUtil;

@PerActivity
public class MainPresenter extends Presenter<MainSettingView> {

    @Inject
    SharedPreferenceUtil spUtil;

    @Inject
    List<Schedule> schedules;

    @Inject
    Schedule schedule;

    @Inject
    @Named("saveSchedule")
    ScheduleCase<Boolean> saveSchedule;

    @Inject
    @Named("updateSchedule")
    ScheduleCase<Boolean> updateSchedule;

    @Inject
    @Named("deleteSchedule")
    ScheduleCase<Boolean> deleteSchedule;

    @Inject
    ScheduleCase<List<Schedule>> getScheduleList;

    @Inject
    public MainPresenter() {}

    @Override
    public void start() {
        addSubscriptionsForUnbinding(getScheduleList.
                execute(new DefaultSubscriber<List<Schedule>>() {
                    @Override
                    public void onNext(List<Schedule> scheduleList) {
                        schedules.addAll(scheduleList);
                        if (getView() != null) {
                            getView().showSchedules();
                        }
                    }
                }));
    }

    @Override
    public void bindView(@NonNull MainSettingView view) {
        super.bindView(view);

        view.changeState(false);
        view.changeLauncherState(spUtil.isLaunched());
        view.changeTimerState(spUtil.isStarted());
        view.changeVibrationState(spUtil.isVib());
        view.changeRepetitionState(spUtil.isRepeated());
    }

    public List<Schedule> getSchedules() {
        return this.schedules;
    }

    public Schedule getSchedule(int position) {
        return this.schedules.get(position);
    }

    public void addSchedule(Schedule schedule) {
        spUtil.setId(schedule._id = spUtil.getId() + 2);
        this.schedule.copy(schedule);
        if (!spUtil.isStarted()) {
            AlarmUtil.startSchedule(schedule);
        }
        schedules.add(schedule);
        addSubscriptionsForUnbinding(saveSchedule.execute(new OperationSubscriber()));
    }

    public void updateSchedule(Schedule schedule, int position) {
        this.schedule.copy(schedule);
        this.schedules.set(position, schedule);
        if (!spUtil.isStarted()) {
            AlarmUtil.startSchedule(schedule);
        }
        addSubscriptionsForUnbinding(updateSchedule.execute(new OperationSubscriber()));
    }

    public void deleteSchedule(Schedule schedule) {
        AlarmUtil.cancelOldAlarm(schedule);
        this.schedule.copy(schedule);
        this.schedules.remove(schedule);
        if (schedules.size() == 0) {
            spUtil.setRunningId(-1);
        }
        addSubscriptionsForUnbinding(deleteSchedule.execute(new OperationSubscriber()));
    }

    private class OperationSubscriber extends DefaultSubscriber<Boolean> {
        @Override
        public void onNext(Boolean aBoolean) {
            if (aBoolean) {
                if (getView() != null) {
                    getView().showSchedules();
                }
            }
        }
    }
}

package com.example.userssdk.ui.calendar;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.userssdk.R;
import com.example.userssdk.UsersSdk;
import com.example.userssdk.appointments.AppointmentUtils;
import com.example.userssdk.model.UserDTO;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.O)
public class UserCalendarFragment extends Fragment {

    // Views
    private CalendarView calendarView;
    private RecyclerView rvMonthList;

    // Adapters / data
    private SimpleTextAdapter monthAdapter;
    private final List<String> monthItems = new ArrayList<>();
    private final Set<LocalDate> apptDates = new HashSet<>();
    private TextView tvMonthTitle;
    private final DateTimeFormatter monthTitleFmt =
            DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault());
    // State
    private YearMonth visibleMonth;

    // Formatters
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
    private final DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.userssdk_fragment_user_calendar, container, false);
        tvMonthTitle = v.findViewById(R.id.tvMonthTitle);

        calendarView = v.findViewById(R.id.calendarView);
        rvMonthList  = v.findViewById(R.id.rvMonthList);

        rvMonthList.setLayoutManager(new LinearLayoutManager(requireContext()));
        monthAdapter = new SimpleTextAdapter(monthItems);
        rvMonthList.setAdapter(monthAdapter);

        // Calendar setup
        LocalDate today = LocalDate.now();
        YearMonth start = YearMonth.from(today.minusMonths(3));
        YearMonth end   = YearMonth.from(today.plusMonths(3));
        visibleMonth    = YearMonth.from(today);
        updateMonthTitle();

        // cv_dayViewResource כבר מוגדר ב-XML (userssdk_fragment_user_calendar),
        // לכן אין חובה לקרוא setDayViewResource כאן.
        calendarView.setup(start, end, DayOfWeek.SUNDAY);
        calendarView.scrollToDate(today);

        // צביעה של תאריכים עם תורים
        calendarView.setDayBinder(new MonthDayBinder<DayViewHolder>() {
            @NonNull
            @Override
            public DayViewHolder create(@NonNull View view) {
                return new DayViewHolder(view);
            }

            @Override
            public void bind(@NonNull DayViewHolder holder, @NonNull CalendarDay day) {
                LocalDate d = day.getDate();
                holder.text.setText(String.valueOf(d.getDayOfMonth()));
                holder.text.setTextColor(YearMonth.from(d).equals(visibleMonth) ? Color.BLACK : Color.LTGRAY);
                holder.text.setBackgroundResource(apptDates.contains(d) ? R.drawable.circle_red : 0);
            }
        });

        calendarView.setMonthScrollListener(month -> {
            visibleMonth = month.getYearMonth();
            updateMonthTitle();          // ✅ הכיתוב של החודש
            reloadMonthList();           // ✅ הרשימה שמתחת
            calendarView.notifyCalendarChanged(); // ✅ צביעת ימים (שחור/אפור) מחדש
            return null;
        });

        // טעינות ראשוניות
        loadAppointments();   // ימלא apptDates ויצבע את הלוח
        reloadMonthList();    // ימלא את רשימת התורים של החודש

        return v;
    }

    /** טוען את כל התורים של המשתמש לצביעת הלוח (נקודות/רקע). */
    private void loadAppointments() {
        UsersSdk.get().currentUser(new UsersSdk.Callback<UserDTO>() {
            @Override public void onSuccess(UserDTO user) {
                List<String> values = AppointmentUtils.readValues(user);
                apptDates.clear();
                for (String v : values) {
                    try {
                        apptDates.add(LocalDate.parse(v.split(" ")[0], dateFmt));
                    } catch (Exception ignore) { }
                }
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> calendarView.notifyCalendarChanged());
            }
            @Override public void onError(Throwable error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load appointments", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /** ממלא את רשימת התורים של החודש הנראה כרגע מתחת ללוח. */
    private void reloadMonthList() {
        UsersSdk.get().currentUser(new UsersSdk.Callback<UserDTO>() {
            @Override public void onSuccess(UserDTO user) {
                List<String> all = AppointmentUtils.readValues(user);
                List<String> filtered = new ArrayList<>();
                for (String s : all) {
                    try {
                        LocalDateTime dt = LocalDateTime.parse(s.trim(), dateTimeFmt);
                        if (YearMonth.from(dt.toLocalDate()).equals(visibleMonth)) {
                            filtered.add(s);
                        }
                    } catch (Exception ignore) { }
                }
                filtered.sort((a, b) -> {
                    try {
                        return LocalDateTime.parse(a, dateTimeFmt).compareTo(LocalDateTime.parse(b, dateTimeFmt));
                    } catch (Exception e) { return a.compareTo(b); }
                });

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    monthItems.clear();
                    for (String x : filtered) monthItems.add(x + " (30m)");
                    monthAdapter.notifyDataSetChanged();
                });
            }
            @Override public void onError(Throwable error) { /* optional log */ }
        });
    }

    /** ViewHolder לתא יום בלוח. */
    static class DayViewHolder extends ViewContainer {
        final TextView text;
        DayViewHolder(@NonNull View view) {
            super(view);
            text = view.findViewById(R.id.calendarDayText);
        }
    }

    private void updateMonthTitle() {
        String title = getString(
                R.string.userssdk_appts_for_month,
                monthTitleFmt.format(visibleMonth.atDay(1))
        );
        tvMonthTitle.setText(title);
    }

    /** אדפטר טקסט פשוט לרשימת התורים של החודש. */
    static class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.VH> {
        private final List<String> items;
        SimpleTextAdapter(List<String> items) { this.items = items; }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tv;
            VH(@NonNull View v) { super(v); tv = v.findViewById(android.R.id.text1); }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setId(android.R.id.text1);
            tv.setTextSize(16f);
            int pad = (int) (16 * parent.getResources().getDisplayMetrics().density);
            tv.setPadding(pad, pad / 2, pad, pad / 2);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.tv.setText(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
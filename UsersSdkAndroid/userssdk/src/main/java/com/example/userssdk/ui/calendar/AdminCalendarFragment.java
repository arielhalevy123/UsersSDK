package com.example.userssdk.ui.calendar;

import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.userssdk.UsersSdk;
import com.example.userssdk.model.CustomFieldDTO;
import com.example.userssdk.model.UserDTO;
import com.example.userssdk.R;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.view.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AdminCalendarFragment extends Fragment {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd", java.util.Locale.getDefault());
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.getDefault());

    private CalendarView calendarView;
    private RecyclerView list;
    private TextView titleDay;

    private final Map<LocalDate, List<Slot>> slotsByDate = new HashMap<>();
    private final List<Slot> visibleSlots = new ArrayList<>();
    private AdminSlotsAdapter adapter;

    private YearMonth currentMonth;
    private LocalDate selectedDate = null;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        View root = inflater.inflate(R.layout.userssdk_fragment_admin_calendar, parent, false);
        calendarView = root.findViewById(R.id.calendarView);
        list = root.findViewById(R.id.rvDaySlots);
        titleDay = root.findViewById(R.id.tvDayTitle);

        list.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminSlotsAdapter(visibleSlots);
        list.setAdapter(adapter);

        setupCalendar();
        loadAllAppointments();
        return root;
    }

    private void setupCalendar() {
        LocalDate today = LocalDate.now();
        YearMonth startMonth = YearMonth.from(today.minusMonths(3));
        YearMonth endMonth   = YearMonth.from(today.plusMonths(3));
        currentMonth = YearMonth.from(today);

        calendarView.setup(startMonth, endMonth, DayOfWeek.SUNDAY);
        calendarView.setDayViewResource(R.layout.calendar_day_admin);
        calendarView.scrollToDate(today);

        calendarView.setDayBinder(new MonthDayBinder<DayHolder>() {
            @Override public DayHolder create(@NonNull View view) { return new DayHolder(view); }

            @Override public void bind(@NonNull DayHolder c, @NonNull CalendarDay day) {
                LocalDate date = day.getDate();
                c.dayText.setText(String.valueOf(date.getDayOfMonth()));

                // שחור בחודש הנוכחי, אפור בחודשים אחרים
                c.dayText.setTextColor(YearMonth.from(date).equals(currentMonth) ? 0xFF000000 : 0xFFAAAAAA);

                List<Slot> slots = slotsByDate.get(date);
                if (slots != null && !slots.isEmpty()) {
                    c.badge.setVisibility(View.VISIBLE);
                    c.badge.setText(String.valueOf(slots.size()));
                } else {
                    c.badge.setVisibility(View.INVISIBLE);
                }

                View cell = c.getView();
                cell.setSelected(date.equals(selectedDate));
                cell.setOnClickListener(v -> {
                    selectedDate = date;
                    updateDayList(date);
                    calendarView.notifyCalendarChanged();
                });
            }
        });

        // ✅ מאזין לגלילת חודשים — מעדכן currentMonth ומרענן
        calendarView.setMonthScrollListener(month -> {
            currentMonth = month.getYearMonth();
            // אם היום הנבחר לא שייך לחודש החדש, בחר את היום הראשון בחודש
            if (selectedDate == null || !YearMonth.from(selectedDate).equals(currentMonth)) {
                selectedDate = currentMonth.atDay(1);
            }
            updateDayList(selectedDate);
            calendarView.notifyCalendarChanged();
            return null;
        });
    }

    private void updateDayList(LocalDate date) {
        List<Slot> slots = slotsByDate.getOrDefault(date, Collections.emptyList());
        visibleSlots.clear();
        visibleSlots.addAll(slots);
        visibleSlots.sort(Comparator.comparing(s -> s.time));

        titleDay.setText(slots.isEmpty()
                ? getString(R.string.userssdk_no_appts_for_day, DATE_FMT.format(date))
                : getString(R.string.userssdk_appts_for_day, DATE_FMT.format(date)));

        adapter.notifyDataSetChanged();
    }

    private void loadAllAppointments() {
        UsersSdk.get().myUsers(new UsersSdk.Callback<List<UserDTO>>() {
            @Override public void onSuccess(List<UserDTO> users) {
                slotsByDate.clear();

                for (UserDTO u : users) {
                    List<CustomFieldDTO> fields = u.getCustomFields();
                    if (fields == null) continue;

                    for (CustomFieldDTO f : fields) {
                        String name = f.getFieldName();
                        String value = f.getFieldValue();
                        if (name == null || value == null) continue;
                        if (!name.toLowerCase().startsWith("appointment")) continue;

                        for (String part : value.split(";")) {
                            String s = part.trim();
                            if (s.isEmpty()) continue;
                            try {
                                LocalDateTime start = LocalDateTime.parse(s, DATE_TIME_FMT);
                                LocalDate d = start.toLocalDate();
                                LocalTime t = start.toLocalTime();
                                slotsByDate.computeIfAbsent(d, k -> new ArrayList<>())
                                        .add(new Slot(u.getId(), u.getName(), u.getEmail(), d, t));
                            } catch (Exception ignore) {}
                        }
                    }
                }
                for (List<Slot> day : slotsByDate.values()) {
                    day.sort(Comparator.comparing(s -> s.time));
                }

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (selectedDate == null) selectedDate = LocalDate.now();
                    updateDayList(selectedDate);
                    calendarView.notifyCalendarChanged();
                });
            }

            @Override public void onError(Throwable error) {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Failed to load users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===== Holders/Adapters =====
    static class DayHolder extends ViewContainer {
        final TextView dayText, badge;
        DayHolder(@NonNull View view) {
            super(view);
            dayText = view.findViewById(R.id.calendarDayText);
            badge = view.findViewById(R.id.calendarDayBadge);
        }
    }

    static class Slot {
        final long userId;
        final String userName;
        final String userEmail;
        final LocalDate date;
        final LocalTime time;
        Slot(long userId, String userName, String userEmail, LocalDate date, LocalTime time) {
            this.userId = userId; this.userName = userName; this.userEmail = userEmail;
            this.date = date; this.time = time;
        }
    }

    static class AdminSlotsAdapter extends RecyclerView.Adapter<AdminSlotsAdapter.VH> {
        private final List<Slot> items;
        AdminSlotsAdapter(List<Slot> items) { this.items = items; }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_slot, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Slot s = items.get(pos);
            h.time.setText(s.time.format(TIME_FMT));
            h.name.setText(s.userName != null ? s.userName : "-");
            h.email.setText(s.userEmail != null ? s.userEmail : "-");
        }
        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView time, name, email;
            VH(@NonNull View v) {
                super(v);
                time = v.findViewById(R.id.tvTime);
                name = v.findViewById(R.id.tvName);
                email = v.findViewById(R.id.tvEmail);
            }
        }
    }
}
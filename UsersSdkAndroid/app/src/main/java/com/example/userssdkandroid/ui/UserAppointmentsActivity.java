package com.example.userssdkandroid.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.userssdk.UsersSdk;
import com.example.userssdk.model.UserDTO;
import com.example.userssdk.ui.appointments.UsersSdkAddAppointmentFab;
import com.example.userssdk.ui.calendar.UserCalendarFragment;
import com.example.userssdkandroid.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

@RequiresApi(api = Build.VERSION_CODES.O)
public class UserAppointmentsActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private UsersSdkAddAppointmentFab fab;

    // נשמור מופעים כדי לא לאבד state בכל מעבר
    private Fragment calendarFragment;
    private Fragment profileFragment;

    // דגל שמונע לולאה כשמשנים בחירה בצורה תכנית
    private boolean suppressNavCallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_calendar_host);

        bottomNav = findViewById(R.id.bottom_navigation);
        fab = findViewById(R.id.userssdkFab);

        // מאזין ל-NAV: רק מעבר פרגמנטים. לא משנים פה setSelectedItemId.
        bottomNav.setOnItemSelectedListener(item -> {
            if (suppressNavCallback) return true; // מונע לופ אם בחרנו תכנית
            int id = item.getItemId();
            if (id == R.id.menu_calendar) {
                switchToCalendar(/*fromUserTap=*/true);
                return true;
            } else if (id == R.id.menu_profile) {
                switchToProfile(/*fromUserTap=*/true);
                return true;
            }
            return false;
        });

        // יצירת פרגמנטים/שחזור
        if (savedInstanceState == null) {
            calendarFragment = new UserCalendarFragment();
            profileFragment  = new ProfileFragment();

            FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
            tx.add(R.id.calendarContainer, calendarFragment, "tab_calendar");
            tx.add(R.id.calendarContainer, profileFragment,  "tab_profile");
            tx.hide(profileFragment);
            tx.commit();

            // בחירת טאב ברירת מחדל (זה יפעיל את ה-listener פעם אחת — זה בסדר)
            bottomNav.setSelectedItemId(R.id.menu_calendar);
        } else {
            calendarFragment = getSupportFragmentManager().findFragmentByTag("tab_calendar");
            profileFragment  = getSupportFragmentManager().findFragmentByTag("tab_profile");
        }

        // קונפיגורציה של ה-FAB (מניח שה־Listener הוא ממשק רגיל, לא למבדה)
        fab.setListener(new UsersSdkAddAppointmentFab.Listener() {
            @Override public void onAdded(String newTime, UserDTO updated) {
                Toast.makeText(UserAppointmentsActivity.this,
                        "Appointment added: " + newTime, Toast.LENGTH_SHORT).show();
                // נרענן את פרגמנט היומן אם נראה לעין
                Fragment f = getSupportFragmentManager().findFragmentByTag("tab_calendar");
                if (f instanceof UserCalendarFragment && f.isVisible()) {
                    // Detach/attach כדי לטעון מחדש
                    FragmentTransaction txx = getSupportFragmentManager().beginTransaction();
                    txx.detach(f).attach(f).commit();
                }
            }
        });

        // הצגת/הסתרת FAB לפי תפקיד המשתמש
        UsersSdk.get().currentUser(new UsersSdk.Callback<UserDTO>() {
            @Override public void onSuccess(UserDTO user) {
                runOnUiThread(() -> {
                    boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
                    // ביומן נראה את ה-FAB (לאדמין — מסתירים), בפרופיל — מסתירים תמיד
                    if (getVisibleTab() == R.id.menu_calendar && !isAdmin) {
                        fab.setVisibility(View.VISIBLE);
                    } else {
                        fab.setVisibility(View.GONE);
                    }
                });
            }
            @Override public void onError(Throwable error) { /* התעלם/טוסט אם תרצה */ }
        });
    }

    private int getVisibleTab() {
        Fragment cal = getSupportFragmentManager().findFragmentByTag("tab_calendar");
        if (cal != null && cal.isVisible()) return R.id.menu_calendar;
        return R.id.menu_profile;
    }

    // מעבר ליומן
    private void switchToCalendar(boolean fromUserTap) {
        if (calendarFragment == null) calendarFragment = new UserCalendarFragment();
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setReorderingAllowed(true);
        tx.hide(profileFragment);
        tx.show(calendarFragment);
        tx.commit();

        // ה-FAB ביומן: מוצג רק אם המשתמש אינו אדמין
        UsersSdk.get().currentUser(new UsersSdk.Callback<UserDTO>() {
            @Override public void onSuccess(UserDTO user) {
                runOnUiThread(() -> {
                    boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
                    fab.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
                });
            }
            @Override public void onError(Throwable error) { fab.setVisibility(View.VISIBLE); }
        });

        // עדכון בחירה בתפריט רק אם לא נלחץ ע"י המשתמש (למנוע לולאה)
        if (!fromUserTap) {
            suppressNavCallback = true;
            bottomNav.setSelectedItemId(R.id.menu_calendar);
            suppressNavCallback = false;
        }
    }

    // מעבר לפרופיל
    private void switchToProfile(boolean fromUserTap) {
        if (profileFragment == null) profileFragment = new ProfileFragment();
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setReorderingAllowed(true);
        tx.hide(calendarFragment);
        tx.show(profileFragment);
        tx.commit();

        // בפרופיל ה-FAB מוסתר תמיד
        fab.setVisibility(View.GONE);

        if (!fromUserTap) {
            suppressNavCallback = true;
            bottomNav.setSelectedItemId(R.id.menu_profile);
            suppressNavCallback = false;
        }
    }
}
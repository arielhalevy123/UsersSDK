package com.example.userssdk;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.userssdk.datasource.AuthLocalDataSource;
import com.example.userssdk.datasource.AuthRemoteDataSource;
import com.example.userssdk.model.*;
import com.example.userssdk.network.*;
import com.example.userssdk.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Facade class – הנקודה היחידה שמפתחים יצטרכו להכיר.
 */
public class UsersSdk {
    // בתוך UsersSdk
    private static SdkConfig config = new SdkConfig();


    public static void setConfig(SdkConfig cfg) {
        if (cfg != null) config = cfg;
    }
    public static SdkConfig config() { return config; }



    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable error);
    }

    // -------------------- Singleton --------------------
    private static UsersSdk instance;

    public static synchronized UsersSdk init(@NonNull Context context, @NonNull String baseUrl) {
        if (instance == null) {
            instance = new UsersSdk(context.getApplicationContext(), baseUrl);
        }
        return instance;
    }

    public static UsersSdk get() {
        if (instance == null) {
            throw new IllegalStateException("Call init() before using UsersSdk");
        }
        return instance;
    }

    // -------------------- Internal Fields --------------------
    private final UserRepository repo;
    private UserDTO currentUser;
    private final AuthApi api;
    private UsersSdk(Context ctx, String baseUrl) {
        AuthLocalDataSource local = new AuthLocalDataSource(ctx);
        retrofit2.Retrofit retrofit = ServiceGenerator.create(baseUrl, local::getToken);
        AuthApi api = retrofit.create(AuthApi.class);
        AuthRemoteDataSource remote = new AuthRemoteDataSource(api);
        this.repo = new UserRepository(remote, local);
        this.api = api; // <-- חדש
    }
    public void listAdmins(Callback<List<UserDTO>> cb) {   // <-- חדש
        api.allUsers().enqueue(new retrofit2.Callback<List<UserDTO>>() {
            @Override public void onResponse(retrofit2.Call<List<UserDTO>> call,
                                             retrofit2.Response<List<UserDTO>> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    if (cb != null) cb.onError(new RuntimeException("Failed to load users"));
                    return;
                }
                List<UserDTO> admins = new java.util.ArrayList<>();
                for (UserDTO u : resp.body()) {
                    if (u != null && "ADMIN".equalsIgnoreCase(u.getRole())) admins.add(u);
                }
                if (cb != null) cb.onSuccess(admins);
            }
            @Override public void onFailure(retrofit2.Call<List<UserDTO>> call, Throwable t) {
                if (cb != null) cb.onError(t);
            }
        });
    }

    // -------------------- Public API --------------------

    public void register(String name, String email, String password, String role, Long adminId,
                         List<CustomFieldDTO> customFields, Callback<AuthResponse> cb) {
        RegisterRequest req = new RegisterRequest(name, email, password, role, adminId, customFields);
        repo.register(req, wrap(cb));
    }

    public void login(String email, String password, Callback<AuthResponse> cb) {
        repo.login(new LoginRequest(email, password), wrap(cb));
    }

    public void currentUser(Callback<UserDTO> cb) {
        repo.currentUser(new UserRepository.ResultCallback<UserDTO>() {
            @Override
            public void onSuccess(UserDTO data) {
                setCurrentUser(data);
                if (cb != null) cb.onSuccess(data);
            }

            @Override
            public void onError(Throwable t) {
                if (cb != null) cb.onError(t);
            }
        });
    }

    public void myUsers(Callback<List<UserDTO>> cb) {
        repo.myUsers(wrap(cb));
    }

    public void updateUser(UserDTO user, Callback<UserDTO> cb) {
        repo.updateUser(user, new UserRepository.ResultCallback<UserDTO>() {
            @Override public void onSuccess(UserDTO updated) {
                setCurrentUser(updated);            // ← חשוב: לרענן את ה-cache
                if (cb != null) cb.onSuccess(updated);
            }
            @Override public void onError(Throwable t) {
                if (cb != null) cb.onError(t);
            }
        });
    }

    public String getToken() {
        return repo.getToken();
    }

    public void logout() {
        repo.clearToken();     // 🧼 מנקה את הטוקן מה־SharedPreferences
        currentUser = null;    // 🧼 אפס את המשתמש הנוכחי
    }

    // -------------------- User Access --------------------

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserDTO user) {
        this.currentUser = user;
    }

    // -------------------- Helper --------------------

    private <T> UserRepository.ResultCallback<T> wrap(Callback<T> cb) {
        return new UserRepository.ResultCallback<T>() {
            @Override
            public void onSuccess(T data) {
                if (cb != null) cb.onSuccess(data);
            }

            @Override
            public void onError(Throwable t) {
                if (cb != null) cb.onError(t);
            }
        };
    }
    // ========= Appointments Facade =========
// בתוך UsersSdk (ברמת המחלקה, לא בתוך המחלקה הפנימית)
    private static Appointments appts;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static synchronized Appointments appointments() {
        if (instance == null) {
            throw new IllegalStateException("Call init() before using UsersSdk");
        }
        if (appts == null) appts = new Appointments(instance);
        return appts;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static final class Appointments {
        private final UsersSdk sdk;
        public static final int DEFAULT_MINUTES = 30; // או 45, לבחירתך

        Appointments(UsersSdk sdk) { this.sdk = sdk; }

        /** שליפת תורים כטקסטים. */

        public void listValues(UserDTO user, Callback<List<String>> cb) {
            List<String> values = com.example.userssdk.appointments.AppointmentUtils.readValues(user);
            if (cb != null) cb.onSuccess(values);
        }

        /** הוספת תור (טקסט "yyyy-MM-dd HH:mm"). */
        public void add(UserDTO user, String value, Callback<UserDTO> cb) {
            com.example.userssdk.appointments.AppointmentUtils.addValue(user, value);
            sdk.repo.updateUser(user, sdk.wrap(cb));
        }

        /** החלפת תור ישן בחדש. */
        public void replace(UserDTO user, String oldValue, String newValue, Callback<UserDTO> cb) {
            com.example.userssdk.appointments.AppointmentUtils.replaceValue(user, oldValue, newValue);
            sdk.repo.updateUser(user, sdk.wrap(cb));
        }


        /** מחיקת תור. */
        public void delete(UserDTO user, String value, Callback<UserDTO> cb) {
            com.example.userssdk.appointments.AppointmentUtils.removeValue(user, value);
            sdk.repo.updateUser(user, sdk.wrap(cb));
        }

        /** בדיקת חפיפה מול "myUsers" (כל התורים אצל האדמין). */
        public void hasConflictAgainstMyUsers(String candidate,
                                              int minutes,
                                              Long currentUserId,
                                              boolean allowSameUserId,
                                              UsersSdk.Callback<Boolean> cb) {
            UsersSdk.get().myUsers(new UsersSdk.Callback<List<UserDTO>>() {
                @Override public void onSuccess(List<UserDTO> users) {
                    // שם השדה של התורים – אם יש לך קונפיג, קח ממנו. אחרת ברירת מחדל:
                    String fieldName = UsersSdk.config().getAppointmentFieldName();
                    if (fieldName == null) fieldName = "Appointment";

                    // בונים דטקטור, מאנדקסים את כל התורים של ה־users, ואז בודקים חפיפה
                    com.example.userssdk.appointments.ConflictDetector cd =
                            new com.example.userssdk.appointments.ConflictDetector(minutes);

                    cd.indexFromUsers(users, fieldName);

                    boolean conflict = cd.hasConflict(candidate, currentUserId, allowSameUserId);
                    if (cb != null) cb.onSuccess(conflict);
                }
                @Override public void onError(Throwable error) {
                    if (cb != null) cb.onError(error);
                }
            });
        }
    }
}
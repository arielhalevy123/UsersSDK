# Android library

[← Documentation home](index.md)

## Install

### 1. Add the JitPack repository

In `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

<details>
<summary>Groovy (<code>settings.gradle</code>)</summary>

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```
</details>

### 2. Add the dependency

In your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.arielhalevy123:UsersSDK:1.0.0")
}
```

Groovy: `implementation 'com.github.arielhalevy123:UsersSDK:1.0.0'`

The library already declares the `INTERNET` permission.

### Requirements

| | |
|---|---|
| minSdk | **24** (Android 7.0) |
| Appointments API and calendar fragments | API **26+** (they use `java.time`) |
| compileSdk used to build the library | 35 |
| Java / Kotlin | Works from both; the API is plain Java |
| AndroidX | `android.useAndroidX=true` (the default for new projects) |
| Theme | Material Components / Material3 theme, for the bundled UI fragments |
| Server | A UsersSDK server reachable over **HTTPS** (see [Server](#server)) |

## Quick start

All calls are asynchronous; callbacks arrive on the main thread.

### Initialise

Once, before any other call (for example in `Application.onCreate()`):

```java
import io.github.arielhalevy123.userssdk.UsersSdk;

UsersSdk.init(context, "https://userssdk-api-production.up.railway.app/");
```

`baseUrl` comes from you, so you can point at the hosted server, a staging server or your own.
A missing trailing slash is added; an invalid URL throws `IllegalArgumentException`.
Use **https**. Android blocks plain `http` unless your app explicitly allows cleartext for that
host (fine for `http://10.0.2.2:8080/` during local development, see
[network security config](https://developer.android.com/privacy-and-security/security-config)).

Optional settings, before `init`:

```java
UsersSdk.setConfig(new SdkConfig()
        .setAppointmentMinutes(30)        // slot length used for conflict checks
        .setHttpLogging(false));          // true logs request bodies (passwords, tokens): debug only
```

### Register

```java
UsersSdk.get().register(
        "Dana Levi", "dana@example.com", "a-strong-password",
        "USER",          // or "ADMIN"
        adminId,         // Long id of the admin this user belongs to, or null
        null,            // optional initial custom fields (List<CustomFieldDTO>)
        new UsersSdk.Callback<AuthResponse>() {
            @Override public void onSuccess(AuthResponse res) {
                UserDTO me = res.getUser();   // the JWT is stored for you
            }
            @Override public void onError(Throwable error) { /* show a message */ }
        });
```

To let a new user pick their admin (for example a barber), `UsersSdk.get().listAdmins(cb)`
returns the admins, without login. Each entry has only `getId()` and `getName()`; no email or
custom fields.

### Login and logout

```java
UsersSdk.get().login("dana@example.com", "a-strong-password", new UsersSdk.Callback<AuthResponse>() {
    @Override public void onSuccess(AuthResponse res) { /* logged in */ }
    @Override public void onError(Throwable error) { /* wrong credentials or network */ }
});

UsersSdk.get().logout();   // clears the stored token
```

The token is saved on the device and sent automatically as `Authorization: Bearer ...`.

### Profile

```java
UsersSdk.get().currentUser(new UsersSdk.Callback<UserDTO>() {
    @Override public void onSuccess(UserDTO me) {
        String name = me.getName();
        String role = me.getRole();   // "USER" or "ADMIN"
    }
    @Override public void onError(Throwable error) { }
});

UserDTO me = UsersSdk.get().getCurrentUser();   // cached copy after currentUser()/updateUser()
me.setName("Dana L.");
UsersSdk.get().updateUser(me, callback);
```

Admins can list the users they manage with `UsersSdk.get().myUsers(cb)`.

### Custom fields

Any key/value data per user (phone, notes, preferences). `updateUser` saves the **whole list**, so
modify the existing list rather than sending a new one with a single field:

```java
UserDTO me = UsersSdk.get().getCurrentUser();
List<CustomFieldDTO> fields = me.getCustomFields() != null
        ? me.getCustomFields() : new ArrayList<>();
fields.add(new CustomFieldDTO("phone", "050-1234567"));
me.setCustomFields(fields);

UsersSdk.get().updateUser(me, new UsersSdk.Callback<UserDTO>() {
    @Override public void onSuccess(UserDTO updated) { }
    @Override public void onError(Throwable error) { }
});
```

### Appointments (API 26+)

Appointments are stored on the user as `"yyyy-MM-dd HH:mm"` values.

```java
UsersSdk.Appointments appts = UsersSdk.appointments();
UserDTO me = UsersSdk.get().getCurrentUser();

appts.listValues(me, cb);                                    // List<String>
appts.add(me, "2026-10-12 10:30", cb);                       // book
appts.replace(me, "2026-10-12 10:30", "2026-10-12 11:00", cb);
appts.delete(me, "2026-10-12 11:00", cb);

// Before booking: does this slot overlap anyone else's appointment with the same admin?
appts.hasConflictAgainstMyUsers("2026-10-12 10:30",
        UsersSdk.config().getAppointmentMinutes(), me.getId(), true,
        new UsersSdk.Callback<Boolean>() {
            @Override public void onSuccess(Boolean conflict) { }
            @Override public void onError(Throwable error) { }
        });
```

### Ready-made UI (API 26+)

```java
Fragment calendar = UsersSdkCalendar.newUserCalendarFragment();    // a user's appointments
Fragment adminCal = UsersSdkCalendar.newAdminCalendarFragment();   // all appointments of an admin's users
Fragment profile  = new UserProfileFragment();                      // name + custom fields editor
```

### Theming the built-in screens

The calendar, profile and add-appointment screens take their look from your app theme, so
the same SDK screens can look like a barbershop or a gel nails studio. Set any of these in
the theme your activities use; whatever you leave out falls back to your Material colours.

```xml
<style name="Theme.MyShop" parent="Theme.MaterialComponents.DayNight.NoActionBar">
    <item name="usersSdkColorPrimary">#D4A84B</item>     <!-- marked days, badges, button -->
    <item name="usersSdkColorOnPrimary">#141414</item>   <!-- text on the primary colour -->
    <item name="usersSdkColorSurface">#F5F0E8</item>     <!-- screen background -->
    <item name="usersSdkColorCard">#FFFFFF</item>        <!-- list rows -->
    <item name="usersSdkColorOnSurface">#1A1A1A</item>   <!-- text -->
    <item name="usersSdkColorMuted">#8A8A8A</item>       <!-- secondary text, other months -->
    <item name="usersSdkCornerRadius">6dp</item>
    <item name="usersSdkFontFamily">@font/my_font</item>
</style>
```

The `barberapp` sample builds twice from one codebase: `assembleBarberDebug` (black and gold,
sharp corners) and `assembleNailsDebug` (rose and blush, 24dp corners). The flavours differ
only in `src/<flavour>/res`.

## Versions

The JitPack version is the Git tag. `1.1.0` adds theming of the built-in screens; `1.0.0` is
the first public release. Every tag also creates a
[GitHub Release](https://github.com/arielhalevy123/UsersSDK/releases).

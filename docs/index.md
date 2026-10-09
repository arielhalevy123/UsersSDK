# UsersSDK documentation

UsersSDK gives an Android app user accounts, admin and user roles, per-user custom fields and
appointments, from one Gradle dependency. It talks to a Spring Boot + PostgreSQL server that is
hosted on Railway, or that you run yourself with Docker.

```kotlin
implementation("com.github.arielhalevy123:UsersSDK:1.1.0")
```

| Page | What it covers |
|------|----------------|
| [Architecture](architecture.md) | The components and how a request flows through them, with diagrams |
| [Server and REST API](server.md) | Endpoints, authentication, running locally, deploying |
| [Android library](library.md) | Install, initialise, every call, ready-made screens, theming |
| [Example apps](example-apps.md) | The demo app and the barber / gel nails apps, setup and screenshots |
| [Install a demo APK](INSTALL_APP.md) | Putting a demo app on a phone |

## The parts

- **API service** (`/src`): Spring Boot REST API, JWT authentication (24 hours), BCrypt
  passwords, PostgreSQL. Live at `https://userssdk-api-production.up.railway.app/`.
- **Android library** (`UsersSdkAndroid/userssdk`): Java facade `UsersSdk`, published on
  [JitPack](https://jitpack.io/#arielhalevy123/UsersSDK).
- **Example apps** (`UsersSdkAndroid/app`, `UsersSdkAndroid/barberapp`): a demo of every call, and a
  booking app built twice from one codebase, as a barbershop and as a gel nails studio.
- **Admin portal**: a web page served by the same server, for admins to manage their users.
- **CI/CD**: GitHub Actions run the tests on every pull request, deploy the server to Railway
  from `main`, and publish a GitHub Release on every version tag.

Source code: [github.com/arielhalevy123/UsersSDK](https://github.com/arielhalevy123/UsersSDK) ·
Licence: MIT

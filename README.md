# CoralCove

CoralCove is an offline resort-group desk. One administrator can run several properties. Front desk, managers, and attendants stay on the resort they are assigned to. The same twelve screens cover stays, lodges, guests, arrivals, billing, lodge care, experiences, employees, inventory, and reports.

This repository used to be a single-hotel desk; the running product is CoralCove, a multi-resort desk.

## Stack

- Java 17, Swing, programmatic layouts (no form files)
- SQLite through the vendored JDBC driver
- FlatLaf look and feel, with the system look and feel if FlatLaf fails
- JFreeChart for the dashboard
- JUnit 4 for rules, not for the windows
- NetBeans Ant project (`build.xml`, `nbproject/`)

Runtime JARs live in `lib/` and are copied beside the application JAR as `dist/lib`.

## Layout

```
src/coralcove/          package coralcove, main class coralcove.Main
test/coralcove/         JUnit tests
lib/                    sqlite-jdbc, flatlaf, jfreechart, junit, hamcrest
data/coralcove.db       created on first launch (not committed)
dist/CoralCove.jar      built application
```

The database path is `data/coralcove.db`. Set `CORALCOVE_DB` to point somewhere else.

## Sign in

| Username | Password | Notes |
| --- | --- | --- |
| admin | Admin@123 | Group administrator. Must change the password on first sign-in. Can switch resorts, including All resorts. |

A fresh database seeds one real property, CoralCove Beach (Kochi, Malabar coast), plus lodge categories and an experience catalog for that property. Demonstration rows are not loaded until Settings asks for them.

## Roles

| Role | What they can open |
| --- | --- |
| ADMIN | Every screen, including Settings. `users.resort_id` is empty so they are not tied to one property. |
| MANAGER | Every screen except Settings. Must belong to one resort. |
| FRONT_DESK | Dashboard, stays, lodges (view only), guests, arrivals, billing, lodge care (view), experiences. |
| ATTENDANT | Dashboard and lodge care. |

A non-admin account that tries to change another resort's lodge, stay, employee, stock, or experience gets: `That record belongs to another resort.`

## Build

In NetBeans, open the folder and use **Clean and Build**. The main class is `coralcove.Main`. The IDE writes `dist/CoralCove.jar`.

From a JDK 17 or newer install, with `jar` and `javac` on that JDK:

```
javac --release 17 -encoding UTF-8 -cp "lib\sqlite-jdbc-3.49.1.0.jar;lib\flatlaf-3.6.jar;lib\jfreechart-1.5.6.jar" -d build\classes @build\sources.txt
jar --create --file dist\CoralCove.jar --manifest manifest.mf -C build\classes .
java -jar dist\CoralCove.jar
```

`build\sources.txt` is one absolute path per Java file under `src`. Tests:

```
java -cp "build\classes;build\test-classes;lib\sqlite-jdbc-3.49.1.0.jar;lib\junit-4.13.2.jar;lib\hamcrest-core-1.3.jar" org.junit.runner.JUnitCore coralcove.PasswordHasherTest coralcove.DatabaseInitializerTest coralcove.AuthenticationServiceTest coralcove.ResortRulesTest coralcove.DemoDataTest
```

## Documents

- `USER_GUIDE.md` — how to use the desk
- `DATABASE_SCHEMA.md` — tables, statuses, occupancy
- `PROJECT_DOCUMENT.md` — architecture, threads, each Java file, and the ER diagram

## If something fails

- **FlatLaf failed:** the app logs it in `data/coralcove.log` and continues with the system look and feel.
- **Database is locked:** close the other CoralCove window that has the same file open.
- **Password rejected:** `Admin@123` works only until it is changed. Delete `data/coralcove.db` only when you intend to drop every local record, then start again.
- **Ambiguous column:** stay and lodge queries qualify `status` (`r.status`). A login that dies on `ambiguous column name: status` means a join selected `status` without a table name.

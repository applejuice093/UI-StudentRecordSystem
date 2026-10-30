# Java UI-Based Application Using MySQL

## NetBeans Desktop Project

## 📌 Project Description

This project is a Java-based desktop application developed using NetBeans IDE.
It provides a graphical user interface (GUI) built with Java Swing and uses MySQL as the backend database.

The application lets a registered user log in, add student records and view or search them, using JDBC with prepared statements.

## 🛠️ Technologies Used

Java (JDK 17 or higher)

Java Swing (GUI)

MySQL Database

JDBC (Java Database Connectivity)

NetBeans IDE

## 🎯 Objectives

To understand Java GUI programming

To connect Java application with MySQL database

To insert and query records safely using JDBC prepared statements

To develop a user-friendly desktop application

## 📂 Project Structure
```
UI-StudentRecordSystem/
├── src/studentmanagementsystem/
│   ├── StudentManagementSystem.java  # Entry point (opens LoginForm)
│   ├── DBConnection.java             # JDBC connection (settings from env vars)
│   ├── Validation.java               # Input validation helpers
│   ├── PasswordHasher.java           # Salted PBKDF2 password hashing
│   ├── StudentTableFormatter.java    # Text table used by ViewStudent
│   ├── LoginForm / RegisterForm      # Account screens
│   ├── Menu                          # Main menu
│   ├── SaveDetailForm                # Add a student record
│   └── ViewStudent                   # List / search student records
├── test/studentmanagementsystem/     # Unit tests (no external deps)
├── db/schema.sql                     # MySQL schema
├── nbproject/, build.xml             # NetBeans / Ant project files
└── Makefile                          # Build without NetBeans
```

## ⚙️ Software Requirements

- Java JDK 17 or newer
- MySQL Server 8+
- MySQL Connector/J
- NetBeans IDE (optional – the Makefile works without it)

## 🗄️ Database Setup

```bash
mysql -u root -p < db/schema.sql
```

Credentials are **not** stored in the code. Set them as environment variables
before running the app (defaults shown):

| Variable      | Default                                   |
|---------------|-------------------------------------------|
| `SRS_DB_URL`  | `jdbc:mysql://localhost:3306/studentdb`   |
| `SRS_DB_USER` | `root`                                    |
| `SRS_DB_PASS` | *(empty)*                                 |

## ▶️ How to Run the Project

**From the command line**

```bash
export SRS_DB_PASS=your_mysql_password
make test                                         # run unit tests
make run MYSQL_JAR=/path/to/mysql-connector-j.jar # launch the GUI
```

**From NetBeans**: File → Open Project, add the MySQL Connector JAR to
Libraries, set the environment variables, then Run.

## 🔐 Passwords

Login passwords are stored as salted PBKDF2-HMAC-SHA256 hashes
(`pbkdf2$<iterations>$<salt>$<hash>`), never as plain text.

Accounts created with an older version of the app hold a plain-text
password. They can still log in, and on that first successful login the
stored value is replaced with a hash automatically. To clear old
plain-text rows without waiting for a login, delete them
(`DELETE FROM users WHERE password NOT LIKE 'pbkdf2$%';`) and re-register.

## ✅ Input Validation

| Field               | Rule                                        |
|---------------------|---------------------------------------------|
| Registration Number | letters, digits, `-`; up to 20 chars        |
| Full Name           | 2–100 letters/spaces/`.`/`'`/`-`            |
| Year                | whole number 1–5                            |
| Semester            | whole number 1–10                           |
| CGPA                | 0–10, at most 2 decimal places (e.g. `8.75`) |
| Username            | 3–30 letters, digits, `_`, `.`              |
| Password            | at least 6 characters                       |

## 🖥️ User Interface

Designed using Java Swing

Created using NetBeans GUI Builder

Simple and interactive layout

## ✨ Features

- Login and registration (hashed passwords)
- Add a student record
- View all students, or search one by registration number
- Input validation and clear error messages (duplicate keys, missing JDBC driver)

## 🧪 Testing

`make test` compiles the project and runs the dependency-free unit tests
(no database or GUI needed):

- `ValidationTest`: form input rules
- `PasswordHasherTest`: hashing, verification, malformed values
- `StudentTableFormatterTest`: table layout and CGPA formatting
- `DBConnectionTest`: env-var fallback and the missing-driver message

The Swing forms and SQL itself are tested manually against a local MySQL.

## 🚀 Future Enhancements

Update and delete student records

Improved UI using JavaFX

Data export functionality

Role-based access control

## 👨‍💻 Developed By

Aniket Mahto

## 📄 License

Released under the MIT License (see `LICENSE`). Built as an educational project.

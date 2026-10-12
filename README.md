# Java UI-Based Application Using MySQL

## NetBeans Desktop Project

## 📌 Project Description

This project is a Java-based desktop application developed using NetBeans IDE.
It provides a graphical user interface (GUI) built with Java Swing and uses MySQL as the backend database.

The application performs basic database operations such as Insert, View, Update, and Delete (CRUD) using JDBC.

## 🛠️ Technologies Used

Java (JDK 8 or higher)

Java Swing (GUI)

MySQL Database

JDBC (Java Database Connectivity)

NetBeans IDE

## 🎯 Objectives

To understand Java GUI programming

To connect Java application with MySQL database

To perform CRUD operations using JDBC

To develop a user-friendly desktop application

## 📂 Project Structure
```
UI-StudentRecordSystem/
├── src/studentmanagementsystem/
│   ├── StudentManagementSystem.java  # Entry point (opens LoginForm)
│   ├── DBConnection.java             # JDBC connection (settings from env vars)
│   ├── Validation.java               # Input validation helpers
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

## ✅ Input Validation

| Field               | Rule                                        |
|---------------------|---------------------------------------------|
| Registration Number | letters, digits, `-`; up to 20 chars        |
| Full Name           | 2–100 letters/spaces/`.`/`'`/`-`            |
| Year                | whole number 1–5                            |
| Semester            | whole number 1–10                           |
| CGPA                | 0.0–10.0                                    |
| Username            | 3–30 letters, digits, `_`, `.`              |
| Password            | at least 6 characters                       |

## 🖥️ User Interface

Designed using Java Swing

Created using NetBeans GUI Builder

Simple and interactive layout

## ✨ Features

Graphical User Interface

Database connectivity using JDBC

Add, View, Update, Delete records

Error handling and validations

## 🧪 Testing

Tested with sample data

Valid input and invalid input handling

Database connection verified

## 🚀 Future Enhancements

User authentication system

Improved UI using JavaFX

Data export functionality

Role-based access control

## 👨‍💻 Developed By

Aniket Mahto

📄 License

This project is developed for educational purposes only.

-- Database schema for UI-StudentRecordSystem.
-- Usage: mysql -u root -p < db/schema.sql

CREATE DATABASE IF NOT EXISTS studentdb;
USE studentdb;

-- Login accounts (used by LoginForm / RegisterForm)
CREATE TABLE IF NOT EXISTS users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(30)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL  -- PBKDF2 hash (see PasswordHasher), never plain text
);

-- Student records (used by SaveDetailForm / ViewStudent)
CREATE TABLE IF NOT EXISTS students (
    reg_no           VARCHAR(20)  PRIMARY KEY,
    full_name        VARCHAR(100) NOT NULL,
    current_year     TINYINT      NOT NULL CHECK (current_year BETWEEN 1 AND 5),
    current_semester TINYINT      NOT NULL CHECK (current_semester BETWEEN 1 AND 10),
    cgpa             DECIMAL(4,2) NOT NULL CHECK (cgpa BETWEEN 0 AND 10)
);

-- Optional demo rows for trying out "View Students".
-- Usage (after schema.sql): mysql -u root -p studentdb < db/sample_data.sql
-- Re-running is safe: existing registration numbers are left unchanged.
-- No login accounts are seeded; create one with the Register screen so the
-- password is hashed by the app.

INSERT IGNORE INTO students (reg_no, full_name, current_year, current_semester, cgpa) VALUES
    ('RA2311003010001', 'Aarav Sharma',  1, 2, 8.75),
    ('RA2211003010042', 'Diya Patel',    2, 4, 9.10),
    ('RA2111003010107', 'Rohan Verma',   3, 6, 7.45),
    ('RA2011003010230', 'Ananya Iyer',   4, 8, 8.20),
    ('RA2311003010015', 'Kabir D''Souza', 1, 1, 6.90);

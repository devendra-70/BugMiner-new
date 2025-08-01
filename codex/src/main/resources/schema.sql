-- Create learning_paths table
CREATE TABLE IF NOT EXISTS learning_paths (
    id BIGSERIAL PRIMARY KEY,
    language VARCHAR(255),
    topic VARCHAR(255),
    subtopic VARCHAR(255)
);

-- Create problems table (matching your JPA entity)
CREATE TABLE IF NOT EXISTS problems (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    description VARCHAR(10000),
    constraints VARCHAR(5000),
    default_code VARCHAR(5000)
);

-- Create test_cases table (matching your JPA entity)
CREATE TABLE IF NOT EXISTS test_cases (
    id BIGSERIAL PRIMARY KEY,
    input VARCHAR(10000),
    expected_output VARCHAR(10000),
    is_public BOOLEAN NOT NULL DEFAULT true,
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE
);

-- Insert learning path data
INSERT INTO learning_paths (language, topic, subtopic)
VALUES
    -- Python topics
    ('Python', 'Basics', 'Variables & Data Types'),
    ('Python', 'Basics', 'Control Flow'),
    ('Python', 'Basics', 'Functions'),
    ('Python', 'OOP', 'Classes & Objects'),
    ('Python', 'OOP', 'Inheritance'),
    ('Python', 'Advanced', 'Modules & Packages'),
    ('Python', 'Advanced', 'File I/O'),
    ('Python', 'Advanced', 'Error Handling'),

    -- Java topics
    ('Java', 'Basics', 'Variables & Primitives'),
    ('Java', 'Basics', 'Control Statements'),
    ('Java', 'OOP', 'Classes & Objects'),
    ('Java', 'OOP', 'Interfaces & Abstract Classes'),
    ('Java', 'Advanced', 'Exception Handling'),
    ('Java', 'Advanced', 'Collections Framework'),

    -- C++ topics
    ('C++', 'Basics', 'Syntax & Data Types'),
    ('C++', 'Basics', 'Pointers & References'),
    ('C++', 'OOP', 'Classes & Inheritance'),
    ('C++', 'Advanced', 'Templates & STL'),
    ('C++', 'Advanced', 'Exception Handling'),

    -- NumPy topics
    ('NumPy', 'Basics', 'Arrays & Vectorization'),
    ('NumPy', 'Basics', 'Indexing & Slicing'),
    ('NumPy', 'Advanced', 'Math & Stats'),
    ('NumPy', 'Advanced', 'Linear Algebra'),

    -- Pandas topics
    ('Pandas', 'Basics', 'Series & DataFrames'),
    ('Pandas', 'Basics', 'Filtering & Sorting'),
    ('Pandas', 'Advanced', 'GroupBy & Aggregations'),
    ('Pandas', 'Advanced', 'Merging & Joins'),
    ('Pandas', 'Advanced', 'Time Series')
ON CONFLICT DO NOTHING;

-- Insert sample problems
INSERT INTO problems (id, title, description, constraints, default_code)
VALUES
    (1, 'Sum of Two Numbers',
     'Given two integers a and b, return their sum.\n\nExample:\nInput: a = 2, b = 3\nOutput: 5',
     '• -1000 <= a, b <= 1000',
     'public class Solution {\n    public int sum(int a, int b) {\n        // Your code here\n        return 0;\n    }\n}'),
    (2, 'Palindrome Check',
     'Check whether a given string is a palindrome. A palindrome reads the same forward and backward.\n\nExample:\nInput: "madam"\nOutput: true',
     '• 1 <= s.length <= 1000\n• s consists of lowercase English letters only',
     'public class Solution {\n    public boolean isPalindrome(String s) {\n        // Your code here\n        return false;\n    }\n}')
ON CONFLICT (id) DO NOTHING;

-- Insert test cases
INSERT INTO test_cases (input, expected_output, is_public, problem_id)
VALUES
    ('2,3', '5', true, 1),
    ('10,-4', '6', true, 1),
    ('-100,200', '100', false, 1),
    ('madam', 'true', true, 2),
    ('hello', 'false', true, 2),
    ('racecar', 'true', false, 2)
ON CONFLICT DO NOTHING;

-- Reset sequences to continue from the inserted data
SELECT setval('learning_paths_id_seq', (SELECT MAX(id) FROM learning_paths));
SELECT setval('problems_id_seq', (SELECT MAX(id) FROM problems));
SELECT setval('test_cases_id_seq', (SELECT MAX(id) FROM test_cases));
-- Create problems table
CREATE TABLE IF NOT EXISTS problems (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255),
    description TEXT,
    topic_tag VARCHAR(255),
    source VARCHAR(255)
);

-- Create default_code_map table (Map<String, String> in JPA)
CREATE TABLE IF NOT EXISTS default_code_map (
    problem_id INTEGER REFERENCES problems(id) ON DELETE CASCADE,
    language VARCHAR(100),
    default_code TEXT,
    PRIMARY KEY (problem_id, language)
);

-- Create test_cases table
CREATE TABLE IF NOT EXISTS test_cases (
    id SERIAL PRIMARY KEY,
    input TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    problem_id INTEGER REFERENCES problems(id) ON DELETE CASCADE
);

-- Insert problems (with ON CONFLICT to avoid duplication)
INSERT INTO problems (id, title, description, topic_tag, source)
VALUES
    (1, 'Sum of Two Numbers', 'Given two integers, return their sum.', 'math', 'Custom'),
    (2, 'Palindrome Check', 'Check whether a string is a palindrome.', 'string', 'Custom')
ON CONFLICT (id) DO NOTHING;

-- Insert default code for each problem
INSERT INTO default_code_map (problem_id, language, default_code)
VALUES
    (1, 'java', 'public class Main { public static int sum(int a, int b) { return a + b; } }'),
    (2, 'java', 'public class Main { public static boolean isPalindrome(String s) { return false; } }')
ON CONFLICT DO NOTHING;

-- Insert test cases
INSERT INTO test_cases (input, expected_output, problem_id)
VALUES
    ('2 3', '5', 1),
    ('10 -4', '6', 1),
    ('madam', 'true', 2),
    ('hello', 'false', 2)
ON CONFLICT DO NOTHING;

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
SELECT setval('problems_id_seq', (SELECT MAX(id) FROM problems));
SELECT setval('test_cases_id_seq', (SELECT MAX(id) FROM test_cases));
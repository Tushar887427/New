-- School Management System - Supabase Database Schema
-- Run this SQL in your Supabase SQL Editor to set up the database

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Students Table
CREATE TABLE IF NOT EXISTS students (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    student_id TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT,
    grade TEXT,
    gpa NUMERIC(3, 2),
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'inactive')),
    date_of_birth DATE,
    guardian_name TEXT,
    enrollment_date DATE,
    class_section TEXT,
    attendance_rate NUMERIC(5, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Staff Table
CREATE TABLE IF NOT EXISTS staff (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    staff_id TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT,
    position TEXT NOT NULL,
    department TEXT NOT NULL,
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'leave', 'inactive')),
    join_date DATE,
    employment_type TEXT DEFAULT 'full-time',
    office_location TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Documents Table
CREATE TABLE IF NOT EXISTS documents (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    file_url TEXT NOT NULL,
    file_name TEXT NOT NULL,
    file_size TEXT,
    uploaded_by TEXT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- School Info Table
CREATE TABLE IF NOT EXISTS school_info (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    school_name TEXT NOT NULL,
    address TEXT,
    city TEXT,
    state TEXT,
    zip_code TEXT,
    country TEXT,
    phone TEXT,
    email TEXT,
    website TEXT,
    principal_name TEXT,
    established_year INTEGER,
    total_students INTEGER DEFAULT 0,
    total_staff INTEGER DEFAULT 0,
    total_classrooms INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Users Table (for authentication)
CREATE TABLE IF NOT EXISTS users (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    role TEXT DEFAULT 'clerk' CHECK (role IN ('admin', 'clerk', 'teacher')),
    phone TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_students_student_id ON students(student_id);
CREATE INDEX IF NOT EXISTS idx_students_status ON students(status);
CREATE INDEX IF NOT EXISTS idx_students_grade ON students(grade);

CREATE INDEX IF NOT EXISTS idx_staff_staff_id ON staff(staff_id);
CREATE INDEX IF NOT EXISTS idx_staff_status ON staff(status);
CREATE INDEX IF NOT EXISTS idx_staff_department ON staff(department);

CREATE INDEX IF NOT EXISTS idx_documents_category ON documents(category);
CREATE INDEX IF NOT EXISTS idx_documents_uploaded_at ON documents(uploaded_at);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = TIMEZONE('utc', NOW());
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at triggers to all tables
CREATE TRIGGER update_students_updated_at BEFORE UPDATE ON students
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_staff_updated_at BEFORE UPDATE ON staff
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_school_info_updated_at BEFORE UPDATE ON school_info
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Row Level Security (RLS) Policies
-- Enable RLS on all tables
ALTER TABLE students ENABLE ROW LEVEL SECURITY;
ALTER TABLE staff ENABLE ROW LEVEL SECURITY;
ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_info ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Allow authenticated users to read all data
CREATE POLICY "Allow authenticated users to read students"
    ON students FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Allow authenticated users to read staff"
    ON staff FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Allow authenticated users to read documents"
    ON documents FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Allow authenticated users to read school_info"
    ON school_info FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Allow authenticated users to read users"
    ON users FOR SELECT
    TO authenticated
    USING (true);

-- Allow authenticated users to insert/update/delete
CREATE POLICY "Allow authenticated users to insert students"
    ON students FOR INSERT
    TO authenticated
    WITH CHECK (true);

CREATE POLICY "Allow authenticated users to update students"
    ON students FOR UPDATE
    TO authenticated
    USING (true);

CREATE POLICY "Allow authenticated users to delete students"
    ON students FOR DELETE
    TO authenticated
    USING (true);

-- Similar policies for staff
CREATE POLICY "Allow authenticated users to insert staff"
    ON staff FOR INSERT
    TO authenticated
    WITH CHECK (true);

CREATE POLICY "Allow authenticated users to update staff"
    ON staff FOR UPDATE
    TO authenticated
    USING (true);

CREATE POLICY "Allow authenticated users to delete staff"
    ON staff FOR DELETE
    TO authenticated
    USING (true);

-- Similar policies for documents
CREATE POLICY "Allow authenticated users to insert documents"
    ON documents FOR INSERT
    TO authenticated
    WITH CHECK (true);

CREATE POLICY "Allow authenticated users to update documents"
    ON documents FOR UPDATE
    TO authenticated
    USING (true);

CREATE POLICY "Allow authenticated users to delete documents"
    ON documents FOR DELETE
    TO authenticated
    USING (true);

-- Similar policies for school_info
CREATE POLICY "Allow authenticated users to update school_info"
    ON school_info FOR UPDATE
    TO authenticated
    USING (true);

CREATE POLICY "Allow authenticated users to insert school_info"
    ON school_info FOR INSERT
    TO authenticated
    WITH CHECK (true);

-- Insert sample data (optional)
-- Uncomment to populate with sample data

-- Sample school info
INSERT INTO school_info (school_name, address, city, state, zip_code, country, phone, email, principal_name, established_year, total_students, total_staff, total_classrooms)
VALUES ('Greenwood High School', '123 Education Lane', 'Springfield', 'IL', '62701', 'USA', '+1 (555) 100-0000', 'info@greenwood.edu', 'Dr. Jane Anderson', 1985, 500, 50, 30)
ON CONFLICT DO NOTHING;

-- Sample students
INSERT INTO students (student_id, name, email, phone, grade, gpa, status, date_of_birth, guardian_name, enrollment_date, class_section, attendance_rate)
VALUES 
    ('STU001', 'Alice Johnson', 'alice.j@school.edu', '+1 (555) 123-4567', '10th Grade', 3.8, 'active', '2008-01-15', 'John Johnson', '2023-09-15', 'A', 95.0),
    ('STU002', 'Bob Smith', 'bob.s@school.edu', '+1 (555) 234-5678', '11th Grade', 3.5, 'active', '2007-03-22', 'Mary Smith', '2023-09-15', 'B', 92.0),
    ('STU003', 'Carol Williams', 'carol.w@school.edu', '+1 (555) 345-6789', '9th Grade', 3.9, 'active', '2009-07-10', 'David Williams', '2023-09-16', 'A', 97.0),
    ('STU004', 'David Brown', 'david.b@school.edu', '+1 (555) 456-7890', '12th Grade', 3.6, 'inactive', '2006-11-05', 'Lisa Brown', '2022-09-14', 'C', 88.0)
ON CONFLICT DO NOTHING;

-- Sample staff
INSERT INTO staff (staff_id, name, email, phone, position, department, status, join_date, employment_type, office_location)
VALUES 
    ('STF001', 'John Smith', 'john.s@school.edu', '+1 (555) 123-4567', 'Principal', 'Administration', 'active', '2015-01-15', 'full-time', 'Room 101'),
    ('STF002', 'Sarah Johnson', 'sarah.j@school.edu', '+1 (555) 234-5678', 'Vice Principal', 'Administration', 'active', '2016-03-10', 'full-time', 'Room 102'),
    ('STF003', 'Michael Chen', 'michael.c@school.edu', '+1 (555) 345-6789', 'Math Teacher', 'Mathematics', 'active', '2018-08-20', 'full-time', 'Room 201'),
    ('STF004', 'Emily Davis', 'emily.d@school.edu', '+1 (555) 456-7890', 'Science Teacher', 'Science', 'leave', '2017-09-01', 'full-time', 'Room 202')
ON CONFLICT DO NOTHING;

-- Create storage bucket for documents (Run this in Supabase Dashboard > Storage)
-- INSERT INTO storage.buckets (id, name, public) VALUES ('documents', 'documents', true);

-- Set up storage policies (Run this in Supabase Dashboard > Storage > Policies)
-- CREATE POLICY "Allow authenticated uploads"
-- ON storage.objects FOR INSERT
-- TO authenticated
-- WITH CHECK (bucket_id = 'documents');

-- CREATE POLICY "Allow authenticated downloads"
-- ON storage.objects FOR SELECT
-- TO authenticated
-- USING (bucket_id = 'documents');

-- CREATE POLICY "Allow authenticated deletes"
-- ON storage.objects FOR DELETE
-- TO authenticated
-- USING (bucket_id = 'documents');

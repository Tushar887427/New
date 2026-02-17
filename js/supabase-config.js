/**
 * Supabase Configuration
 * 
 * Instructions:
 * 1. Create a Supabase project at https://supabase.com
 * 2. Replace the placeholders below with your actual credentials
 * 3. Run the SQL schema from SUPABASE_SCHEMA.sql in your Supabase SQL editor
 * 4. Uncomment the supabase client initialization
 */

// Supabase Configuration
const SUPABASE_CONFIG = {
    url: 'YOUR_SUPABASE_PROJECT_URL', // e.g., 'https://xxxxx.supabase.co'
    anonKey: 'YOUR_SUPABASE_ANON_KEY' // Your anon/public key
};

// Uncomment after adding your credentials and including Supabase JS library
// <script src="https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2"></script>
// const supabase = window.supabase.createClient(SUPABASE_CONFIG.url, SUPABASE_CONFIG.anonKey);

/**
 * Database Helper Functions
 * Uncomment and use these after setting up Supabase
 */

// Get all students
async function getAllStudents() {
    try {
        const { data, error } = await supabase
            .from('students')
            .select('*')
            .order('created_at', { ascending: false });
        
        if (error) throw error;
        return data;
    } catch (error) {
        console.error('Error fetching students:', error);
        return [];
    }
}

// Get student by ID
async function getStudentById(id) {
    try {
        const { data, error } = await supabase
            .from('students')
            .select('*')
            .eq('student_id', id)
            .single();
        
        if (error) throw error;
        return data;
    } catch (error) {
        console.error('Error fetching student:', error);
        return null;
    }
}

// Add new student
async function addStudent(studentData) {
    try {
        const { data, error } = await supabase
            .from('students')
            .insert([studentData])
            .select();
        
        if (error) throw error;
        return data[0];
    } catch (error) {
        console.error('Error adding student:', error);
        return null;
    }
}

// Update student
async function updateStudent(id, updates) {
    try {
        const { data, error } = await supabase
            .from('students')
            .update(updates)
            .eq('student_id', id)
            .select();
        
        if (error) throw error;
        return data[0];
    } catch (error) {
        console.error('Error updating student:', error);
        return null;
    }
}

// Delete student
async function deleteStudent(id) {
    try {
        const { error } = await supabase
            .from('students')
            .delete()
            .eq('student_id', id);
        
        if (error) throw error;
        return true;
    } catch (error) {
        console.error('Error deleting student:', error);
        return false;
    }
}

// Get all staff
async function getAllStaff() {
    try {
        const { data, error } = await supabase
            .from('staff')
            .select('*')
            .order('created_at', { ascending: false });
        
        if (error) throw error;
        return data;
    } catch (error) {
        console.error('Error fetching staff:', error);
        return [];
    }
}

// Get staff by ID
async function getStaffById(id) {
    try {
        const { data, error } = await supabase
            .from('staff')
            .select('*')
            .eq('staff_id', id)
            .single();
        
        if (error) throw error;
        return data;
    } catch (error) {
        console.error('Error fetching staff:', error);
        return null;
    }
}

// Add new staff member
async function addStaff(staffData) {
    try {
        const { data, error } = await supabase
            .from('staff')
            .insert([staffData])
            .select();
        
        if (error) throw error;
        return data[0];
    } catch (error) {
        console.error('Error adding staff:', error);
        return null;
    }
}

// Get all documents
async function getAllDocuments() {
    try {
        const { data, error } = await supabase
            .from('documents')
            .select('*')
            .order('uploaded_at', { ascending: false });
        
        if (error) throw error;
        return data;
    } catch (error) {
        console.error('Error fetching documents:', error);
        return [];
    }
}

// Upload document
async function uploadDocument(file, metadata) {
    try {
        // Upload file to storage
        const fileName = `${Date.now()}_${file.name}`;
        const { data: fileData, error: uploadError } = await supabase.storage
            .from('documents')
            .upload(fileName, file);
        
        if (uploadError) throw uploadError;

        // Get public URL
        const { data: urlData } = supabase.storage
            .from('documents')
            .getPublicUrl(fileName);

        // Save metadata to database
        const { data, error } = await supabase
            .from('documents')
            .insert([{
                ...metadata,
                file_url: urlData.publicUrl,
                file_name: fileName
            }])
            .select();
        
        if (error) throw error;
        return data[0];
    } catch (error) {
        console.error('Error uploading document:', error);
        return null;
    }
}

// Get school info
async function getSchoolInfo() {
    try {
        const { data, error } = await supabase
            .from('school_info')
            .select('*')
            .single();
        
        if (error) throw error;
        return data;
    } catch (error) {
        console.error('Error fetching school info:', error);
        return null;
    }
}

// Update school info
async function updateSchoolInfo(updates) {
    try {
        const { data, error } = await supabase
            .from('school_info')
            .upsert(updates)
            .select();
        
        if (error) throw error;
        return data[0];
    } catch (error) {
        console.error('Error updating school info:', error);
        return null;
    }
}

// User authentication
async function signIn(email, password) {
    try {
        const { data, error } = await supabase.auth.signInWithPassword({
            email,
            password
        });
        
        if (error) throw error;
        return data;
    } catch (error) {
        console.error('Error signing in:', error);
        return null;
    }
}

async function signOut() {
    try {
        const { error } = await supabase.auth.signOut();
        if (error) throw error;
        return true;
    } catch (error) {
        console.error('Error signing out:', error);
        return false;
    }
}

// Check if user is authenticated
async function getCurrentUser() {
    try {
        const { data: { user } } = await supabase.auth.getUser();
        return user;
    } catch (error) {
        console.error('Error getting current user:', error);
        return null;
    }
}

// Export functions for use in other files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        getAllStudents,
        getStudentById,
        addStudent,
        updateStudent,
        deleteStudent,
        getAllStaff,
        getStaffById,
        addStaff,
        getAllDocuments,
        uploadDocument,
        getSchoolInfo,
        updateSchoolInfo,
        signIn,
        signOut,
        getCurrentUser
    };
}

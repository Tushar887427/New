# Google Sheets Import Guide

This guide explains how to import student and staff data from Google Sheets into the School Management System.

## 📋 Prerequisites

1. A Google account with access to Google Sheets
2. Your data organized in Google Sheets
3. Access to the School Management System settings page

## 📊 Data Format

### Students Sheet Format

Your Google Sheets should have the following columns (in any order):

| Column Name | Type | Required | Example |
|------------|------|----------|---------|
| Student ID | Text | Yes | STU001 |
| Name | Text | Yes | Alice Johnson |
| Email | Text | Yes | alice.j@school.edu |
| Phone | Text | No | +1 (555) 123-4567 |
| Grade | Text | Yes | 10th Grade |
| GPA | Number | No | 3.8 |
| Status | Text | No | active |
| Date of Birth | Date | No | 01/15/2008 |
| Guardian | Text | No | John Johnson |
| Enrollment Date | Date | No | 09/15/2023 |
| Section | Text | No | A |
| Attendance | Number | No | 95 |

**Example Sheet:**
```
Student ID | Name           | Email               | Phone            | Grade      | GPA | Status
STU001     | Alice Johnson  | alice.j@school.edu  | +1 (555) 123-4567| 10th Grade | 3.8 | active
STU002     | Bob Smith      | bob.s@school.edu    | +1 (555) 234-5678| 11th Grade | 3.5 | active
STU003     | Carol Williams | carol.w@school.edu  | +1 (555) 345-6789| 9th Grade  | 3.9 | active
```

### Staff Sheet Format

| Column Name | Type | Required | Example |
|------------|------|----------|---------|
| Staff ID | Text | Yes | STF001 |
| Name | Text | Yes | John Smith |
| Email | Text | Yes | john.s@school.edu |
| Phone | Text | No | +1 (555) 123-4567 |
| Position | Text | Yes | Principal |
| Department | Text | Yes | Administration |
| Status | Text | No | active |
| Join Date | Date | No | 01/15/2015 |
| Employment Type | Text | No | Full-time |
| Office | Text | No | Room 101 |

**Example Sheet:**
```
Staff ID | Name        | Email              | Phone            | Position  | Department     | Status
STF001   | John Smith  | john.s@school.edu  | +1 (555) 123-4567| Principal | Administration | active
STF002   | Sarah Johnson| sarah.j@school.edu| +1 (555) 234-5678| Vice Principal | Administration | active
```

## 🔗 Step-by-Step Import Process

### Method 1: Using Google Sheets API (Recommended)

1. **Prepare Your Google Sheet**
   - Open your Google Sheet
   - Ensure the first row contains column headers
   - Verify all required fields are present

2. **Share Your Sheet**
   - Click "Share" button in Google Sheets
   - Set to "Anyone with the link can view"
   - Copy the share link

3. **Get the Sheet ID**
   - Your URL looks like: `https://docs.google.com/spreadsheets/d/SHEET_ID/edit`
   - Copy the `SHEET_ID` part

4. **Import in School Management System**
   - Go to **Settings** page
   - Find "Google Sheets Import" section
   - Paste your Sheet ID
   - Select data type (Students or Staff)
   - Click "Preview Data"
   - Review the data mapping
   - Click "Import Data"

### Method 2: CSV Export & Import

1. **Export from Google Sheets**
   - Open your Google Sheet
   - Go to File > Download > Comma-separated values (.csv)
   - Save the file

2. **Import in School Management System**
   - Go to **Settings** page
   - Find "CSV Import" section
   - Click "Choose File"
   - Select your downloaded CSV
   - Select data type (Students or Staff)
   - Click "Import"

## 🔍 Data Validation

The system will validate your data before importing:

- **Required Fields**: Student/Staff ID, Name, Email
- **Email Format**: Must be valid email address
- **Phone Format**: Should include country code
- **Status**: Must be 'active' or 'inactive'
- **Duplicate IDs**: Will skip or update existing records
- **Invalid Data**: Will show errors and skip invalid rows

## ⚙️ Import Options

### Update Existing Records
- Check "Update existing records" to overwrite data for existing Student/Staff IDs
- Leave unchecked to skip existing records

### Import Mode
- **Add Only**: Only add new records, skip existing ones
- **Update Only**: Only update existing records, skip new ones
- **Add & Update**: Add new records and update existing ones (default)

## 📝 Tips for Successful Import

1. **Clean Your Data**
   - Remove empty rows
   - Ensure consistent formatting
   - Use UTF-8 encoding for special characters

2. **Test with Small Sample**
   - Try importing 5-10 records first
   - Verify data appears correctly
   - Then import the full dataset

3. **Backup Before Import**
   - Export existing data before large imports
   - Keep a copy of your Google Sheet

4. **Check After Import**
   - Review imported records
   - Verify counts match expectations
   - Check for any missing data

## 🔧 Troubleshooting

### Common Issues

**Issue**: "Unable to access Google Sheet"
- **Solution**: Ensure sheet is shared publicly or with specific email
- Check that the Sheet ID is correct

**Issue**: "Invalid data format"
- **Solution**: Verify column names match expected format
- Ensure required fields are not empty

**Issue**: "Some records failed to import"
- **Solution**: Check the error log
- Fix invalid data in Google Sheets
- Try importing again

**Issue**: "Email already exists"
- **Solution**: Enable "Update existing records" option
- Or remove duplicate emails from your sheet

## 📞 Support

If you encounter issues:
1. Check this guide carefully
2. Verify your data format matches the examples
3. Try importing a small sample first
4. Check browser console for error messages

## 🔒 Security & Privacy

- Google Sheets links should be set to "view only"
- Do not share API keys or credentials
- Regular users cannot see import functionality
- All imports are logged with timestamp and user

## 🚀 Advanced: Automated Sync

For regular updates:
1. Keep your Google Sheet as the source of truth
2. Schedule regular imports (weekly/monthly)
3. Use update mode to refresh existing records
4. Monitor import logs for issues

## 📊 Sample Google Sheets Templates

Download pre-formatted templates:
- [Students Template](https://docs.google.com/spreadsheets/d/sample-students)
- [Staff Template](https://docs.google.com/spreadsheets/d/sample-staff)

Make a copy and fill in your data!

---

**Note**: Google Sheets import requires proper API configuration. Follow the setup instructions in the main README.md file.

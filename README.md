# School Management System

A minimalist, visually appealing school management application designed for school clerks to manage students, staff, and school data efficiently.

## 🌟 Features

### Core Features
- **Login System**: Secure authentication for users
- **Dashboard**: Overview of school statistics and quick actions
- **Student Management**: 
  - View all students with search and filters
  - Individual student profiles with detailed information
  - Track student status (active/inactive)
- **Staff Management**:
  - Browse staff directory
  - View staff profiles with contact and employment details
  - Filter by department and status
- **School Information**: Manage school details and contact information
- **Document Management**: Upload, organize, and manage school documents
- **Settings**: Configure system preferences and import data
- **User Profile**: Manage logged-in user account

### Design
- ✨ **Minimalist Design**: Clean and distraction-free interface
- 🎨 **Light Color Theme**: Soft blues (#42a5f5, #64b5f6) for easy viewing
- 📱 **Fully Responsive**: Works seamlessly on desktop, tablet, and mobile
- 🚀 **Fast & Lightweight**: No heavy frameworks, pure HTML/CSS/JS

## 🚀 Getting Started

### Prerequisites
- A modern web browser (Chrome, Firefox, Safari, Edge)
- (Optional) A web server for production deployment
- (Optional) Supabase account for database integration

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Tushar887427/New.git
   cd New
   ```

2. **Open the application**:
   - Simply open `login.html` in your web browser
   - Or use a local server:
     ```bash
     # Using Python
     python -m http.server 8000
     
     # Using Node.js
     npx serve
     ```

3. **Login with demo credentials**:
   - Username: `admin`
   - Password: `admin123`

## 📁 Project Structure

```
New/
├── css/
│   ├── common.css           # Shared styles and utilities
│   └── dashboard.css        # Dashboard layout styles
├── js/
│   ├── common.js            # Utility functions
│   ├── auth.js              # Authentication handling
│   └── navigation.js        # Navigation menu logic
├── login.html               # Login page
├── dashboard.html           # Main dashboard
├── students-list.html       # Students directory
├── student-info.html        # Student detail page
├── staff-list.html          # Staff directory
├── staff-info.html          # Staff detail page
├── school-info.html         # School information
├── documents.html           # Document management
├── settings.html            # System settings
├── profile.html             # User profile
└── README.md                # This file
```

## 🎨 Design Philosophy

The application follows a minimalist design approach with:
- **Light Color Palette**: Blues (#e3f2fd, #bbdefb, #64b5f6, #42a5f5, #1976d2)
- **Clean Typography**: Segoe UI for readability
- **Subtle Shadows**: Gentle elevation for depth
- **Smooth Transitions**: 0.3s ease for interactions
- **Generous Whitespace**: Improved focus and readability

## 📊 Database Integration (Coming Soon)

The application is designed to integrate with **Supabase** for data persistence:

### Planned Database Schema

**Students Table**:
```sql
- id (uuid, primary key)
- student_id (text, unique)
- name (text)
- email (text)
- phone (text)
- grade (text)
- gpa (numeric)
- status (text)
- created_at (timestamp)
```

**Staff Table**:
```sql
- id (uuid, primary key)
- staff_id (text, unique)
- name (text)
- email (text)
- phone (text)
- position (text)
- department (text)
- status (text)
- created_at (timestamp)
```

**Documents Table**:
```sql
- id (uuid, primary key)
- name (text)
- category (text)
- file_url (text)
- file_size (text)
- uploaded_by (text)
- uploaded_at (timestamp)
```

### Setup Supabase

1. Create a project at [supabase.com](https://supabase.com)
2. Create the tables using the schema above
3. Get your API keys from Project Settings
4. Add configuration in `settings.html` or create `js/supabase-config.js`:
   ```javascript
   const SUPABASE_URL = 'your-project-url';
   const SUPABASE_ANON_KEY = 'your-anon-key';
   ```

## 📥 Google Sheets Import

The application includes functionality to import data from Google Sheets:

1. Navigate to **Settings** page
2. Enter your Google Sheets URL
3. Map columns to fields
4. Click "Import Data"

### Supported Formats
- Students: Name, Email, Phone, Grade, GPA, Status
- Staff: Name, Email, Phone, Position, Department, Status

## 🔒 Security

- Client-side authentication (upgrade to server-side for production)
- No sensitive data stored in localStorage
- HTTPS recommended for production deployment
- Supabase Row Level Security (RLS) for data protection

## 🌐 Browser Support

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## 📱 Mobile Support

The application is fully responsive and works on:
- iOS devices (iPhone, iPad)
- Android devices (phones, tablets)
- Touch-friendly navigation
- Optimized mobile layouts

## 🛠️ Customization

### Changing Colors
Edit `css/dashboard.css` and `css/common.css` to customize:
- Primary color: `#42a5f5`
- Secondary color: `#64b5f6`
- Background: `#f5f7fa`

### Adding Pages
1. Create new HTML file
2. Include the sidebar navigation
3. Link required CSS and JS files
4. Add navigation link in all page sidebars

## 📝 Current Limitations

- Data is stored in browser localStorage (temporary)
- No real-time collaboration
- Limited to single-user at a time
- File uploads are simulated (not actual file storage)

These will be addressed with Supabase integration.

## 🚧 Roadmap

- [ ] Complete Supabase integration
- [ ] Implement Google Sheets import
- [ ] Add data export functionality
- [ ] Real-time notifications
- [ ] Multi-user support with roles
- [ ] Advanced reporting and analytics
- [ ] Email notifications
- [ ] Mobile app (PWA)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is open source and available under the MIT License.

## 👤 Author

**Tushar887427**
- GitHub: [@Tushar887427](https://github.com/Tushar887427)

## 🙏 Acknowledgments

- Icons: Unicode emoji
- Fonts: System fonts (Segoe UI)
- Inspiration: Modern open-source management apps

---

**Note**: This is a frontend-focused application. For production use, implement proper backend authentication and database integration with Supabase or similar services.

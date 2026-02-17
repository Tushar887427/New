const fs = require('fs');
const path = require('path');

// Create www directory if it doesn't exist
if (!fs.existsSync('www')) {
    fs.mkdirSync('www', { recursive: true });
}

// Files and directories to copy
const itemsToCopy = [
    'css',
    'js',
    'login.html',
    'dashboard.html',
    'students-list.html',
    'student-info.html',
    'staff-list.html',
    'staff-info.html',
    'school-info.html',
    'documents.html',
    'settings.html',
    'profile.html'
];

// Copy function
function copyRecursive(src, dest) {
    const stats = fs.statSync(src);
    
    if (stats.isDirectory()) {
        if (!fs.existsSync(dest)) {
            fs.mkdirSync(dest, { recursive: true });
        }
        const files = fs.readdirSync(src);
        files.forEach(file => {
            copyRecursive(path.join(src, file), path.join(dest, file));
        });
    } else {
        fs.copyFileSync(src, dest);
    }
}

// Copy all items
console.log('Building app for www directory...');
itemsToCopy.forEach(item => {
    const src = path.join(__dirname, item);
    const dest = path.join(__dirname, 'www', item);
    
    if (fs.existsSync(src)) {
        console.log(`Copying ${item}...`);
        copyRecursive(src, dest);
    } else {
        console.warn(`Warning: ${item} not found, skipping...`);
    }
});

// Create index.html that redirects to login.html
const indexContent = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>School Management System</title>
    <script>
        window.location.href = 'login.html';
    </script>
</head>
<body>
    <p>Loading School Management System...</p>
</body>
</html>`;

fs.writeFileSync(path.join(__dirname, 'www', 'index.html'), indexContent);

console.log('Build completed successfully!');
console.log('Files copied to www directory.');

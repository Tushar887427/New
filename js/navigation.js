/**
 * Navigation Module
 * Handles sidebar navigation and menu interactions
 */

document.addEventListener('DOMContentLoaded', function() {
    const sidebar = document.getElementById('sidebar');
    const menuBtn = document.getElementById('menuBtn');
    const closeSidebar = document.getElementById('closeSidebar');
    const logoutBtn = document.getElementById('logoutBtn');

    // Menu toggle
    if (menuBtn) {
        menuBtn.addEventListener('click', () => {
            sidebar.classList.toggle('active');
        });
    }

    // Close sidebar
    if (closeSidebar) {
        closeSidebar.addEventListener('click', () => {
            sidebar.classList.remove('active');
        });
    }

    // Close sidebar when clicking outside on mobile
    document.addEventListener('click', (e) => {
        if (window.innerWidth <= 768) {
            if (sidebar && !sidebar.contains(e.target) && menuBtn && !menuBtn.contains(e.target)) {
                sidebar.classList.remove('active');
            }
        }
    });

    // Logout functionality
    if (logoutBtn) {
        logoutBtn.addEventListener('click', (e) => {
            e.preventDefault();
            if (confirm('Are you sure you want to logout?')) {
                logout();
            }
        });
    }

    // Set user name in header
    const userSession = getCurrentUser();
    const userNameElement = document.getElementById('userName');
    if (userSession && userSession.username && userNameElement) {
        userNameElement.textContent = userSession.username;
    }
});

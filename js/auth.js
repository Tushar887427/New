/**
 * Authentication Module
 * Handles user authentication and session management
 */

// Check if user is authenticated
function checkAuth() {
    const userSession = getStorage('userSession') || 
                        JSON.parse(sessionStorage.getItem('userSession') || 'null');
    
    if (!userSession || !userSession.loggedIn) {
        window.location.href = 'login.html';
        return false;
    }
    return true;
}

// Get current user
function getCurrentUser() {
    const userSession = getStorage('userSession') || 
                        JSON.parse(sessionStorage.getItem('userSession') || 'null');
    return userSession;
}

// Logout user
function logout() {
    removeStorage('userSession');
    sessionStorage.removeItem('userSession');
    window.location.href = 'login.html';
}

// Initialize auth check on protected pages
if (window.location.pathname !== '/login.html' && !window.location.pathname.endsWith('login.html')) {
    window.addEventListener('DOMContentLoaded', checkAuth);
}

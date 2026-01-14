/**
 * Common JavaScript Functions for Multi-Page Website
 * Shared utilities and helper functions used across all pages
 * Last Updated: 2026-01-14
 */

// ============================================================================
// DOM Utilities
// ============================================================================

/**
 * Get element by ID with error handling
 * @param {string} id - Element ID
 * @returns {Element|null} - The element or null if not found
 */
function getElementById(id) {
  const element = document.getElementById(id);
  if (!element) {
    console.warn(`Element with ID '${id}' not found`);
  }
  return element;
}

/**
 * Get elements by class name
 * @param {string} className - Class name to search for
 * @returns {HTMLCollection} - Collection of matching elements
 */
function getElementByClass(className) {
  return document.getElementsByClassName(className);
}

/**
 * Query selector wrapper with error handling
 * @param {string} selector - CSS selector
 * @returns {Element|null} - First matching element or null
 */
function querySelector(selector) {
  return document.querySelector(selector);
}

/**
 * Query all selector wrapper
 * @param {string} selector - CSS selector
 * @returns {NodeList} - All matching elements
 */
function querySelectorAll(selector) {
  return document.querySelectorAll(selector);
}

// ============================================================================
// Event Handling
// ============================================================================

/**
 * Add event listener with error handling
 * @param {Element} element - Target element
 * @param {string} eventType - Event type (e.g., 'click', 'submit')
 * @param {Function} callback - Callback function
 */
function addEventListener(element, eventType, callback) {
  if (element) {
    element.addEventListener(eventType, callback);
  } else {
    console.warn('addEventListener: Element not provided');
  }
}

/**
 * Remove event listener
 * @param {Element} element - Target element
 * @param {string} eventType - Event type
 * @param {Function} callback - Callback function
 */
function removeEventListener(element, eventType, callback) {
  if (element) {
    element.removeEventListener(eventType, callback);
  }
}

/**
 * Delegate event handling to parent element
 * @param {Element} parent - Parent element
 * @param {string} eventType - Event type
 * @param {string} selector - CSS selector for child elements
 * @param {Function} callback - Callback function
 */
function addEventDelegation(parent, eventType, selector, callback) {
  if (!parent) return;
  
  parent.addEventListener(eventType, (event) => {
    if (event.target.matches(selector)) {
      callback(event);
    }
  });
}

// ============================================================================
// String Utilities
// ============================================================================

/**
 * Capitalize first letter of a string
 * @param {string} str - Input string
 * @returns {string} - Capitalized string
 */
function capitalizeFirstLetter(str) {
  if (!str) return '';
  return str.charAt(0).toUpperCase() + str.slice(1);
}

/**
 * Trim and normalize whitespace
 * @param {string} str - Input string
 * @returns {string} - Normalized string
 */
function normalizeString(str) {
  return str.trim().replace(/\s+/g, ' ');
}

/**
 * Truncate string with ellipsis
 * @param {string} str - Input string
 * @param {number} maxLength - Maximum length
 * @returns {string} - Truncated string
 */
function truncateString(str, maxLength) {
  if (str.length <= maxLength) return str;
  return str.substring(0, maxLength - 3) + '...';
}

/**
 * Check if string contains a value (case-insensitive)
 * @param {string} str - Input string
 * @param {string} value - Value to search for
 * @returns {boolean} - True if contains
 */
function containsIgnoreCase(str, value) {
  return str.toLowerCase().includes(value.toLowerCase());
}

// ============================================================================
// Array Utilities
// ============================================================================

/**
 * Remove duplicates from array
 * @param {Array} arr - Input array
 * @returns {Array} - Array with unique values
 */
function getUnique(arr) {
  return [...new Set(arr)];
}

/**
 * Flatten nested array
 * @param {Array} arr - Input array
 * @returns {Array} - Flattened array
 */
function flattenArray(arr) {
  return arr.flat(Infinity);
}

/**
 * Group array by property
 * @param {Array} arr - Input array
 * @param {string} property - Property to group by
 * @returns {Object} - Grouped object
 */
function groupBy(arr, property) {
  return arr.reduce((acc, obj) => {
    const key = obj[property];
    if (!acc[key]) {
      acc[key] = [];
    }
    acc[key].push(obj);
    return acc;
  }, {});
}

// ============================================================================
// Number Utilities
// ============================================================================

/**
 * Format number with commas
 * @param {number} num - Input number
 * @returns {string} - Formatted number
 */
function formatNumber(num) {
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

/**
 * Round number to decimal places
 * @param {number} num - Input number
 * @param {number} decimals - Number of decimal places
 * @returns {number} - Rounded number
 */
function roundNumber(num, decimals = 2) {
  return Math.round(num * Math.pow(10, decimals)) / Math.pow(10, decimals);
}

/**
 * Generate random number between min and max
 * @param {number} min - Minimum value
 * @param {number} max - Maximum value
 * @returns {number} - Random number
 */
function getRandomNumber(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

// ============================================================================
// Date/Time Utilities
// ============================================================================

/**
 * Format date to readable string
 * @param {Date} date - Date object
 * @param {string} format - Format string (default: 'DD/MM/YYYY')
 * @returns {string} - Formatted date
 */
function formatDate(date, format = 'DD/MM/YYYY') {
  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const year = date.getFullYear();
  
  return format
    .replace('DD', day)
    .replace('MM', month)
    .replace('YYYY', year);
}

/**
 * Get current date and time
 * @returns {Object} - Object with date and time properties
 */
function getCurrentDateTime() {
  const now = new Date();
  return {
    date: formatDate(now),
    time: now.toLocaleTimeString(),
    timestamp: now.getTime()
  };
}

/**
 * Calculate days between two dates
 * @param {Date} date1 - First date
 * @param {Date} date2 - Second date
 * @returns {number} - Number of days
 */
function daysBetween(date1, date2) {
  const oneDay = 24 * 60 * 60 * 1000;
  return Math.round(Math.abs((date1 - date2) / oneDay));
}

// ============================================================================
// Local Storage Utilities
// ============================================================================

/**
 * Set item in localStorage
 * @param {string} key - Storage key
 * @param {*} value - Value to store (will be JSON stringified)
 */
function setStorage(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch (error) {
    console.error(`Error setting localStorage for key '${key}':`, error);
  }
}

/**
 * Get item from localStorage
 * @param {string} key - Storage key
 * @returns {*} - Stored value or null
 */
function getStorage(key) {
  try {
    const item = localStorage.getItem(key);
    return item ? JSON.parse(item) : null;
  } catch (error) {
    console.error(`Error getting localStorage for key '${key}':`, error);
    return null;
  }
}

/**
 * Remove item from localStorage
 * @param {string} key - Storage key
 */
function removeStorage(key) {
  try {
    localStorage.removeItem(key);
  } catch (error) {
    console.error(`Error removing localStorage for key '${key}':`, error);
  }
}

/**
 * Clear all localStorage
 */
function clearStorage() {
  try {
    localStorage.clear();
  } catch (error) {
    console.error('Error clearing localStorage:', error);
  }
}

// ============================================================================
// HTTP/AJAX Utilities
// ============================================================================

/**
 * Fetch data from API
 * @param {string} url - API endpoint
 * @param {Object} options - Fetch options
 * @returns {Promise} - Promise resolving to JSON response
 */
async function fetchData(url, options = {}) {
  try {
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers
      },
      ...options
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Fetch error:', error);
    throw error;
  }
}

/**
 * POST request helper
 * @param {string} url - API endpoint
 * @param {Object} data - Data to send
 * @returns {Promise} - Promise resolving to JSON response
 */
function postData(url, data) {
  return fetchData(url, {
    method: 'POST',
    body: JSON.stringify(data)
  });
}

/**
 * PUT request helper
 * @param {string} url - API endpoint
 * @param {Object} data - Data to send
 * @returns {Promise} - Promise resolving to JSON response
 */
function putData(url, data) {
  return fetchData(url, {
    method: 'PUT',
    body: JSON.stringify(data)
  });
}

/**
 * DELETE request helper
 * @param {string} url - API endpoint
 * @returns {Promise} - Promise resolving to JSON response
 */
function deleteData(url) {
  return fetchData(url, {
    method: 'DELETE'
  });
}

// ============================================================================
// Validation Utilities
// ============================================================================

/**
 * Validate email address
 * @param {string} email - Email address
 * @returns {boolean} - True if valid
 */
function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

/**
 * Validate phone number
 * @param {string} phone - Phone number
 * @returns {boolean} - True if valid
 */
function isValidPhone(phone) {
  const phoneRegex = /^[\d\s\-\+\(\)]+$/;
  return phoneRegex.test(phone) && phone.replace(/\D/g, '').length >= 10;
}

/**
 * Check if value is empty
 * @param {*} value - Value to check
 * @returns {boolean} - True if empty
 */
function isEmpty(value) {
  return value === null || value === undefined || value === '' || 
         (Array.isArray(value) && value.length === 0) ||
         (typeof value === 'object' && Object.keys(value).length === 0);
}

/**
 * Check if value is a number
 * @param {*} value - Value to check
 * @returns {boolean} - True if number
 */
function isNumber(value) {
  return !isNaN(value) && value !== null && value !== '';
}

// ============================================================================
// CSS/Style Utilities
// ============================================================================

/**
 * Add class to element
 * @param {Element} element - Target element
 * @param {string} className - Class name to add
 */
function addClass(element, className) {
  if (element) {
    element.classList.add(className);
  }
}

/**
 * Remove class from element
 * @param {Element} element - Target element
 * @param {string} className - Class name to remove
 */
function removeClass(element, className) {
  if (element) {
    element.classList.remove(className);
  }
}

/**
 * Toggle class on element
 * @param {Element} element - Target element
 * @param {string} className - Class name to toggle
 */
function toggleClass(element, className) {
  if (element) {
    element.classList.toggle(className);
  }
}

/**
 * Check if element has class
 * @param {Element} element - Target element
 * @param {string} className - Class name to check
 * @returns {boolean} - True if element has class
 */
function hasClass(element, className) {
  return element ? element.classList.contains(className) : false;
}

/**
 * Set multiple styles on element
 * @param {Element} element - Target element
 * @param {Object} styles - Object with style properties
 */
function setStyles(element, styles) {
  if (!element) return;
  Object.assign(element.style, styles);
}

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Debounce function calls
 * @param {Function} func - Function to debounce
 * @param {number} wait - Wait time in milliseconds
 * @returns {Function} - Debounced function
 */
function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

/**
 * Throttle function calls
 * @param {Function} func - Function to throttle
 * @param {number} limit - Time limit in milliseconds
 * @returns {Function} - Throttled function
 */
function throttle(func, limit) {
  let inThrottle;
  return function(...args) {
    if (!inThrottle) {
      func.apply(this, args);
      inThrottle = true;
      setTimeout(() => (inThrottle = false), limit);
    }
  };
}

/**
 * Wait/sleep for specified milliseconds
 * @param {number} ms - Milliseconds to wait
 * @returns {Promise} - Promise that resolves after delay
 */
function wait(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Copy text to clipboard
 * @param {string} text - Text to copy
 * @returns {Promise} - Promise that resolves when copied
 */
async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch (error) {
    console.error('Error copying to clipboard:', error);
    return false;
  }
}

/**
 * Log message with timestamp
 * @param {string} message - Message to log
 * @param {string} type - Log type ('info', 'warn', 'error')
 */
function logMessage(message, type = 'info') {
  const timestamp = new Date().toLocaleTimeString();
  const prefix = `[${timestamp}]`;
  console.log(`${prefix} ${message}`);
}

// Export for module systems (if needed)
if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    // DOM utilities
    getElementById, getElementByClass, querySelector, querySelectorAll,
    // Event handling
    addEventListener, removeEventListener, addEventDelegation,
    // String utilities
    capitalizeFirstLetter, normalizeString, truncateString, containsIgnoreCase,
    // Array utilities
    getUnique, flattenArray, groupBy,
    // Number utilities
    formatNumber, roundNumber, getRandomNumber,
    // Date/Time utilities
    formatDate, getCurrentDateTime, daysBetween,
    // Local storage
    setStorage, getStorage, removeStorage, clearStorage,
    // HTTP/AJAX
    fetchData, postData, putData, deleteData,
    // Validation
    isValidEmail, isValidPhone, isEmpty, isNumber,
    // CSS/Style
    addClass, removeClass, toggleClass, hasClass, setStyles,
    // Utilities
    debounce, throttle, wait, copyToClipboard, logMessage
  };
}

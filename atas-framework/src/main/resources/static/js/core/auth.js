/**
 * Authentication Module
 * Handles token storage, validation, and redirect logic
 * 
 * @module core/auth
 */

import { config } from '../config.js';

/**
 * Get authentication token from storage
 * @returns {string|null} Access token or null if not found
 */
export function getAuthToken() {
    return localStorage.getItem(config.security.tokenStorageKey);
}

/**
 * Get refresh token from storage
 * @returns {string|null} Refresh token or null if not found
 */
export function getRefreshToken() {
    return localStorage.getItem(config.security.refreshTokenStorageKey);
}

/**
 * Store authentication tokens
 * @param {string} accessToken - Access token
 * @param {string} [refreshToken] - Optional refresh token
 */
export function storeTokens(accessToken, refreshToken = null) {
    localStorage.setItem(config.security.tokenStorageKey, accessToken);
    if (refreshToken) {
        localStorage.setItem(config.security.refreshTokenStorageKey, refreshToken);
    }
}

/**
 * Clear authentication tokens
 */
export function clearTokens() {
    localStorage.removeItem(config.security.tokenStorageKey);
    localStorage.removeItem(config.security.refreshTokenStorageKey);
}

/**
 * Validate redirect URL against whitelist to prevent open redirect attacks
 * @param {string} redirectUrl - URL to validate
 * @returns {string} Safe redirect URL or default dashboard
 */
export function getSafeRedirect(redirectUrl = null) {
    // Get from URL params if not provided
    if (!redirectUrl) {
        const urlParams = new URLSearchParams(window.location.search);
        redirectUrl = urlParams.get('redirect');
    }
    
    if (!redirectUrl) {
        return config.security.allowedRedirectPaths[0]; // Default to dashboard
    }
    
    // Use URL constructor for strict validation
    try {
        // If it's a relative path, construct full URL for validation
        const testUrl = redirectUrl.startsWith('/') 
            ? new URL(redirectUrl, window.location.origin)
            : new URL(redirectUrl);
        
        // Must match current origin
        if (testUrl.origin !== window.location.origin) {
            console.warn('Redirect URL origin mismatch, using default');
            return config.security.allowedRedirectPaths[0];
        }
        
        // Must be in whitelist
        const path = testUrl.pathname;
        if (config.security.allowedRedirectPaths.includes(path)) {
            return path;
        }
        
        console.warn('Redirect URL not in whitelist, using default');
        return config.security.allowedRedirectPaths[0];
    } catch (e) {
        console.warn('Invalid redirect URL format, using default:', e);
        return config.security.allowedRedirectPaths[0];
    }
}

/**
 * Redirect to login page with safe redirect parameter
 * @param {string} [currentPath] - Current path to redirect back to after login
 */
export function redirectToLogin(currentPath = null) {
    const redirect = currentPath || window.location.pathname;
    const safeRedirect = getSafeRedirect(redirect);
    window.location.href = `/login?redirect=${encodeURIComponent(safeRedirect)}`;
}

/**
 * Check if user is authenticated (has valid token)
 * @returns {boolean} True if token exists
 */
export function isAuthenticated() {
    return !!getAuthToken();
}

/**
 * Validate token by making a profile request
 * @returns {Promise<boolean>} True if token is valid
 */
export async function validateToken() {
    const token = getAuthToken();
    if (!token) {
        return false;
    }
    
    try {
        const response = await fetch(config.api.endpoints.auth.profile, {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        return response.ok;
    } catch (e) {
        console.error('Token validation failed:', e);
        return false;
    }
}


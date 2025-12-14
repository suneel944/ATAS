/**
 * API Client Module
 * Handles authenticated fetch requests with error handling and retry logic
 * 
 * @module core/api
 */

import { getAuthToken, clearTokens, redirectToLogin } from './auth.js';
import { config } from '../config.js';

/**
 * Create full API URL
 * @param {string} endpoint - API endpoint path
 * @returns {string} Full URL
 */
function buildApiUrl(endpoint) {
    return `${config.api.baseUrl}${endpoint}`;
}

/**
 * Authenticated fetch with automatic token injection and error handling
 * @param {string} url - API endpoint URL
 * @param {RequestInit} [options={}] - Fetch options
 * @returns {Promise<Response>} Fetch response
 */
export async function authenticatedFetch(url, options = {}) {
    const token = getAuthToken();
    
    if (!token) {
        redirectToLogin();
        return Promise.reject(new Error('Not authenticated'));
    }
    
    const headers = {
        'Authorization': `Bearer ${token}`,
        ...options.headers
    };
    
    try {
        const response = await fetch(buildApiUrl(url), {
            ...options,
            headers
        });
        
        // Handle authentication errors
        if (response.status === 401 || response.status === 403) {
            clearTokens();
            redirectToLogin();
            return Promise.reject(new Error('Authentication failed'));
        }
        
        return response;
    } catch (error) {
        console.error('API request failed:', error);
        throw error;
    }
}

/**
 * Fetch with exponential backoff retry logic
 * @param {string} url - API endpoint URL
 * @param {RequestInit} [options={}] - Fetch options
 * @param {number} [maxRetries=3] - Maximum number of retries
 * @returns {Promise<Response>} Fetch response
 */
export async function fetchWithRetry(url, options = {}, maxRetries = 3) {
    const backoff = config.polling.exponentialBackoff;
    let delay = backoff.initialDelay;
    
    for (let attempt = 0; attempt <= maxRetries; attempt++) {
        try {
            const response = await authenticatedFetch(url, options);
            
            // If successful or client error (4xx), don't retry
            if (response.ok || (response.status >= 400 && response.status < 500)) {
                return response;
            }
            
            // Server error (5xx), retry
            if (attempt < maxRetries) {
                await new Promise(resolve => setTimeout(resolve, delay));
                delay = Math.min(delay * backoff.multiplier, backoff.maxDelay);
                continue;
            }
            
            return response;
        } catch (error) {
            if (attempt < maxRetries) {
                await new Promise(resolve => setTimeout(resolve, delay));
                delay = Math.min(delay * backoff.multiplier, backoff.maxDelay);
                continue;
            }
            throw error;
        }
    }
}

/**
 * Parse JSON response with error handling
 * @param {Response} response - Fetch response
 * @returns {Promise<any>} Parsed JSON data
 */
export async function parseJsonResponse(response) {
    try {
        return await response.json();
    } catch (e) {
        throw new Error('Server error: Invalid response format');
    }
}


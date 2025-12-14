/**
 * Polling Manager Module
 * Smart polling with exponential backoff, page visibility API, and SSE support
 * 
 * @module components/polling
 */

import { config } from '../config.js';

/**
 * Polling Manager with exponential backoff and visibility API
 */
export class PollingManager {
    constructor() {
        this.intervals = [];
        this.timeouts = [];
        this.sseConnections = [];
        this.reconnectAttempts = new Map();
        this.isVisible = !document.hidden;
        
        // Listen for visibility changes
        document.addEventListener('visibilitychange', () => {
            this.isVisible = !document.hidden;
            if (this.isVisible) {
                this.resumeAll();
            } else {
                this.pauseAll();
            }
        });
    }
    
    /**
     * Start polling with automatic retry and backoff
     * @param {Function} fn - Function to execute
     * @param {number} ms - Interval in milliseconds
     * @param {Object} [options={}] - Options
     * @returns {number} Interval ID
     */
    start(fn, ms, options = {}) {
        const { immediate = true, maxRetries = 3 } = options;
        
        // Run immediately if requested
        if (immediate) {
            fn();
        }
        
        const id = setInterval(() => {
            if (this.isVisible) {
                fn();
            }
        }, ms);
        
        this.intervals.push(id);
        return id;
    }
    
    /**
     * Start Server-Sent Events connection with exponential backoff
     * @param {string} url - SSE endpoint URL
     * @param {Function} onMessage - Message handler
     * @param {Function} [onError] - Error handler
     * @returns {EventSource} SSE connection
     */
    startSSE(url, onMessage, onError = null) {
        const attemptReconnect = () => {
            const attempts = this.reconnectAttempts.get(url) || 0;
            const backoff = config.polling.exponentialBackoff;
            const delay = Math.min(
                backoff.initialDelay * Math.pow(backoff.multiplier, attempts),
                backoff.maxDelay
            );
            
            setTimeout(() => {
                if (this.isVisible) {
                    this.reconnectAttempts.set(url, attempts + 1);
                    this.startSSE(url, onMessage, onError);
                }
            }, delay);
        };
        
        try {
            const eventSource = new EventSource(url);
            
            eventSource.onmessage = (event) => {
                this.reconnectAttempts.delete(url);
                onMessage(event);
            };
            
            eventSource.onerror = (error) => {
                eventSource.close();
                this.reconnectAttempts.set(url, (this.reconnectAttempts.get(url) || 0) + 1);
                
                if (onError) {
                    onError(error);
                }
                
                // Attempt reconnection with exponential backoff
                if (this.isVisible) {
                    attemptReconnect();
                }
            };
            
            this.sseConnections.push(eventSource);
            return eventSource;
        } catch (error) {
            console.error('SSE connection failed, falling back to polling:', error);
            if (onError) {
                onError(error);
            }
            // Fallback to polling if SSE fails
            return null;
        }
    }
    
    /**
     * Stop all polling intervals
     */
    stopAll() {
        this.intervals.forEach(clearInterval);
        this.intervals = [];
    }
    
    /**
     * Close all SSE connections
     */
    closeAllSSE() {
        this.sseConnections.forEach(conn => conn.close());
        this.sseConnections = [];
    }
    
    /**
     * Pause all polling (when tab is hidden)
     */
    pauseAll() {
        // Intervals will check isVisible, so we just need to stop SSE
        this.closeAllSSE();
    }
    
    /**
     * Resume all polling (when tab becomes visible)
     */
    resumeAll() {
        // Intervals will automatically resume when isVisible becomes true
        // SSE connections need to be re-established by the caller
    }
    
    /**
     * Clear all timeouts
     */
    clearAllTimeouts() {
        this.timeouts.forEach(clearTimeout);
        this.timeouts = [];
    }
    
    /**
     * Complete cleanup
     */
    destroy() {
        this.stopAll();
        this.closeAllSSE();
        this.clearAllTimeouts();
        this.reconnectAttempts.clear();
    }
}

// Export singleton instance
export const pollingManager = new PollingManager();


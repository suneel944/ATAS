/**
 * Spotlight Component
 * Optimized mouse tracking with requestAnimationFrame to prevent layout thrashing
 * 
 * @module components/spotlight
 */

import { throttleRAF } from '../utils/dom.js';

/**
 * Initialize spotlight effect on a card element
 * Uses requestAnimationFrame to throttle updates and prevent layout thrashing
 * @param {HTMLElement} card - Card element with .spotlight-card class
 */
export function initializeSpotlightCard(card) {
    if (!card || !card.classList.contains('spotlight-card')) {
        return;
    }
    
    let rafId = null;
    let mouseX = '50%';
    let mouseY = '50%';
    let isHovering = false;
    
    /**
     * Update spotlight position (throttled via RAF)
     */
    const updateSpotlight = throttleRAF(() => {
        if (isHovering) {
            card.style.setProperty('--mouse-x', mouseX);
            card.style.setProperty('--mouse-y', mouseY);
        }
    });
    
    /**
     * Handle mouse move - calculate position relative to card
     */
    const handleMouseMove = (e) => {
        if (!isHovering) return;
        
        // Calculate position relative to card
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        
        mouseX = `${x}px`;
        mouseY = `${y}px`;
        
        updateSpotlight();
    };
    
    /**
     * Handle mouse enter - show spotlight and initialize position
     */
    const handleMouseEnter = (e) => {
        isHovering = true;
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        mouseX = `${x}px`;
        mouseY = `${y}px`;
        updateSpotlight();
    };
    
    /**
     * Handle mouse leave - hide spotlight
     */
    const handleMouseLeave = () => {
        isHovering = false;
        mouseX = '50%';
        mouseY = '50%';
        card.style.setProperty('--mouse-x', mouseX);
        card.style.setProperty('--mouse-y', mouseY);
    };
    
    // Attach event listeners
    card.addEventListener('mousemove', handleMouseMove, { passive: true });
    card.addEventListener('mouseenter', handleMouseEnter, { passive: true });
    card.addEventListener('mouseleave', handleMouseLeave, { passive: true });
    
    // Cleanup function
    return () => {
        card.removeEventListener('mousemove', handleMouseMove);
        card.removeEventListener('mouseenter', handleMouseEnter);
        card.removeEventListener('mouseleave', handleMouseLeave);
        if (rafId !== null) {
            cancelAnimationFrame(rafId);
        }
    };
}

/**
 * Initialize all spotlight cards on the page
 */
export function initializeAllSpotlightCards() {
    const cards = document.querySelectorAll('.spotlight-card');
    const cleanupFunctions = [];
    
    cards.forEach(card => {
        const cleanup = initializeSpotlightCard(card);
        if (cleanup) {
            cleanupFunctions.push(cleanup);
        }
    });
    
    // Return cleanup function for all cards
    return () => {
        cleanupFunctions.forEach(cleanup => cleanup());
    };
}


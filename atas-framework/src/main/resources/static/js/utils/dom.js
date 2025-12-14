/**
 * DOM Utilities Module
 * XSS-safe HTML manipulation and DOM helpers
 * 
 * @module utils/dom
 */

/**
 * Escape HTML to prevent XSS attacks
 * @param {string} text - Text to escape
 * @returns {string} Escaped HTML string
 */
export function escapeHtml(text) {
    if (text == null) return '';
    
    const div = document.createElement('div');
    div.textContent = String(text);
    return div.innerHTML;
}

/**
 * Create DOM element from template
 * @param {HTMLTemplateElement|string} template - Template element or selector
 * @param {Object} [data={}] - Data to populate template
 * @returns {DocumentFragment} Cloned template content
 */
export function createFromTemplate(template, data = {}) {
    let templateEl;
    
    if (typeof template === 'string') {
        templateEl = document.querySelector(template);
        if (!templateEl) {
            throw new Error(`Template not found: ${template}`);
        }
    } else {
        templateEl = template;
    }
    
    if (!(templateEl instanceof HTMLTemplateElement)) {
        throw new Error('Element is not a template');
    }
    
    const clone = templateEl.content.cloneNode(true);
    
    // Populate data attributes and text content
    if (data) {
        Object.entries(data).forEach(([key, value]) => {
            // Update elements with data-* attributes
            clone.querySelectorAll(`[data-${key}]`).forEach(el => {
                el.textContent = escapeHtml(value);
            });
            
            // Update elements with specific IDs
            const target = clone.querySelector(`#${key}`);
            if (target) {
                if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') {
                    target.value = escapeHtml(value);
                } else {
                    target.textContent = escapeHtml(value);
                }
            }
        });
    }
    
    return clone;
}

/**
 * Safely set innerHTML with XSS protection
 * @param {HTMLElement} element - Target element
 * @param {string} html - HTML string (will be escaped)
 */
export function safeSetInnerHTML(element, html) {
    element.textContent = ''; // Clear first
    const temp = document.createElement('div');
    temp.textContent = html;
    element.appendChild(temp);
}

/**
 * Create element with attributes
 * @param {string} tagName - HTML tag name
 * @param {Object} [attributes={}] - Element attributes
 * @param {string|Node} [content] - Text content or child node
 * @returns {HTMLElement} Created element
 */
export function createElement(tagName, attributes = {}, content = null) {
    const el = document.createElement(tagName);
    
    Object.entries(attributes).forEach(([key, value]) => {
        if (key === 'className') {
            el.className = value;
        } else if (key === 'dataset') {
            Object.entries(value).forEach(([dataKey, dataValue]) => {
                el.dataset[dataKey] = dataValue;
            });
        } else if (key.startsWith('on') && typeof value === 'function') {
            el.addEventListener(key.substring(2).toLowerCase(), value);
        } else {
            el.setAttribute(key, value);
        }
    });
    
    if (content) {
        if (typeof content === 'string') {
            el.textContent = escapeHtml(content);
        } else if (content instanceof Node) {
            el.appendChild(content);
        }
    }
    
    return el;
}

/**
 * Throttle function execution using requestAnimationFrame
 * @param {Function} fn - Function to throttle
 * @returns {Function} Throttled function
 */
export function throttleRAF(fn) {
    let rafId = null;
    
    return function(...args) {
        if (rafId === null) {
            rafId = requestAnimationFrame(() => {
                fn.apply(this, args);
                rafId = null;
            });
        }
    };
}

/**
 * Debounce function execution
 * @param {Function} fn - Function to debounce
 * @param {number} delay - Delay in milliseconds
 * @returns {Function} Debounced function
 */
export function debounce(fn, delay) {
    let timeoutId = null;
    
    return function(...args) {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => {
            fn.apply(this, args);
        }, delay);
    };
}


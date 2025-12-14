/**
 * JSON Viewer Module
 * XSS-safe JSON syntax highlighting using DOM manipulation
 * 
 * @module utils/json-viewer
 */

import { escapeHtml, createElement } from './dom.js';

/**
 * Create a syntax-highlighted JSON viewer element
 * @param {any} json - JSON data to display
 * @param {Object} [options={}] - Display options
 * @returns {HTMLElement} Container element with highlighted JSON
 */
export function createJsonViewer(json, options = {}) {
    const container = createElement('pre', {
        className: 'json-viewer',
        style: 'background: var(--bg-tertiary); padding: 16px; border-radius: 8px; overflow-x: auto; font-family: var(--font-mono); font-size: 12px;'
    });
    
    const formatted = JSON.stringify(json, null, 2);
    const lines = formatted.split('\n');
    
    lines.forEach((line, index) => {
        const lineEl = createElement('div', {
            className: 'json-line'
        });
        
        // Parse and highlight JSON syntax
        const highlighted = highlightJsonLine(line);
        lineEl.appendChild(highlighted);
        
        container.appendChild(lineEl);
    });
    
    return container;
}

/**
 * Highlight a single JSON line
 * @param {string} line - JSON line to highlight
 * @returns {DocumentFragment} Fragment with highlighted elements
 */
function highlightJsonLine(line) {
    const fragment = document.createDocumentFragment();
    
    // Match JSON syntax patterns
    const patterns = [
        { regex: /^(\s*)(\{|\[)/, className: 'json-brace' },
        { regex: /^(\s*)(\}|\])/, className: 'json-brace' },
        { regex: /"([^"]+)":/, className: 'json-key' },
        { regex: /:\s*"([^"]*)"/, className: 'json-string' },
        { regex: /:\s*(\d+\.?\d*)/, className: 'json-number' },
        { regex: /:\s*(true|false|null)/, className: 'json-literal' }
    ];
    
    let remaining = line;
    let lastIndex = 0;
    
    // Find all matches
    const matches = [];
    patterns.forEach(({ regex, className }) => {
        let match;
        while ((match = regex.exec(remaining)) !== null) {
            matches.push({
                index: match.index,
                length: match[0].length,
                className,
                text: match[0]
            });
        }
    });
    
    // Sort matches by index
    matches.sort((a, b) => a.index - b.index);
    
    // Create elements for each match
    matches.forEach(match => {
        // Add text before match
        if (match.index > lastIndex) {
            const text = remaining.substring(lastIndex, match.index);
            if (text) {
                fragment.appendChild(document.createTextNode(escapeHtml(text)));
            }
        }
        
        // Add highlighted match
        const span = createElement('span', {
            className: match.className
        }, match.text);
        fragment.appendChild(span);
        
        lastIndex = match.index + match.length;
    });
    
    // Add remaining text
    if (lastIndex < remaining.length) {
        const text = remaining.substring(lastIndex);
        if (text) {
            fragment.appendChild(document.createTextNode(escapeHtml(text)));
        }
    }
    
    return fragment;
}

/**
 * Add JSON viewer styles to document if not already present
 */
export function injectJsonViewerStyles() {
    if (document.getElementById('json-viewer-styles')) {
        return;
    }
    
    const style = createElement('style', {
        id: 'json-viewer-styles'
    });
    
    style.textContent = `
        .json-viewer { 
            color: var(--text-primary);
            line-height: 1.6;
        }
        .json-viewer .json-key { 
            color: var(--accent-primary);
            font-weight: 600;
        }
        .json-viewer .json-string { 
            color: var(--accent-success);
        }
        .json-viewer .json-number { 
            color: var(--accent-warning);
        }
        .json-viewer .json-literal { 
            color: var(--accent-secondary);
        }
        .json-viewer .json-brace { 
            color: var(--text-secondary);
            font-weight: 700;
        }
    `;
    
    document.head.appendChild(style);
}


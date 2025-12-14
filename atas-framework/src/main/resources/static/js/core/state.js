/**
 * State Management Module
 * Reactive state store using Proxy for automatic UI updates
 * 
 * @module core/state
 */

/**
 * Create a reactive state store
 * @param {Object} initialState - Initial state object
 * @returns {Proxy} Reactive state proxy
 */
export function createStateStore(initialState = {}) {
    const state = { ...initialState };
    
    /**
     * Update UI elements bound to state properties
     * @param {string} prop - Property name
     * @param {*} value - New value
     */
    function updateBoundElements(prop, value) {
        // Update elements with data-bind attribute
        document.querySelectorAll(`[data-bind="${prop}"]`).forEach(el => {
            if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT') {
                el.value = value;
            } else {
                el.textContent = value;
            }
        });
        
        // Update elements with data-bind-format attribute (for formatted values)
        document.querySelectorAll(`[data-bind-format="${prop}"]`).forEach(el => {
            const formatter = el.dataset.format || 'text';
            switch (formatter) {
                case 'number':
                    el.textContent = typeof value === 'number' ? value.toLocaleString() : value;
                    break;
                case 'percent':
                    el.textContent = typeof value === 'number' ? `${value.toFixed(1)}%` : value;
                    break;
                case 'date':
                    el.textContent = value instanceof Date ? value.toLocaleString() : value;
                    break;
                default:
                    el.textContent = value;
            }
        });
    }
    
    return new Proxy(state, {
        set(target, prop, value) {
            const oldValue = target[prop];
            target[prop] = value;
            
            // Only update UI if value actually changed
            if (oldValue !== value) {
                updateBoundElements(prop, value);
            }
            
            return true;
        },
        
        get(target, prop) {
            return target[prop];
        }
    });
}

/**
 * Create a computed property that updates when dependencies change
 * @param {Function} computeFn - Function that computes the value
 * @param {Array<string>} dependencies - Array of state property names to watch
 * @param {Proxy} state - State store
 * @returns {Function} Getter function for computed value
 */
export function createComputed(computeFn, dependencies, state) {
    let cachedValue = null;
    let cachedDeps = null;
    
    return () => {
        const currentDeps = dependencies.map(dep => state[dep]);
        
        // Check if dependencies changed
        if (cachedDeps === null || 
            cachedDeps.length !== currentDeps.length ||
            cachedDeps.some((val, i) => val !== currentDeps[i])) {
            cachedValue = computeFn();
            cachedDeps = [...currentDeps];
        }
        
        return cachedValue;
    };
}


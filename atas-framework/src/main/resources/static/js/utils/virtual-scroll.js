/**
 * Virtual Scrolling Utility
 * Efficiently renders only visible rows in large tables
 * 
 * @module utils/virtual-scroll
 */

/**
 * Virtual Scroll Manager
 * Manages virtual scrolling for a table tbody element
 */
export class VirtualScrollManager {
    constructor(options = {}) {
        this.tbody = options.tbody;
        this.container = options.container; // Scrollable container (table-wrapper)
        this.rowHeight = options.rowHeight || 50; // Estimated row height in pixels
        this.overscan = options.overscan || 5; // Number of rows to render outside viewport
        this.data = []; // Full dataset
        this.renderRow = options.renderRow; // Function to render a row
        this.updateRow = options.updateRow; // Function to update existing row
        
        this.scrollTop = 0;
        this.containerHeight = 0;
        this.visibleStart = 0;
        this.visibleEnd = 0;
        this.renderedRows = new Map(); // Map of row index to DOM element
        
        if (!this.tbody || !this.container) {
            throw new Error('tbody and container are required');
        }
        
        this.init();
    }
    
    init() {
        // Measure actual row height from first row if available
        this.measureRowHeight();
        
        // Set up scroll listener
        this.container.addEventListener('scroll', this.handleScroll.bind(this), { passive: true });
        
        // Handle resize
        const resizeObserver = new ResizeObserver(() => {
            this.updateContainerHeight();
            this.updateVisibleRange();
            this.render();
        });
        resizeObserver.observe(this.container);
        
        // Initial render
        this.updateContainerHeight();
    }
    
    measureRowHeight() {
        // Try to measure from existing rows
        const existingRow = this.tbody.querySelector('tr');
        if (existingRow) {
            const rect = existingRow.getBoundingClientRect();
            if (rect.height > 0) {
                this.rowHeight = rect.height;
            }
        }
    }
    
    updateContainerHeight() {
        const rect = this.container.getBoundingClientRect();
        this.containerHeight = rect.height;
    }
    
    handleScroll() {
        this.scrollTop = this.container.scrollTop;
        this.updateVisibleRange();
        this.render();
    }
    
    updateVisibleRange() {
        const totalRows = this.data.length;
        if (totalRows === 0) {
            this.visibleStart = 0;
            this.visibleEnd = 0;
            return;
        }
        
        // Calculate visible range
        const startIndex = Math.max(0, Math.floor(this.scrollTop / this.rowHeight) - this.overscan);
        const endIndex = Math.min(
            totalRows - 1,
            Math.ceil((this.scrollTop + this.containerHeight) / this.rowHeight) + this.overscan
        );
        
        this.visibleStart = startIndex;
        this.visibleEnd = endIndex;
    }
    
    setData(data) {
        this.data = data;
        this.updateVisibleRange();
        this.render();
    }
    
    render() {
        if (!this.tbody) return;
        
        const totalRows = this.data.length;
        const visibleCount = this.visibleEnd - this.visibleStart + 1;
        
        // Create spacer for rows before visible range
        const topSpacer = document.createElement('tr');
        topSpacer.className = 'virtual-scroll-spacer';
        topSpacer.style.height = `${this.visibleStart * this.rowHeight}px`;
        topSpacer.innerHTML = '<td colspan="100%" style="padding: 0; border: none;"></td>';
        
        // Create spacer for rows after visible range
        const bottomSpacer = document.createElement('tr');
        bottomSpacer.className = 'virtual-scroll-spacer';
        const remainingRows = totalRows - this.visibleEnd - 1;
        bottomSpacer.style.height = `${Math.max(0, remainingRows) * this.rowHeight}px`;
        bottomSpacer.innerHTML = '<td colspan="100%" style="padding: 0; border: none;"></td>';
        
        // Get existing rows
        const existingRows = Array.from(this.tbody.querySelectorAll('tr:not(.virtual-scroll-spacer)'));
        const existingRowMap = new Map();
        existingRows.forEach((row, index) => {
            const dataIndex = row.dataset.virtualIndex;
            if (dataIndex !== undefined) {
                existingRowMap.set(parseInt(dataIndex), row);
            }
        });
        
        // Clear tbody and add top spacer
        this.tbody.innerHTML = '';
        if (this.visibleStart > 0) {
            this.tbody.appendChild(topSpacer);
        }
        
        // Render visible rows
        const fragment = document.createDocumentFragment();
        for (let i = this.visibleStart; i <= this.visibleEnd && i < totalRows; i++) {
            const item = this.data[i];
            let row = existingRowMap.get(i);
            
            if (row && this.updateRow) {
                // Update existing row
                this.updateRow(row, item, i);
            } else {
                // Create new row
                row = this.renderRow(item, i);
                row.dataset.virtualIndex = i;
            }
            
            fragment.appendChild(row);
            this.renderedRows.set(i, row);
        }
        
        this.tbody.appendChild(fragment);
        
        // Add bottom spacer
        if (remainingRows > 0) {
            this.tbody.appendChild(bottomSpacer);
        }
    }
    
    /**
     * Update a specific row by index (for real-time updates)
     */
    updateRowByIndex(index, item) {
        const row = this.renderedRows.get(index);
        if (row && this.updateRow) {
            this.updateRow(row, item, index);
        } else if (index >= this.visibleStart && index <= this.visibleEnd) {
            // Row is visible but not rendered, re-render
            this.render();
        }
    }
    
    /**
     * Scroll to a specific row index
     */
    scrollToIndex(index) {
        const scrollTop = index * this.rowHeight;
        this.container.scrollTop = scrollTop;
    }
    
    /**
     * Get total height of all rows
     */
    getTotalHeight() {
        return this.data.length * this.rowHeight;
    }
    
    /**
     * Cleanup
     */
    destroy() {
        this.container.removeEventListener('scroll', this.handleScroll);
        this.renderedRows.clear();
        this.data = [];
    }
}

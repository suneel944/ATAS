/**
 * Chart Utilities Module
 * Chart.js configurations, gradient caching, and chart management
 * 
 * @module components/charts
 */

import { config } from '../config.js';

/**
 * Gradient cache to prevent memory leaks
 * Key format: `${canvasId}-${gradientType}-${width}-${height}`
 */
const gradientCache = new Map();

/**
 * Clear gradient cache for a specific canvas or all
 * @param {string} [canvasId] - Canvas ID to clear, or clear all if omitted
 */
export function clearGradientCache(canvasId = null) {
    if (canvasId) {
        const keysToDelete = [];
        gradientCache.forEach((value, key) => {
            if (key.startsWith(`${canvasId}-`)) {
                keysToDelete.push(key);
            }
        });
        keysToDelete.forEach(key => gradientCache.delete(key));
    } else {
        gradientCache.clear();
    }
}

/**
 * Create or retrieve cached gradient
 * @param {CanvasRenderingContext2D} ctx - Canvas context
 * @param {string} cacheKey - Cache key
 * @param {Function} createFn - Function to create gradient if not cached
 * @returns {CanvasGradient} Gradient object
 */
function getCachedGradient(ctx, cacheKey, createFn) {
    if (gradientCache.has(cacheKey)) {
        return gradientCache.get(cacheKey);
    }
    
    const gradient = createFn(ctx);
    gradientCache.set(cacheKey, gradient);
    return gradient;
}

/**
 * Create distribution chart gradients (for pie/doughnut charts)
 * @param {CanvasRenderingContext2D} ctx - Canvas context
 * @param {number} width - Canvas width
 * @param {number} height - Canvas height
 * @param {string} canvasId - Canvas ID for caching
 * @returns {Object} Gradient objects
 */
export function createDistributionGradients(ctx, width, height, canvasId) {
    const cacheKey = (type) => `${canvasId}-dist-${type}-${width}-${height}`;
    
    return {
        passed: getCachedGradient(ctx, cacheKey('passed'), (ctx) => {
            const grad = ctx.createLinearGradient(0, 0, width, height);
            grad.addColorStop(0, '#06b6d4');
            grad.addColorStop(1, '#22d3ee');
            return grad;
        }),
        failed: getCachedGradient(ctx, cacheKey('failed'), (ctx) => {
            const grad = ctx.createLinearGradient(0, 0, width, height);
            grad.addColorStop(0, '#f97316');
            grad.addColorStop(1, '#fb923c');
            return grad;
        }),
        skipped: getCachedGradient(ctx, cacheKey('skipped'), (ctx) => {
            const grad = ctx.createLinearGradient(0, 0, width, height);
            grad.addColorStop(0, '#fbbf24');
            grad.addColorStop(1, '#fcd34d');
            return grad;
        }),
        running: getCachedGradient(ctx, cacheKey('running'), (ctx) => {
            const grad = ctx.createLinearGradient(0, 0, width, height);
            grad.addColorStop(0, '#a855f7');
            grad.addColorStop(1, '#c084fc');
            return grad;
        })
    };
}

/**
 * Create trends chart gradients (for line/bar charts)
 * @param {CanvasRenderingContext2D} ctx - Canvas context
 * @param {Object} chartArea - Chart area bounds
 * @param {string} canvasId - Canvas ID for caching
 * @returns {Object} Gradient objects
 */
export function createTrendsGradients(ctx, chartArea, canvasId) {
    const height = chartArea.bottom - chartArea.top;
    const width = chartArea.right - chartArea.left;
    const cacheKey = (type) => `${canvasId}-trends-${type}-${width}-${height}`;
    
    return {
        passed: getCachedGradient(ctx, cacheKey('passed'), (ctx) => {
            const grad = ctx.createLinearGradient(0, chartArea.bottom, 0, chartArea.top);
            grad.addColorStop(0, 'rgba(16, 185, 129, 0.05)');
            grad.addColorStop(1, 'rgba(16, 185, 129, 0.4)');
            return grad;
        }),
        failed: getCachedGradient(ctx, cacheKey('failed'), (ctx) => {
            const grad = ctx.createLinearGradient(0, chartArea.bottom, 0, chartArea.top);
            grad.addColorStop(0, 'rgba(239, 68, 68, 0.05)');
            grad.addColorStop(1, 'rgba(239, 68, 68, 0.4)');
            return grad;
        }),
        skipped: getCachedGradient(ctx, cacheKey('skipped'), (ctx) => {
            const grad = ctx.createLinearGradient(0, chartArea.bottom, 0, chartArea.top);
            grad.addColorStop(0, 'rgba(245, 158, 11, 0.05)');
            grad.addColorStop(1, 'rgba(245, 158, 11, 0.35)');
            return grad;
        }),
        passedBorder: getCachedGradient(ctx, cacheKey('passedBorder'), (ctx) => {
            const grad = ctx.createLinearGradient(chartArea.left, 0, chartArea.right, 0);
            grad.addColorStop(0, '#10b981');
            grad.addColorStop(1, '#34d399');
            return grad;
        }),
        failedBorder: getCachedGradient(ctx, cacheKey('failedBorder'), (ctx) => {
            const grad = ctx.createLinearGradient(chartArea.left, 0, chartArea.right, 0);
            grad.addColorStop(0, '#ef4444');
            grad.addColorStop(1, '#f87171');
            return grad;
        }),
        skippedBorder: getCachedGradient(ctx, cacheKey('skippedBorder'), (ctx) => {
            const grad = ctx.createLinearGradient(chartArea.left, 0, chartArea.right, 0);
            grad.addColorStop(0, '#f59e0b');
            grad.addColorStop(1, '#fbbf24');
            return grad;
        })
    };
}

/**
 * Calculate spider label positions relative to chart radius
 * @param {number} radius - Chart radius
 * @returns {Object} Label position offsets
 */
export function calculateSpiderLabelOffsets(radius) {
    return {
        slantLength: radius * config.ui.chart.spiderLabel.slantLengthRatio,
        horizontalLength: radius * config.ui.chart.spiderLabel.horizontalLengthRatio,
        verticalOffset: radius * config.ui.chart.spiderLabel.verticalOffsetRatio
    };
}

/**
 * Destroy chart and clear associated gradients
 * @param {Chart} chart - Chart.js instance
 */
export function destroyChart(chart) {
    if (chart) {
        const canvasId = chart.canvas.id;
        chart.destroy();
        clearGradientCache(canvasId);
    }
}

/**
 * Chart.js plugin for dynamic gradient updates with caching
 * @param {string} canvasId - Canvas ID
 * @param {Function} gradientFn - Function to create gradients
 * @returns {Object} Chart.js plugin
 */
export function createGradientPlugin(canvasId, gradientFn) {
    return {
        id: `gradientCache-${canvasId}`,
        beforeLayout: (chart) => {
            if (!chart.chartArea) return;
            
            const gradients = gradientFn(chart.ctx, chart.chartArea, canvasId);
            
            // Apply gradients to datasets
            if (chart.data.datasets && chart.data.datasets.length > 0) {
                chart.data.datasets.forEach((dataset, index) => {
                    if (gradients[`dataset${index}`]) {
                        dataset.backgroundColor = gradients[`dataset${index}`].bg;
                        dataset.borderColor = gradients[`dataset${index}`].border;
                    }
                });
            }
        }
    };
}


/**
 * Application Configuration
 * Centralized configuration for API endpoints, timeouts, and other constants
 */

export const config = {
    // API Configuration
    api: {
        baseUrl: '',
        version: 'v1',
        endpoints: {
            auth: {
                login: '/api/v1/auth/login',
                profile: '/api/v1/profile/me'
            },
            testExecution: {
                dashboard: '/api/v1/test-execution/dashboard',
                dashboardRecent: '/api/v1/test-execution/dashboard/recent',
                trends: '/api/v1/test-execution/dashboard/trends'
            },
            database: {
                health: '/api/v1/database/health',
                operations: '/api/v1/database/operations',
                tables: '/api/v1/database/tables',
                tableStats: '/api/v1/database/tables/statistics'
            }
        }
    },
    
    // Polling Configuration
    polling: {
        defaultInterval: 10000, // 10 seconds
        intervals: [0, 5000, 10000, 30000, 60000], // Manual, 5s, 10s, 30s, 1m
        exponentialBackoff: {
            initialDelay: 1000,
            maxDelay: 30000,
            multiplier: 2
        }
    },
    
    // UI Configuration
    ui: {
        defaultDays: 30,
        chart: {
            // Relative to chart radius for responsive labels
            spiderLabel: {
                slantLengthRatio: 0.3,  // 30% of radius
                horizontalLengthRatio: 0.25,  // 25% of radius
                verticalOffsetRatio: 0.15  // 15% of radius
            }
        }
    },
    
    // Security Configuration
    security: {
        allowedRedirectPaths: [
            '/monitoring/dashboard',
            '/monitoring/database',
            '/login'
        ],
        tokenStorageKey: 'accessToken',
        refreshTokenStorageKey: 'refreshToken'
    }
};


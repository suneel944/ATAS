package com.atas.framework.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Simple controller to serve the monitoring dashboard page.
 * This provides a basic web UI for the monitoring functionality.
 */
@Controller
public class MonitoringDashboardController {

  /**
   * Serves the main monitoring dashboard page.
   * @return the dashboard view name
   */
  @GetMapping("/monitoring/dashboard")
  public String dashboard() {
    return "monitoring-dashboard";
  }

  /**
   * Serves the database management page.
   * @return the database management view name
   */
  @GetMapping("/monitoring/database")
  public String databaseManagement() {
    return "database-management";
  }
}

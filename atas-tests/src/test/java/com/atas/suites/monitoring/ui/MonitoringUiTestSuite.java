package com.atas.suites.monitoring.ui;

import com.atas.features.monitoring.ui.MonitoringDashboardUiTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/** Test suite for Monitoring UI tests Groups all UI tests related to monitoring functionality */
@Suite
@SelectClasses({MonitoringDashboardUiTest.class})
public class MonitoringUiTestSuite {}

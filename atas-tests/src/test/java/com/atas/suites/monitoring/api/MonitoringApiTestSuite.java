package com.atas.suites.monitoring.api;

import com.atas.features.monitoring.api.TestExecutionStatusApiTest;
import com.atas.features.monitoring.api.TestMonitoringApiTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/** Test suite for Monitoring API tests Groups all API tests related to monitoring functionality */
@Suite
@SelectClasses({TestExecutionStatusApiTest.class, TestMonitoringApiTest.class})
public class MonitoringApiTestSuite {}

package com.atas.suites;

import com.atas.suites.authentication.api.AuthenticationApiTestSuite;
import com.atas.suites.authentication.ui.AuthenticationUiTestSuite;
import com.atas.suites.monitoring.api.MonitoringApiTestSuite;
import com.atas.suites.monitoring.ui.MonitoringUiTestSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Master test suite that includes all feature test suites Run this to execute all tests across all
 * features
 */
@Suite
@SelectClasses({
  AuthenticationUiTestSuite.class,
  AuthenticationApiTestSuite.class,
  MonitoringUiTestSuite.class,
  MonitoringApiTestSuite.class
})
public class AllTestSuites {}

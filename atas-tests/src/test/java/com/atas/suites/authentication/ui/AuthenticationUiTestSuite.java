package com.atas.suites.authentication.ui;

import com.atas.features.authentication.ui.LoginUiTest;
import com.atas.features.authentication.ui.LoginValidationUiTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for Authentication UI tests Groups all UI tests related to authentication
 * functionality
 */
@Suite
@SelectClasses({LoginUiTest.class, LoginValidationUiTest.class})
public class AuthenticationUiTestSuite {}

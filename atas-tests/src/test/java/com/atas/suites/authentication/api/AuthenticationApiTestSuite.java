package com.atas.suites.authentication.api;

import com.atas.features.authentication.api.LoginApiTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for Authentication API tests Groups all API tests related to authentication
 * functionality
 */
@Suite
@SelectClasses({LoginApiTest.class})
public class AuthenticationApiTestSuite {}

package com.atas.framework.model;

/**
 * Types of attachments that can be stored for test results.
 * Screenshots and videos are the primary types supported out of
 * the box, but other types (e.g. logs, data files) can be added.
 */
public enum AttachmentType {
    SCREENSHOT,
    VIDEO,
    LOG,
    OTHER;
}
package com.android.compatibility.common.util;


import org.junit.Assert;

import java.io.File;

/**
 * Static methods used to validate preconditions in the media CTS suite to simplify failure
 * diagnosis.
 */
public final class Preconditions {
    private static final String TAG = "Preconditions";

    /**
     * While accessing resource file, if it is not present, media codec api sometimes sends
     * obfuscated message indicating the same. Have the test run this check before accessing
     * resource.
     */
    public static void assertTestFileExists(String pathName) {
        File testFile = new File(pathName);
        Assert.assertTrue("Test Setup Error, missing file: " + pathName, testFile.exists());
    }

    private Preconditions() {}
}
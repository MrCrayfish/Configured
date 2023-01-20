package com.mrcrayfish.configured.util.forge;

import com.mrcrayfish.configured.util.CustomValueUtil;

public class CustomValueUtilCvObjectImpl {
    public static int getSizeInternal(CustomValueUtil.CvObject cvObject) {
        return 0;
    }

    public static boolean containsKeyInternal(String key, CustomValueUtil.CvObject cvObject) {
        return false;
    }

    public static CustomValueUtil.CustomValue getInternal(String key, CustomValueUtil.CvObject object) {
        return new CustomValueUtil.CustomValue(object);
    }
}

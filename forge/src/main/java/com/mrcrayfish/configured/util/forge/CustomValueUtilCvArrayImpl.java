package com.mrcrayfish.configured.util.forge;

import com.mrcrayfish.configured.util.CustomValueUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CustomValueUtilCvArrayImpl {
    public static int getSizeInternal(CustomValueUtil.CvArray array) {
        return ((List<?>)CustomValueUtil.getInternalValue(array)).size();
    }

    public static Iterator getIteratorInternal(CustomValueUtil.CvArray array) {
        return ((List<?>)CustomValueUtil.getInternalValue(array)).iterator();
    }
}

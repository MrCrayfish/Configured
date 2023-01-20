package com.mrcrayfish.configured.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Mod;
import net.fabricmc.loader.api.metadata.CustomValue;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Author: Jab125
 */
public class CustomValueUtil {
    @ExpectPlatform
    public static CustomValue getCustomValue(Mod mod, String string) {
        throw new AssertionError();
    }

    public static class CustomValue {
        private final Object object;
        public CustomValue(Object object) {
            this.object = object;
        }

        public String getAsString() {
            return getAsStringInternal(this);
        }

        @ExpectPlatform
        private static String getAsStringInternal(CustomValue customValue) {
            throw new AssertionError();
        }

        public CvArray getAsArray() {
            return getAsArrayInternal(this);
        }

        @ExpectPlatform
        private static CvArray getAsArrayInternal(CustomValue customValue) {
            throw new AssertionError();
        }

        public CvType getType() {
            return getTypeInternal(this);
        }

        @ExpectPlatform
        private static CvType getTypeInternal(CustomValue customValue) {
            throw new AssertionError();
        }

        public CvObject getAsObject() {
            return getAsObjectInternal(this);
        }

        @ExpectPlatform
        private static CvObject getAsObjectInternal(CustomValue customValue) {
            throw new AssertionError();
        }
    }

    public static class CvObject extends CustomValue {

        public CvObject(Object object) {
            super(object);
        }
        public int size() {
            return getSizeInternal(this);
        }

        @ExpectPlatform
        private static int getSizeInternal(CvObject cvObject) {
            throw new AssertionError();
        }

        public boolean containsKey(String key) {
            return containsKeyInternal(key, this);
        }

        @ExpectPlatform
        private static boolean containsKeyInternal(String key, CvObject cvObject) {
            throw new AssertionError();
        }

        public CustomValue get(String key) {
            return getInternal(key, this);
        }

        @ExpectPlatform
        private static CustomValue getInternal(String key, CvObject object) {
            throw new AssertionError();
        }
    }

    public enum CvType {
        OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInternalValue(CustomValue customValue) {
        return (T) customValue.object;
    }

    public static class CvArray extends CustomValue implements Iterable<CustomValue> {
        public CvArray(Object object) {
            super(object);
        }

        public int size() {
            return getSizeInternal(this);
        }

        @ExpectPlatform
        private static int getSizeInternal(CvArray array) {
            throw new AssertionError();
        }

        @ExpectPlatform
        private static Iterator getIteratorInternal(CvArray array) {
            throw new AssertionError();
        }

        @NotNull
        @Override
        public Iterator<CustomValue> iterator() {
            return new Iterator<>() {
                private final Iterator internalIterator = getIteratorInternal(CvArray.this);


                @Override
                public boolean hasNext() {
                    return internalIterator.hasNext();
                }

                @Override
                public CustomValue next() {
                    return new CustomValue(internalIterator.next());
                }
            };
        }
    }
}

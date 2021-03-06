package org.jaudiotagger.tag.images;

/*
 * Provides a class for all Image handling, this is required because the image classes
 * provided by standard java are different to those provided by Android
 */
@SuppressWarnings("unused")
public class ImageHandlingFactory {

    private static AndroidImageHandler androidImageHandler;

    public static ImageHandler getInstance() {

        if (androidImageHandler == null) {
            androidImageHandler = AndroidImageHandler.getInstanceOf();
        }
        return androidImageHandler;

    }
}

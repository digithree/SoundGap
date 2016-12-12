package github.digithree.soundgap.utils;

/**
 * Created by simonkenny on 12/12/2016.
 */

public class ManagementUtil {

    public static int generateRandomCode() {
        return (int) (Math.random() * ((Integer.MAX_VALUE >> 16)- 1));
    }
}

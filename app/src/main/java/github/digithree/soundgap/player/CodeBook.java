/* THIS FILE NOT UNDER COPYRIGHT
 *
 * This file does not appear to be under license, as the original file posted no copyright and
 * the project in general was not explicitly under license in total.
 */

//https://github.com/dingjikerbo/SinVoice/blob/master/app/src/main/java/com/libra/sinvoice/CodeBook.java
//package com.libra.sinvoice;

package github.digithree.soundgap.player;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liwentian on 2016/11/25.
 */

public class CodeBook {

    private final static String CODEBOOK = "0123456789ABCDEF!#";

    private List<Integer> mCodeFreqs;

    private static CodeBook sInstance;

    public static CodeBook getInstance() {
        if (sInstance == null) {
            synchronized (CodeBook.class) {
                if (sInstance == null) {
                    sInstance = new CodeBook();
                }
            }
        }
        return sInstance;
    }

    private CodeBook() {
        mCodeFreqs = new ArrayList<Integer>();
        int freq = 18000;
        for (int i = 0; i < CODEBOOK.length(); i++) {
            mCodeFreqs.add(freq);
            freq += 200;
        }
    }

    public int getFreqs(int index) {
        if (index >= 0 && index < CODEBOOK.length()) {
            return mCodeFreqs.get(index);
        }
        return 1000;
    }
}

/* THIS FILE NOT UNDER COPYRIGHT
 *
 * This file does not appear to be under license, as the original file posted no copyright and
 * the project in general was not explicitly under license in total.
 *
 * Original author note: Created by liwentian on 2016/11/25.
 *
 * -----------------------------------------------------------------------------------------------
 *
 * Modified on 11th Dec 2016 to implement intended purpose of this class
 */

//https://github.com/dingjikerbo/SinVoice/blob/master/app/src/main/java/com/libra/sinvoice/CodeBook.java
//package com.libra.sinvoice;

package github.digithree.soundgap.player;

public class CodeBook {

    private static CodeBook INSTANCE;

    public static CodeBook getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CodeBook();
        }
        return INSTANCE;
    }

    protected CodeBook() {
        //thwart instantiation
    }


    private final static int START_TOKEN = 0;
    private final static int STOP_TOKEN = 11;
    private final static String CODEBOOK = "0123456789";

    private final static int[] CODE_FREQUENCY = {
            1046, 1109, 1175, 1245, 1319, 1397, 1480, 1568, 1661, 1760, 1865, 1976
    };
    //      START, 0,    1,    2,   3,    4,    5,    6,    7,    8,    9,    END
    //      C,     C#,   D,    D#,  E,    F,    F#,   G,    G#,   A,    A#,   B

    private final static String[] CODE_NOTES = {
            "C7", "C#7", "D7", "D#7", "E7", "F7", "F#7", "G7", "G#7", "A7", "A#7", "B7"
    };

    public String getCodeBook() {
        return CODEBOOK;
    }

    public int[] getCodeFrequencyTable() {
        return CODE_FREQUENCY;
    }

    public String[] getCodeNoteNameTable() {
        return CODE_NOTES;
    }

    public int getStartTokenIdx() {
        return START_TOKEN;
    }

    public int getEndTokenIdx() {
        return STOP_TOKEN;
    }
}

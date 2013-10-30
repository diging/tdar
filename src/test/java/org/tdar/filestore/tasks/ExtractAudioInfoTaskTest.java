package org.tdar.filestore.tasks;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.tdar.TestConstants;
import org.tdar.core.bean.resource.Audio;

/**
 * A bare bones test required for the processing of the extract audio info task.
 * A run through some of the test files at http://www-mmsp.ece.mcgill.ca/documents/AudioFormats/AIFF/Samples.html shows that there are a number of AIF files
 * not supported by the java sound api :(
 * 
 * @author Martin Paulo
 */
public class ExtractAudioInfoTaskTest {

    private static final String JAVA_SOUND_API_NEEDS_UPGRADING_TO_DETERMINE_CONTENT = "Java Sound API needs upgrading to determine content.";
    
    private static final String AIFF_EXPECTED =
            "AIFF (.aif) file, byte length: 829726, data format: PCM_SIGNED 44100.0 Hz, 16 bit, stereo, 4 bytes/frame, big-endian, frame length: 207360";
    private static final String WAV_EXPECTED =
            "WAVE (.wav) file, byte length: 829786, data format: PCM_SIGNED 44100.0 Hz, 16 bit, stereo, 4 bytes/frame, little-endian, frame length: 207360";
    private Audio testSubject = new Audio();

    @Test
    public void readAiffFileMetadata() {
        testFileCodec("testing.aiff", AIFF_EXPECTED);
    }

    @Test
    public void readWavFileMetadata() {
        testFileCodec("testing.wav", WAV_EXPECTED);
    }
    
    @Test
    public void readFlacFileMetadata() {
        testFileCodec("testing.flac", JAVA_SOUND_API_NEEDS_UPGRADING_TO_DETERMINE_CONTENT);
    }

    @Test
    public void readMp3FileMetadata() {
        testFileCodec("testing.mp3", JAVA_SOUND_API_NEEDS_UPGRADING_TO_DETERMINE_CONTENT);
    }


    private void testFileCodec(String fileName, String expected) {
        ExtractAudioInfoTask task = new ExtractAudioInfoTask();
        File audioFile = new File(TestConstants.TEST_AUDIO_DIR + fileName);
        task.writeFileMetadataToAudioFile(testSubject, audioFile);
        final String audioCodecFound = testSubject.getAudioCodec();
        assertTrue("Expected: '" + expected + "' but found: '" + audioCodecFound + "'", expected.equals(audioCodecFound));
    }

}

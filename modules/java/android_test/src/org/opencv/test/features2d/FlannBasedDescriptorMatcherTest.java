package org.opencv.test.features2d;

import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.test.OpenCVTestCase;
import org.opencv.test.OpenCVTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlannBasedDescriptorMatcherTest extends OpenCVTestCase {

    DescriptorMatcher matcher;
    int matSize;
    DMatch[] truth;

    protected void setUp() throws Exception {
        matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        matSize = 100;

        truth = new DMatch[] { 
                new DMatch(0, 0, 0, 0.643284f),
                new DMatch(1, 1, 0, 0.92945856f),
                new DMatch(2, 1, 0, 0.2841479f),
                new DMatch(3, 1, 0, 0.9194034f),
                new DMatch(4, 1, 0, 0.3006621f) };

        super.setUp();
    }

    private Mat getTrainImg() {
        Mat cross = new Mat(matSize, matSize, CvType.CV_8U, new Scalar(255));
        Core.line(cross, new Point(20, matSize / 2), new Point(matSize - 21, matSize / 2), new Scalar(100), 2);
        Core.line(cross, new Point(matSize / 2, 20), new Point(matSize / 2, matSize - 21), new Scalar(100), 2);

        return cross;
    }

    private Mat getQueryImg() {
        Mat cross = new Mat(matSize, matSize, CvType.CV_8U, new Scalar(255));
        Core.line(cross, new Point(30, matSize / 2), new Point(matSize - 31, matSize / 2), new Scalar(100), 3);
        Core.line(cross, new Point(matSize / 2, 30), new Point(matSize / 2, matSize - 31), new Scalar(100), 3);

        return cross;
    }

    private Mat getQueryDescriptors() {
        Mat img = getQueryImg();
        List<KeyPoint> keypoints = new ArrayList<KeyPoint>();
        Mat descriptors = new Mat();

        FeatureDetector detector = FeatureDetector.create(FeatureDetector.SURF);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.SURF);

        String filename = OpenCVTestRunner.getTempFileName("yml");
        writeFile(filename, "%YAML:1.0\nhessianThreshold: 8000.\noctaves: 3\noctaveLayers: 4\nupright: 0\n");
        detector.read(filename);

        detector.detect(img, keypoints);
        extractor.compute(img, keypoints, descriptors);

        return descriptors;
    }

    private Mat getTrainDescriptors() {
        Mat img = getTrainImg();
        List<KeyPoint> keypoints = Arrays.asList(new KeyPoint(50, 50, 16, 0, 20000, 1, -1), new KeyPoint(42, 42, 16, 160, 10000, 1, -1));
        Mat descriptors = new Mat();

        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.SURF);

        extractor.compute(img, keypoints, descriptors);

        return descriptors;
    }

    private Mat getMaskImg() {
        return new Mat(5, 2, CvType.CV_8U, new Scalar(0)) {
            {
                put(0,0, 1, 1, 1, 1);
            }
        };
    }

    public void testAdd() {
        matcher.add(Arrays.asList(new Mat()));
        assertFalse(matcher.empty());
    }

    public void testClear() {
        matcher.add(Arrays.asList(new Mat()));

        matcher.clear();

        assertTrue(matcher.empty());
    }

    public void testCloneBoolean() {
        matcher.add(Arrays.asList(new Mat()));

        DescriptorMatcher cloned = matcher.clone(true);

        assertNotNull(cloned);
        assertTrue(cloned.empty());
    }

    public void testClone() {
        Mat train = new Mat(1, 1, CvType.CV_8U, new Scalar(123));
        matcher.add(Arrays.asList(train));

        try {
            matcher.clone();
            fail("Expected CvException (CV_StsNotImplemented)");
        } catch (CvException cverr) {
            // expected
        }
    }

    public void testCreate() {
        assertNotNull(matcher);
    }

    public void testEmpty() {
        assertTrue(matcher.empty());
    }

    public void testGetTrainDescriptors() {
        Mat train = new Mat(1, 1, CvType.CV_8U, new Scalar(123));
        Mat truth = train.clone();
        matcher.add(Arrays.asList(train));

        List<Mat> descriptors = matcher.getTrainDescriptors();

        assertEquals(1, descriptors.size());
        assertMatEqual(truth, descriptors.get(0));
    }

    public void testIsMaskSupported() {
        assertFalse(matcher.isMaskSupported());
    }

    public void testMatchMatMatListOfDMatchMat() {
        Mat train = getTrainDescriptors();
        Mat query = getQueryDescriptors();
        Mat mask = getMaskImg();
        List<DMatch> matches = new ArrayList<DMatch>();
        
        matcher.match(query, train, matches, mask);

        assertListDMatchEquals(Arrays.asList(truth), matches, EPS);
    }

    public void testMatchMatMatListOfDMatch() {
        Mat train = getTrainDescriptors();
        Mat query = getQueryDescriptors();
        List<DMatch> matches = new ArrayList<DMatch>();
        
        matcher.match(query, train, matches);
        
        assertListDMatchEquals(Arrays.asList(truth), matches, EPS);

//        OpenCVTestRunner.Log("matches found: " + matches.size());
//        for (DMatch m : matches)
//            OpenCVTestRunner.Log(m.toString());
    }

    public void testMatchMatListOfDMatchListOfMat() {
        Mat train = getTrainDescriptors();
        Mat query = getQueryDescriptors();
        Mat mask = getMaskImg();
        List<DMatch> matches = new ArrayList<DMatch>();
        matcher.add(Arrays.asList(train));
        matcher.train();

        matcher.match(query, matches, Arrays.asList(mask));

        assertListDMatchEquals(Arrays.asList(truth), matches, EPS);
    }

    public void testMatchMatListOfDMatch() {
        Mat train = getTrainDescriptors();
        Mat query = getQueryDescriptors();
        List<DMatch> matches = new ArrayList<DMatch>();
        matcher.add(Arrays.asList(train));
        matcher.train();
        
        matcher.match(query, matches);
        
        assertListDMatchEquals(Arrays.asList(truth), matches, EPS);
    }

    public void testRead() {
        fail("https://code.ros.org/trac/opencv/ticket/1253");
        String filename = OpenCVTestRunner.getTempFileName("yml");
        writeFile(filename, "%YAML:1.0\n");

        matcher.read(filename);
        assertTrue(true);
    }

    public void testTrain() {
        Mat train = getTrainDescriptors();
        matcher.add(Arrays.asList(train));
        matcher.train();
    }
    
    public void testTrainNoData() {
        try {
            matcher.train();
            fail("Expected CvException - FlannBasedMatcher::train should fail on empty train set");
        } catch (CvException cverr) {
            // expected
        }
    }

    public void testWrite() {
        fail("https://code.ros.org/trac/opencv/ticket/1253");
        String filename = OpenCVTestRunner.getTempFileName("yml");

        matcher.write(filename);

        String truth = "%YAML:1.0\n!!!!!";
        assertEquals(truth, readFile(filename));
    }

}
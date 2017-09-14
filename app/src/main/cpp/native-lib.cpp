#include <jni.h>
#include <string>
#include "Source.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_richard_nativeandroidopencv_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
void JNICALL Java_com_example_richard_nativeandroidopencv_MainActivity_detectIris(JNIEnv *env, jobject instance, jlong addrInput, jlong addrOutput, jlong addrOriginal)
{
    Mat& currentImage = *(Mat*)addrInput;
    Mat& output = *(Mat*)addrOutput;
    Mat& original = *(Mat*)addrOriginal;

    Mat unprocessed = currentImage.clone();
    cvtColor(currentImage, currentImage, COLOR_BGR2GRAY);
    output = findAndExtractIris(currentImage, unprocessed, original);
    unprocessed.release();
}

extern "C"
JNIEXPORT jintArray JNICALL Java_com_example_richard_nativeandroidopencv_MainActivity_returnHist(JNIEnv *env, jobject instance, jlong addrInput)
{
    jintArray javaHistogram;
    javaHistogram = (env)->NewIntArray(59);
    if (javaHistogram == NULL)
    {
        return NULL; /* out of memory error thrown */
    }

    Mat& currentImage = *(Mat*)addrInput;

    //Mat& output = *(Mat*)addrOutput;

    Mat input = currentImage;
    Mat unprocessed = currentImage;

    //Find and extract iris
    currentImage = findAndExtractIris(currentImage, unprocessed, input);

    // fill a temp structure to use to populate the java int array
    vector<int> histogram = LBP(currentImage);
    jint fill[59];
    for (int i = 0; i < 59; i++)
    {
        fill[i] = histogram[i]; // put whatever logic you want to populate the values here.
    }
    // move from the temp structure to the java structure
    (env)->SetIntArrayRegion(javaHistogram, 0, 59, fill);
    return javaHistogram;
}

Mat findAndExtractIris(Mat input, Mat unprocessed, Mat original)
{
    Mat processed;
    threshold(input, processed, 50, 255, THRESH_BINARY_INV);
    //processed = fillHoles(input);

    //GaussianBlur(processed, processed, Size(9, 9), 3, 3);
    return processed;

//    vector<Vec3f> circles;
//    HoughCircles(processed, circles, CV_HOUGH_GRADIENT, 2, original.rows / 8, 255, 30, 0, 0);
//    for (size_t i = 0; i < 1; i++)//circles.size()
//    {
//        Point center(cvRound(circles[i][0]), cvRound(circles[i][1]));
//        pupilx = cvRound(circles[i][0]), pupily = cvRound(circles[i][1]);
//        pupilRadius = cvRound(circles[i][2]);
//        irisRadius = pupilRadius*3;
//        circle(unprocessed, center, pupilRadius, Scalar(0, 0, 0), CV_FILLED);
//        circle(unprocessed, center, irisRadius, Scalar(0, 0, 255), 2, 8, 0);
//    }
//
//    Mat iris = normalize(unprocessed);
//    return unprocessed;
    //return iris;
}

Mat fillHoles(Mat input)
{
    Mat thresholded;
    threshold(input, thresholded, 70, 255, THRESH_BINARY_INV);

    Mat floodfilled = thresholded.clone();
    floodFill(floodfilled, Point(0, 0), Scalar(255));

    bitwise_not(floodfilled, floodfilled);

    return (thresholded | floodfilled);
}

Mat normalize(Mat input) // , int pupilx, int pupily, int pupilRadius, int irisRadius
{
    int yNew = 360;
    int xNew = 100;

    Mat normalized = Mat(xNew, yNew, CV_8U, Scalar(255));
    for (int i = 0; i < yNew; i++)
    {
        double alpha = 2 * PI * i / yNew;
        for (int j = 0; j < xNew; j++)
        {
            double r = 1.0*j / xNew;
            int x = (int)((1 - r)*(pupilx + pupilRadius*cos(alpha)) + r*(pupilx + irisRadius*cos(alpha)));
            int y = (int)((1 - r)*(pupily + pupilRadius*sin(alpha)) + r*(pupily + irisRadius*sin(alpha)));
            if (x < 0)
                x = 0;
            if (y < 0)
                y = 0;
            if (x > input.size().width-1)
                x = input.size().width-1;
            if (y > input.size().height-1)
                y = input.size().height-1;
            normalized.at<uchar>(j, i) = input.at<uchar>(y, x);
        }
    }
    Rect reducedSelection(0, 5, 360, 75);
    normalized = normalized(reducedSelection);
    return normalized;
}

//Uniform LBP
vector<int> LBP(Mat input)
{
    vector<int> outputHist(59);
    fill(outputHist.begin(), outputHist.end(), 0);

    for (size_t i = 1; i < input.rows - 1; i++)
    {
        for (size_t j = 1; j < input.cols - 1; j++)
        {
            //Currently centered pixel
            Scalar otherIntensity = input.at<uchar>(i, j);
            int vectorValue = 0;
            vector<int> binaryCode;
            int pixelIntensity = otherIntensity.val[0];

            //Top left
            otherIntensity = input.at<uchar>(i - 1, j - 1);
            if (otherIntensity.val[0] < pixelIntensity)
            {
                vectorValue += 128;
                binaryCode.push_back(1);
            }
            else
                binaryCode.push_back(0);

            //Top middle
            otherIntensity = input.at<uchar>(i, j - 1);
            if (otherIntensity.val[0] < pixelIntensity)
            {
                vectorValue += 64;
                binaryCode.push_back(1);
            }
            else
                binaryCode.push_back(0);

            //Top right
            otherIntensity = input.at<uchar>(i + 1, j - 1);
            if (otherIntensity.val[0] < pixelIntensity)
            {
                vectorValue += 32;
                binaryCode.push_back(1);
            }
            else
                binaryCode.push_back(0);

            //Right
            otherIntensity = input.at<uchar>(i + 1, j);
            if (otherIntensity.val[0] < pixelIntensity)
            {
                vectorValue += 16;
                binaryCode.push_back(1);
            }
            else
                binaryCode.push_back(0);

            //Bottom right
            otherIntensity = input.at<uchar>(i + 1, j + 1);
            if (otherIntensity.val[0] < pixelIntensity)
            {
                vectorValue += 8;
                binaryCode.push_back(1);
            }
            else
                binaryCode.push_back(0);

            //Botttom middle
            otherIntensity = input.at<uchar>(i, j + 1);
            if (otherIntensity.val[0] < pixelIntensity)
            {
                vectorValue += 4;
                binaryCode.push_back(1);
            }
            else
                binaryCode.push_back(0);

            //Bottom left
            otherIntensity = input.at<uchar>(i - 1, j + 1);
            if (otherIntensity.val[0] < pixelIntensity)
            {
                vectorValue += 2;
                binaryCode.push_back(1);
            }
            else
                binaryCode.push_back(0);

            //Left
            otherIntensity = input.at<uchar>(i - 1, j);
            if (otherIntensity.val[0] < pixelIntensity)
            {
                vectorValue += 1;
                binaryCode.push_back(1);
            }
            else
                binaryCode.push_back(0);

            if (checkUniform(binaryCode))
            {
                for (int x = 0; x < 59; x++)
                    if (histogramValues[x] == vectorValue)
                        outputHist[x]++;
            }
            else
                outputHist[58]++;
        }
    }
    return outputHist;
}

bool checkUniform(vector<int> binaryCode)
{
    int transitionCount = 0;
    for (int i = 1; i < 8; i++)
    {
        if (binaryCode[i] ^ binaryCode[i - 1] == 1)
            transitionCount++;

        if (transitionCount > 2)
            return false;
    }
    return true;
}

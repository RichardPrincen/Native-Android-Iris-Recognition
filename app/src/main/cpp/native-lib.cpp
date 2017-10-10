#include <jni.h>
#include <string>
#include "Source.h"

extern "C"
void JNICALL Java_com_example_richard_nativeandroidopencv_CameraAuthenticateActivity_detectIris(JNIEnv *env, jobject instance, jlong addrInput, jlong addrOutput, jlong addrOutputNormalized, jlong addrOriginal)
{
    Mat& currentImage = *(Mat*)addrInput;
    Mat& output = *(Mat*)addrOutput;
    Mat& outputNormalized = *(Mat*)addrOutputNormalized;
    Mat& original = *(Mat*)addrOriginal;

    Mat unprocessed = currentImage.clone();
    cvtColor(currentImage, currentImage, COLOR_BGR2GRAY);
    output = findAndExtractIris(currentImage, unprocessed, original);
    outputNormalized = normalize(output);
    unprocessed.release();
}

extern "C"
void JNICALL Java_com_example_richard_nativeandroidopencv_CameraRegisterActivity_detectIris(JNIEnv *env, jobject instance, jlong addrInput, jlong addrOutput, jlong addrOutputNormalized, jlong addrOriginal)
{
    Mat& currentImage = *(Mat*)addrInput;
    Mat& output = *(Mat*)addrOutput;
    Mat& outputNormalized = *(Mat*)addrOutputNormalized;
    Mat& original = *(Mat*)addrOriginal;

    Mat unprocessed = currentImage.clone();
    cvtColor(currentImage, currentImage, COLOR_BGR2GRAY);
    output = findAndExtractIris(currentImage, unprocessed, original);
    outputNormalized = normalize(output);
    unprocessed.release();
}

Mat findAndExtractIris(Mat input, Mat unprocessed, Mat original)
{
    Mat processed;
    threshold(input, processed, 70, 255, THRESH_BINARY_INV);
    //processed = fillHoles(input);

    cvtColor(unprocessed, unprocessed, CV_BGR2GRAY);

    GaussianBlur(processed, processed, Size(9, 9), 3, 3);

    vector<Vec3f> circles;
    HoughCircles(processed, circles, CV_HOUGH_GRADIENT, 2, original.rows / 8, 255, 30, 0, 0);
    for (size_t i = 0; i < 1; i++)//circles.size()
    {
        Point center(cvRound(circles[i][0]), cvRound(circles[i][1]));
        pupilx = cvRound(circles[i][0]), pupily = cvRound(circles[i][1]);
        pupilRadius = cvRound(circles[i][2]);
        irisRadius = findIrisRadius(unprocessed, center, pupilRadius); //pupilRadius*3;//
        circle(unprocessed, center, pupilRadius, Scalar(0), CV_FILLED);
        circle(unprocessed, center, irisRadius, Scalar(0), 2, 8, 0);
    }

    return unprocessed;
}

int findIrisRadius(Mat input , Point startPoint, int radius)
{
    int rightIntensity;
    int leftIntensity;
    int position = startPoint.y - (radius+20);
    int newRadius = radius+20;
    while (true)
    {
        rightIntensity = leftIntensity;
        position -= 10;
        newRadius += 10;
        leftIntensity = input.at<uchar>(startPoint.x, position);
        if (leftIntensity - rightIntensity > 30)
            return newRadius-5;
    }
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

Mat normalize(Mat input)
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
    Rect reducedSelection(0, 5, 360, 65);
    normalized = normalized(reducedSelection);
    return normalized;
}

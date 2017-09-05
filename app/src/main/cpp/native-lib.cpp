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
void JNICALL Java_com_example_richard_nativeandroidopencv_MainActivity_detectIris(JNIEnv *env, jlong addrInput, jlong addrOutput)
{

    Mat& currentImage = *(Mat*)addrInput;

    Mat& output = *(Mat*)addrOutput;

    Mat input = currentImage;
    Mat unprocessed = currentImage;

    output = currentImage;

    //Find and extract iris
    //currentImage = findAndExtractIris(currentImage, unprocessed, input);

    //Normalize
    //currentImage = normalize(currentImage, pupilx, pupily, pupilRadius, irisRadius);

    //destroyAllWindows();

    //return currentImage;
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

    //Normalize
    currentImage = normalize(currentImage, pupilx, pupily, pupilRadius, irisRadius);

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

Mat blurImage(Mat input)
{
    Mat blurredFrame;
    GaussianBlur(input, blurredFrame, Size(9, 9), 5, 5);
    return blurredFrame;
}

Mat cannyTransform(Mat input)
{
    Mat processed;
    Canny(input, processed, 100, 120, 3, false);
    return processed;
}

Mat findAndExtractIris(Mat &input, Mat &unprocessed, Mat &original)
{
    Mat processed;
    /*processed = EdgeContour(input);*/

    GaussianBlur(input, processed, Size(9, 9), 3, 3);
    threshold(processed, processed, 70, 255, CV_THRESH_BINARY);

    processed = cannyTransform(processed);


    vector<Vec3f> circles;
    HoughCircles(processed, circles, CV_HOUGH_GRADIENT, 2, original.rows / 8, 255, 30, 0, 0);
    if (circles.empty())
        return original;
    for (size_t i = 0; i < 1; i++)//circles.size()
    {
        Point center(cvRound(circles[i][0]), cvRound(circles[i][1]));

        pupilRadius = cvRound(circles[i][2]);
        irisRadius = pupilRadius*4;
        circle(unprocessed, center, pupilRadius, Scalar(0, 0, 0), CV_FILLED);
        circle(unprocessed, center, irisRadius, Scalar(0, 0, 255), 2, 8, 0);
    }

    Vec3f circ = circles[0];
    Mat1b mask(unprocessed.size(), uchar(0));
    circle(mask, Point(circ[0], circ[1]), irisRadius, Scalar(255), CV_FILLED);
    Rect bbox(circ[0] - irisRadius, circ[1] - irisRadius, 2 * irisRadius, 2 * irisRadius);
    Mat iris(200, 200, CV_8UC3, Scalar(255, 255, 255));

    unprocessed.copyTo(iris, mask);
    iris = iris(bbox);
    pupilx = iris.size().width/2, pupily = iris.size().height/2;
    return iris;
    //return unprocessed;
}

Mat findPupil(Mat input)
{
    Mat cannyImage;
    GaussianBlur(input, cannyImage, Size(9, 9), 3, 3);

    Mat processed;
    double highVal = threshold(input, processed, 70, 255, CV_THRESH_BINARY);
    double lowVal = highVal * 0.5;

    cannyImage = cannyTransform(processed);

    //cannyImage = CannyTransform(cannyImage);

    vector<Vec3f> circles;
    HoughCircles(cannyImage, circles, CV_HOUGH_GRADIENT, 20, input.rows, 255, 30, 0, 0);
    for (size_t i = 0; i < circles.size(); i++)
    {
        Point center(cvRound(circles[i][0]), cvRound(circles[i][1]));
        pupilx = cvRound(circles[i][0]), pupily = cvRound(circles[i][1]);
        pupilRadius = cvRound(circles[i][2] * 1.1);

        circle(input, center, pupilRadius, Scalar(0, 0, 0), CV_FILLED);
    }

    return input;
}

Mat normalize(Mat input, int pupilx, int pupily, int pupilRadius, int irisRadius)
{
    int yNew = 512;
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

double hammingDistance(vector<int> savedCode, vector<int> inputCode)
{
    int currentDistance = 0;
    int averageDistance = 0;
    for (int i = 0; i < inputCode.size(); i++)
    {
        currentDistance = 0;
        unsigned  val = savedCode[i] ^ inputCode[i];
        while (val != 0)
        {
            currentDistance++;
            val &= val - 1;
        }
        averageDistance += currentDistance/8;
    }
    return 1.0*averageDistance / inputCode.size();
}

double chiSquared(vector<int> hist1, vector<int> hist2)
{
    vector<double> normalizedHist1(59);
    vector<double> normalizedHist2(59);

    for (int i = 0; i < 58; i++)
    {
        normalizedHist1[i] = (double)hist1[i]/hist1[58];
        normalizedHist2[i] = (double)hist2[i]/hist2[58];
    }

    normalizedHist1[58] = 1.0;
    normalizedHist2[58] = 1.0;

    double chiSquaredValue = 0.0;
    for (int i = 1; i < 59; i++)
    {
        if (hist1[i] + hist2[i] != 0)
        {
            chiSquaredValue += pow(normalizedHist1[i] - normalizedHist2[i], 2) / (normalizedHist1[i] + normalizedHist2[i]);
        }
    }
    return chiSquaredValue * 10;
}
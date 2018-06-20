from os import listdir
import re
import datetime
import time
import sys
import scipy
from scipy.stats import t
import numpy as np

# Dictionary containing all the results
results = {
    "lwCharge": 0.0,
    "hwCharge" : 0.0,
    "lwChargeArray" : [],
    "hwChargeArray" : [],
    "DeliveryTime" :0,
    "DeliveryTimeArray": [],
    "OverdueTime": 0,
    "OverdueTimeArray": [],
    "DeliveredOrders": 0,
    "DeliveredOrdersArray": [],
    "OverdueOrders": 0,
    "OverdueOrdersArray": [],
    "OrderPercentage": 0.0,
    "OrderPercentageArray": []
    }

def processOrderPercentage(data):
    element = float(data[0].replace(',','.'))
    results["OrderPercentage"] += element
    results["OrderPercentageArray"].append(element)
    return

def processChargePercentage(line):
    data = [s for s in re.findall("NaN|\d+\.\d+", line)]
    for index,element in enumerate(data):
        if element == "NaN": continue
        if index == 0:
            results["lwCharge"] += float(element)
            results["lwChargeArray"].append(float(element))
        if index == 1:
            results["hwCharge"] += float(element)
            results["hwChargeArray"].append(float(element))

def processTime(data, key):
    assert len(data) == 3
    timeString = data[0] + ':' + data[1] + ':' + data[2]
    timeStruct = time.strptime(timeString,'%H:%M:%S')
    h = timeStruct.tm_hour
    m = timeStruct.tm_min
    s = timeStruct.tm_sec
    seconds = datetime.timedelta(hours=h,minutes=m,seconds=s).total_seconds()
    results[key] += seconds
    results[key+"Array"].append(seconds)

def processFile(file):
    lines = file.readlines()
    for index,line in enumerate(lines):
        if index == 0 or index == 1: continue
        data = [s for s in re.findall(r'NaN|-?\d+\,?\d*', line)] # Contains all the numberic data of the line
        if index == 2: processChargePercentage(line)
        if index == 3: processTime(data, "DeliveryTime")
        if index == 4: processTime(data,  "OverdueTime")
        if index == 5:
            results["DeliveredOrders"] += int(data[0])
            results["DeliveredOrdersArray"].append(int(data[0]))
        if index == 6:
            results["OverdueOrders"] += int(data[0])
            results["OverdueOrdersArray"].append(int(data[0]))
        if index == 7: processOrderPercentage(data)


def getConfInterval(mean, deviation):
    return t.interval(0.95, 99, loc=mean, scale=deviation)

def calculateStandardDeviation(key, data, mean, N):
    dataArray = np.array(data)
    interval = t.interval(0.95, dataArray.shape[0] - 1, loc=mean, scale=scipy.stats.sem(dataArray))
    print(key)
    print("Standard deviation value: {}".format(np.std(dataArray)))
    print("Confidence interval: {}".format(interval))


def outputResults():
    for key in results.keys():
        if "Array" in key:
            specificKey = key.replace("Array",'')
            # print(results[key])
            calculateStandardDeviation(specificKey, results[key], results[specificKey], numberOfFiles)
            continue
        if "Time" in key:
            print(key + ': ' + str(datetime.timedelta(seconds=round(results[key]))))
            continue
        print(key + ': ' + str(results[key]))

if __name__ == '__main__':
    directory = "experiments"
    if (len(sys.argv) >= 2): directory = sys.argv[1]
    fileNames = [directory+"/" + name for name in listdir(directory)]
    numberOfFiles = len(fileNames)
    print("Processing results for the {} directory (containing {} files)".format(directory, numberOfFiles))
    for fileName in fileNames:
        # print(fileName)
        file = open(fileName, 'r')
        processFile(file)
    
    for key in results.keys():
        if "Array" in key: 
                continue
        results[key] /= numberOfFiles
    # outputResults()

    print("Research question 1 (orders on time)")
    print("\tConfidence: " + str(getConfInterval(80, np.std(np.array(results["OrderPercentageArray"])))) )
    print("\tActual mean: " + str(results["OrderPercentage"]))

    print("Research question 2 (average delivery times)")
    print("\tConfidence: " + str(getConfInterval(300, np.std(np.array(results["DeliveryTimeArray"])))) )
    print("\tActual mean: " + str(results["DeliveryTime"]))

    print("Research question 3 (occupation charging station) - LW")
    print("\tConfidence: " + str(getConfInterval(95, np.std(np.array(results["lwChargeArray"])))) )
    print("\tActual mean: " + str(results["lwCharge"]))

    print("Research question 3 (occupation charging station) - HW")
    print("\tConfidence: " + str(getConfInterval(95, np.std(np.array(results["hwChargeArray"])))) )
    print("\tActual mean: " + str(results["hwCharge"]))

    print("Research question 4 (average lateness)")
    print("\tConfidence: " + str(getConfInterval(60, np.std(np.array(results["OverdueTimeArray"])))) )
    print("\tActual mean: " + str(results["OverdueTime"]))




    # print(results["hwChargeArray"])
    # print("Research question 5 (average delivery times)")
    # print("\tConfidence: " + str(getConfInterval(results["DeliveryTime"], np.std(np.array(results["DeliveryTimeArray"])))) )
    # # print("\tConfidence hw: " + str(getConfInterval(results["DeliveryTime"], np.std(np.array(results["DeliveryTimeArray"])))) )
    # # print("\tActual mean: " + str(results["DeliveryTime"]))

    # print("Research question 6 (packages too late)")
    # print("\tConfidence: " + str(getConfInterval(results["OrderPercentage"], np.std(np.array(results["OrderPercentageArray"])))) )
    # # print("\tActual mean: " + str(results["DeliveryTime"]))

    # print("Research question 7 (stress charging station)")
    # print("\tConfidence: " + str(getConfInterval(results["lwCharge"], np.std(np.array(results["lwChargeArray"])))) )
    # print("\tConfidence: " + str(getConfInterval(results["hwCharge"], np.std(np.array(results["hwChargeArray"])))) )
    # print("\tActual mean: " + str(results["DeliveryTime"]))


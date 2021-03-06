from os import listdir
import re
import datetime
import time
import sys

# Dictionary containing all the results
results = {
    "lwCharge": 0.0,
    "hwCharge" : 0.0,
    "DeliveryTime" :0,
    "OverdueTime": 0,
    "DeliveredOrders": 0,
    "OverdueOrders": 0,
    "OrderPercentage": 0.0
    }

def processOrderPercentage(data):
    results["OrderPercentage"] += float(data[0].replace(',','.'))
    return
	
def processChargePercentage(data):
    for index,element in enumerate(data):
        if element == "NaN": continue
        if index == 0: results["lwCharge"] += float(element.replace(',','.'))
        if index == 1: results["hwCharge"] += float(element.replace(',','.'))

def processTime(data, key):
    assert len(data) == 3
    timeString = data[0] + ':' + data[1] + ':' + data[2]
    timeStruct = time.strptime(timeString,'%H:%M:%S')
    h = timeStruct.tm_hour
    m = timeStruct.tm_min
    s = timeStruct.tm_sec
    seconds = datetime.timedelta(hours=h,minutes=m,seconds=s).total_seconds()
    results[key] += seconds

def processFile(file):
    lines = file.readlines()
    for index,line in enumerate(lines):
        if index == 0 or index == 1: continue
        data = [s for s in re.findall(r'NaN|-?\d+\,?\d*', line)] # Contains all the numberic data of the line
        if index == 2: processChargePercentage(data)
        if index == 3: processTime(data, "DeliveryTime")
        if index == 4: processTime(data,  "OverdueTime")
        if index == 5: results["DeliveredOrders"] += int(data[0])
        if index == 6: results["OverdueOrders"] += int(data[0])
        if index == 7: processOrderPercentage(data)

def outputResults():
    for key in results.keys():
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
        file = open(fileName, 'r')
        processFile(file)
    for key in results.keys():
        results[key] /= numberOfFiles
    outputResults()

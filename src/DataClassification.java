import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.opencsv.CSVReader;

////List of possible things to change:

public class DataClassification {
	private static double threshold;
	private static String fileName;
	private static HashMap<Integer, String> allAttributesMap = new HashMap<Integer, String>();
	private static ArrayList<ArrayList<String>> allData = new ArrayList<ArrayList<String>>();
	private static HashMap<String, Integer> baseResultCounter = new HashMap<String, Integer>(); 
	private static HashMap<Integer, ArrayList<String>> allAttributeValues = new HashMap<Integer, ArrayList<String>>();
	private static int nodeCount = 0;
	private static boolean printTree = false;
	private static boolean testAllTrees = true;
	
	public static void main(String[] args) throws IOException {
		threshold = Double.parseDouble(args[0]);
		fileName = args[1];
		printTree = Boolean.parseBoolean(args[2]); //Whether or not to dump trees as they are created
		testAllTrees = Boolean.parseBoolean(args[3]); //Whether or not to test all possible trees. Setting to false only tests the first data piece
		CSVReader reader = new CSVReader(new FileReader(fileName), ',' , '"' , 0);
		
		buildAllAttributesMap(reader);
		buildAllData(reader);
		buildBaseResultCounter();
		buildAllAttributeValues();
		testDecisionTrees();
	}
	
	private static Node decisionTree(ArrayList<ArrayList<String>> subDataSet, HashMap<Integer, String> subAttributeMap, Node rootNode, int indentLevel) {
		
		//If all the results are the same within a subDataSet, make a decision for said unanimous result
		if (areAllResultsSame(subDataSet)) {
			printIndentedMessage(indentLevel, "Decide " + subDataSet.get(0).get(0) + "\n");
			rootNode.attribute = subDataSet.get(0).get(0);
			return rootNode;
		}
		
		//If you have no attributes left
		//Just guess based on which final result has the most
		else if (subAttributeMap.size() == 0) {
			String mostFrequentResult = getMostFrequentResult(getResultCounter(subDataSet));
			printIndentedMessage(indentLevel, "Decide " + mostFrequentResult + "\n");
			rootNode.attribute = mostFrequentResult;
			return rootNode;
		}
		
		else {
			
			//Start computing gain values
			HashMap<Integer, Double> gainVals = new HashMap<Integer, Double>();
			HashMap<String, Integer> resultCounter = getResultCounter(subDataSet);
			
			gainVals.put(0, impurityEvalOne(resultCounter));
			for (Entry<Integer, String> entry : subAttributeMap.entrySet()) {
				gainVals.put(entry.getKey(), gainVals.get(0) - impurityEvalTwo(entry.getKey(), subDataSet));
			}
			gainVals.put(0, 0.0);
			
			//Could not combine above and below for loops because there is no command to
			//grab an Entry from a HashMap by key, nor can an Entry be instantiated
			
			//Find the entry with the biggest gain value
			Entry<Integer, Double> biggestEntry = gainVals.entrySet().iterator().next();

			for (Entry<Integer, Double> entry : gainVals.entrySet()) {
				if (entry.getValue() > biggestEntry.getValue()) {
					biggestEntry = entry;
				}
			}
			
			//If biggest gain is less than threshold, stop
			if (biggestEntry.getValue() < threshold) {
				String mostFrequentResult = getMostFrequentResult(resultCounter);
				printIndentedMessage(indentLevel, "Decide " + mostFrequentResult + "\n");
				rootNode.attribute = mostFrequentResult;
				return rootNode;
			}
			
			//List the attribute to break into values
			else {
				
				//Decide which attribute to branch on
				String attributeOfChoice = subAttributeMap.get(biggestEntry.getKey());
				int indexOfAttribute = biggestEntry.getKey();
				printIndentedMessage(indentLevel, "Branch on " + attributeOfChoice + "\n");
				
				rootNode.attribute = attributeOfChoice;
				rootNode.attributeIndex = indexOfAttribute;
				subAttributeMap.remove(attributeOfChoice);
				
				//Getting ready to divide our dataSet into subsets according to each dataPiece's value of the chosen attribute
				HashMap<String, ArrayList<ArrayList<String>>> dataSubsetStorage = new HashMap<String, ArrayList<ArrayList<String>>>();
				for (String value : allAttributeValues.get(indexOfAttribute)) {
					dataSubsetStorage.put(value, new ArrayList<ArrayList<String>>());
				}
				
				//Add the dataPiece to the subset corresponding to its value of the chosen attribute
				for (ArrayList<String> dataPiece : subDataSet) {
					dataSubsetStorage.get(dataPiece.get(indexOfAttribute)).add(dataPiece);
				}
				
				//Make a Node for each value of the chosen attribute, so long as there is data for that value
				for (Entry<String, ArrayList<ArrayList<String>>> entry : dataSubsetStorage.entrySet()) {
					if (entry.getValue().size() != 0) {
						printIndentedMessage(indentLevel, "If " + attributeOfChoice + " is " + entry.getKey() + "\n");
						nodeCount++;
						Node nodeToSend = new Node("", -1, new ArrayList<Pointer>());
						nodeToSend = decisionTree(deepCopyDataSet(entry.getValue()), deepCopyAttributeMap(subAttributeMap), nodeToSend, indentLevel + 1);
						rootNode.pointerArray.add(new Pointer(entry.getKey(), nodeToSend));
					}
				}
			}
		}
		return rootNode; 
	}
	
	public static double impurityEvalOne(HashMap<String, Integer> resultCounter) {
		double totalImpurity = 0;
		double totalResultNum = 0;
		
		//Adding up the total number of results
		for (Entry<String, Integer> entry : resultCounter.entrySet()) {
			totalResultNum += entry.getValue();
		}
		
		//Following impurityOne equation for each type of result
		for (Entry<String, Integer> entry : resultCounter.entrySet()) {
			if (entry.getValue() != 0) {
				double entryProbability = entry.getValue() / totalResultNum;
				totalImpurity += entryProbability * (Math.log(entryProbability)/Math.log(2));
			}
		}
		return totalImpurity * -1;
	}

	public static double impurityEvalTwo(int attributeIndex, ArrayList<ArrayList<String>> subDataSet) {
		
		//Add to the runningTotal for each attribute value
		double runningTotal = 0;
		
		//Prepare to divide the data set by value of the chosen attribute
		HashMap<String, ArrayList<ArrayList<String>>> subsetSorter = new HashMap<String, ArrayList<ArrayList<String>>>();
		for (String value : allAttributeValues.get(attributeIndex)) {
			subsetSorter.put(value, new ArrayList<ArrayList<String>>());
		}
		
		//Divide data set by value
		for (ArrayList<String> dataPiece : subDataSet) {
			subsetSorter.get(dataPiece.get(attributeIndex)).add(dataPiece);
		}
		
		//Follow equation for impurity for each value
		//Add impurity onto running total
		for (Entry<String, ArrayList<ArrayList<String>>> entry : subsetSorter.entrySet()) {
			if (entry.getValue().size() > 0) {
				runningTotal += (entry.getValue().size() / (double) subDataSet.size()) * impurityEvalOne(getResultCounter(entry.getValue()));
			}
		}
		
		return runningTotal;
	}

	public static void printIndentedMessage(int numIndents, String message) {
		if (printTree) {
			for (int i = 0; i < numIndents; i++){
				System.out.print("   ");
			}
			System.out.print(message);
		}
	}
	
	public static String getMostFrequentResult(HashMap<String, Integer> resultsCount) {
		Entry<String, Integer> mostFrequentEntry = resultsCount.entrySet().iterator().next();
		
		for (Entry<String, Integer> entry : resultsCount.entrySet()) {
			if (entry.getValue() > mostFrequentEntry.getValue()) {
				mostFrequentEntry = entry;
			}
		}
		
		return mostFrequentEntry.getKey();
	}
	
	public static boolean areAllResultsSame(ArrayList<ArrayList<String>> subDataSet) {
		//Chose to make this function as opposed to cycling through a resultCount HashMap
		//Since this doesn't require you to build the HashMap in cases where this comes out to true
		//and this loop finds the result faster in most cases
		
		boolean allSame = true;
		String firstClassification = subDataSet.get(0).get(0);
		for (int index = 1; allSame && index < subDataSet.size(); index++) {
			if (!subDataSet.get(index).get(0).equals(firstClassification)) {
				return false;
			}
		}
		return true;
	}
	
	public static HashMap<String, Integer> getResultCounter(ArrayList<ArrayList<String>> subDataSet) {
		
		//resultCounter is a deep copy of the baseResultCounter
		HashMap<String, Integer> resultCounter = new HashMap<String, Integer>();
		for (Entry<String, Integer> entry : baseResultCounter.entrySet()) {
			resultCounter.put(entry.getKey(), entry.getValue());
		}
		
		//Count up the number of each classification in the given data set
		for (ArrayList<String> dataPiece : subDataSet) {
			resultCounter.put(dataPiece.get(0), resultCounter.get(dataPiece.get(0)) + 1);
		}
		return resultCounter;
	}
	
	public static ArrayList<ArrayList<String>> deepCopyDataSet(ArrayList<ArrayList<String>> dataSet) {
		ArrayList<ArrayList<String>> deepCopy = new ArrayList<ArrayList<String>>();
		
		for (ArrayList<String> dataPiece : dataSet) {
			deepCopy.add(dataPiece);
		}
		
		return deepCopy;
	}
	
	public static HashMap<Integer, String> deepCopyAttributeMap(HashMap<Integer, String> attributeMap) {
		HashMap<Integer, String> deepMap = new HashMap<Integer, String>();
		
		for (Entry<Integer, String> entry : attributeMap.entrySet()) {
			deepMap.put(entry.getKey(), entry.getValue());
		}
		
		return deepMap;
	}
	
	public static void buildAllAttributesMap(CSVReader reader) {
		String[] tempAttributes;
		
		try { tempAttributes = reader.readNext(); }
		catch (Exception e) {
			tempAttributes = new String[0];
			System.out.println("Error: File is empty");
		}
		
		for (int i = 1; i < tempAttributes.length; i++) {
			allAttributesMap.put(i, tempAttributes[i]);
		}
	}
	
	public static void buildAllData(CSVReader reader) {
		//Reading in all the data and putting each line either in trainData or testData
		
		String[] nextLine;
		try {
			while ((nextLine = reader.readNext()) != null) {
				ArrayList<String> newDataPiece = new ArrayList<String>();  //Gather the data from that line
				for (String datum : nextLine) {
					newDataPiece.add(datum);
				}
				allData.add(newDataPiece);
				
			}
		}
		
		catch (IOException e) {
			System.out.println("Error: trouble reading in all data");
		}
	}
	
	public static void buildBaseResultCounter() {
		//Gathering all the possible classifications
		for (ArrayList<String> dataPiece : allData) {
			String result = dataPiece.get(0);
			if (!baseResultCounter.containsKey(result)) {
				baseResultCounter.put(result, 0);
			}
		}
	}
	
	public static void buildAllAttributeValues() {
		for (Entry<Integer, String> entry : allAttributesMap.entrySet()) {
			
			ArrayList<String> possibleValues = new ArrayList<String>();
			
			for (ArrayList<String> dataPiece : allData) {
				if (!possibleValues.contains(dataPiece.get(entry.getKey()))) {
					possibleValues.add(dataPiece.get(entry.getKey()));
				}
			}
			
			allAttributeValues.put(entry.getKey(), possibleValues);
		}
	}
	
	public static void testDecisionTrees() {
		int numTotal = 0, numCorrect = 0;
		
		//Each time through the loop, pick a different data piece and test that data piece against
		//the tree made from all of the remaining data pieces
		for (int testIndex = 0; testIndex < (testAllTrees ? allData.size() : 1); testIndex++) {
			long startTime = System.currentTimeMillis();
			
			ArrayList<String> testDataPiece = allData.get(testIndex);
			
			allData.remove(testIndex);
			
			nodeCount = 1;
			Node rootNode = decisionTree(deepCopyDataSet(allData), deepCopyAttributeMap(allAttributesMap), new Node("root", -1, new ArrayList<Pointer>()), 0);

			if (getDecision(testDataPiece, rootNode).equals(testDataPiece.get(0))) {
				numCorrect++;
			}
			numTotal++;
			
			allData.add(testIndex, testDataPiece);
			System.out.println("Testing data index: " + testIndex + "   Correct: " + numCorrect + "   Total: " + numTotal + 
			 "   Time: " + (System.currentTimeMillis() - startTime) + "   Nodes: " + nodeCount);
		}
		
		System.out.println("Out of " + numTotal + " tests, " + numCorrect + " results came out accurately.");
	}

	public static String getDecision(ArrayList<String> dataPiece, Node rootNode) {
		
		//If there are no pointers, the final decision is the attribute in the Node
		if (rootNode.pointerArray.size() == 0) {return rootNode.attribute;}
		
		//This result should not ever actually happen, but was left for safety
		else if (rootNode.pointerArray.size() == 1) {return getDecision(dataPiece, rootNode.pointerArray.get(0).goToNode);}
		
		//Follow the appropriate pointer down the tree according to attribute being analyzed and the value of the data piece
		else {
			String value = dataPiece.get(rootNode.attributeIndex);
			for (Pointer pointer : rootNode.pointerArray) {
				if (pointer.attributeValue.equals(value)) {
					return getDecision(dataPiece, pointer.goToNode);
				}
			}
		}
		return "This shouldn't happen";
	}
}
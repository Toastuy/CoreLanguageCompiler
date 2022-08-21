import java.util.*;

class CoreVar {
	Core type;
	Integer value;
	
	public CoreVar(Core varType) {
		type = varType;
		if (type == Core.INT) {
			value = 0;
		} else {
			value = null;
		}
	}
}

class Executor {
	
	static HashMap<String, CoreVar> globalSpace;
	static ArrayList<Integer> heapSpace;
	
	static Scanner dataFile;
	
	// stackSpace is now our call stack
	static Stack<Stack<HashMap<String, CoreVar>>> stackSpace;
	
	// This will store all FuncDecls so we can look up the function being called
	static HashMap<String, FuncDecl> funcDefinitions;
	
	// Keep track of variable references, this will be 1:1 with the Heap space
	static ArrayList<Integer> referenceSpace;
	static int numOfReachables;
	
	/*
	Overriding some methods from the super class to handle the call stack
	*/
	
	static void initialize(String dataFileName) {
		globalSpace = new HashMap<String, CoreVar>();
		heapSpace = new ArrayList<Integer>();
		dataFile = new Scanner(dataFileName);
		
		stackSpace = new Stack<Stack<HashMap<String, CoreVar>>>();
		funcDefinitions = new HashMap<String, FuncDecl>();
		
		referenceSpace = new ArrayList<Integer>();
		
		numOfReachables = 0;
	}
	
	static void pushLocalScope() {
		stackSpace.peek().push(new HashMap<String, CoreVar>());
	}
	
	static void popLocalScope() {
		//System.out.println("Ref space at beginning of pop local scope: " + referenceSpace);
		HashMap<String, CoreVar> frame = stackSpace.peek().pop();
		//System.out.println("pop local scope frame:  " + frame);
		for (CoreVar value : frame.values()) {
			if (value.type == Core.REF && value.value != null) {
				referenceSpace.set(value.value, 0);
			}
		}
		//System.out.println("Ref space at end of pop local scope: " + referenceSpace);
		printReferences();
	}
	
	static int getNextData() {
		int data = 0;
		if (dataFile.currentToken() == Core.EOF) {
			System.out.println("ERROR: data file is out of values!");
			System.exit(0);
		} else {
			data = dataFile.getCONST();
			dataFile.nextToken();
		}
		return data;
	}
	
	static void allocate(String identifier, Core varType) {
		CoreVar record = new CoreVar(varType);
		// If we are in the DeclSeq, no frames will have been created yet
		if (stackSpace.size()==0) {
			globalSpace.put(identifier, record);
		} else {
			stackSpace.peek().peek().put(identifier, record);
		}
	}
	
	static CoreVar getStackOrStatic(String identifier) {
		CoreVar record = null;
		for (int i=stackSpace.peek().size() - 1; i>=0; i--) {
			if (stackSpace.peek().get(i).containsKey(identifier)) {
				record = stackSpace.peek().get(i).get(identifier);
				break;
			}
		}
		if (record == null) {
			record = globalSpace.get(identifier);
		}
		return record;
	}
	
	static void heapAllocate(String identifier) {
		CoreVar x = getStackOrStatic(identifier);
		if (x.type != Core.REF) {
			System.out.println("ERROR: " + identifier + " is not of type ref, cannot perform \"new\"-assign!");
			System.exit(0);
		}
		x.value = heapSpace.size();
		
		// Initialize the corresponding space in our reference list to have 1 reference
		referenceSpace.add(x.value, 1);
		printReferences();
		
		//System.out.println("heapAllocate Ref Space: " + referenceSpace);
		//System.out.println("heapAllocate heap: " + heapSpace);
		heapSpace.add(null);
	}
	
	static Core getType(String identifier) {
		CoreVar x = getStackOrStatic(identifier);
		return x.type;
	}
	
	static Integer getValue(String identifier) {
		CoreVar x = getStackOrStatic(identifier);
		Integer value = x.value;
		if (x.type == Core.REF) {
			try {
				value = heapSpace.get(value);
			} catch (Exception e) {
				System.out.println("ERROR: invalid heap read attempted!");
				System.exit(0);
			}
		}
		return value;
	}
	
	static void storeValue(String identifier, int value) {
		CoreVar x = getStackOrStatic(identifier);
		if (x.type == Core.REF) {
			try {
				heapSpace.set(x.value, value);
			} catch (Exception e) {
				System.out.println("ERROR: invalid heap write attempted!");
				System.exit(0);
			}
		} else {
			x.value = value;
		}
	}
	
	static void referenceCopy(String var1, String var2) {
		CoreVar x = getStackOrStatic(var1);
		CoreVar y = getStackOrStatic(var2);
		x.value = y.value;
	}
	
	/*
	New methods to handle pushing/popping frames and storing function definitions
	*/
	
	static void storeFuncDef(Id name, FuncDecl definition) {
		funcDefinitions.put(name.getString(), definition);
	}
	
	static Formals getFormalParams(Id name) {
		if (!funcDefinitions.containsKey(name.getString())) {
			System.out.println("ERROR: Function call " + name.getString() + " has no target!");
			System.exit(0);
		}
		return funcDefinitions.get(name.getString()).getFormalParams();
	}
	
	static StmtSeq getBody(Id name) {
		return funcDefinitions.get(name.getString()).getBody();
	}
	
	static void pushFrame() {
		stackSpace.push(new Stack<HashMap<String, CoreVar>>());
		pushLocalScope();
	}
	
	static void pushFrame(Formals formalParams, Formals actualParams) {
		List<String> formals = formalParams.execute();
		List<String> actuals = actualParams.execute();
		
		Stack<HashMap<String, CoreVar>> newFrame = new Stack<HashMap<String, CoreVar>>();
		newFrame.push(new HashMap<String, CoreVar>());
		
		for (int i=0; i<formals.size(); i++) {
			CoreVar temp = new CoreVar(Core.REF);
			temp.value = getStackOrStatic(actuals.get(i)).value;
			//System.out.println(formals.get(i) + " " + actuals.get(i) + " passing:" + temp.value+ " heap:" + heapSpace.get(temp.value));
			newFrame.peek().put(formals.get(i), temp);
		}
		
		stackSpace.push(newFrame);
		pushLocalScope();
	}
	
	static void popFrame() {
		//System.out.println("Ref space at beginning of pop frame: " + referenceSpace);
		Stack<HashMap<String, CoreVar>> frame = stackSpace.pop();
		//System.out.println("frame: " + frame);
		
		for (HashMap<String, CoreVar> e : frame) {
			for (CoreVar value : e.values()) {
				if (value.type == Core.REF && value.value != null) {
					referenceSpace.set(value.value, 0);
				}
			}
		}
		//System.out.println("Ref space at end of pop frame: " + referenceSpace);
		printReferences();
	}
	
	// GARBAGE COLLECTION STUFF
	static void printReferences() {
		
		int i = 0;
		
		// Calculate how many reachable references we currently have
		// If we have one, increment our temp i
		for (int e : referenceSpace) {
			if (e != 0) {
				i++;
			}
		}
		
		// if i is different than our current numOfReachables, update numOfReachables
		if (i != numOfReachables) {
			numOfReachables = i;
			
			// Finally print our number of reachables
			// But only if the value differs from the previous one
			System.out.println("gc:" + numOfReachables);
		}
	}

}
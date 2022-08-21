class Assign implements Stmt {
	int type;
	Id assignTo;
	Id assignFrom;
	Expr expr;
	
	public void parse() {
		assignTo = new Id();
		assignTo.parse();
		Parser.expectedToken(Core.ASSIGN);
		Parser.scanner.nextToken();
		if (Parser.scanner.currentToken() == Core.NEW) {
			type = 1;
			Parser.scanner.nextToken();
		} else if (Parser.scanner.currentToken() == Core.REF) {
			type = 2;
			Parser.scanner.nextToken();
			assignFrom = new Id();
			assignFrom.parse();
		} else {
			type = 3;
			expr = new Expr();
			expr.parse();
		}
		Parser.expectedToken(Core.SEMICOLON);
		Parser.scanner.nextToken();
	}
	
	public void print(int indent) {
		for (int i=0; i<indent; i++) {
			System.out.print("\t");
		}
		assignTo.print();
		System.out.print("=");
		if (type == 1) {
			System.out.print("new");
		} else if (type == 2) {
			System.out.print("ref ");
			assignFrom.print();
		} else {
			expr.print();
		}
		System.out.println(";");
	}
	
	public void execute() {
		if (type == 1) {
			// Doing a "new"-assign
			assignTo.heapAllocate();
		} else if (type == 2) {
			// Doing a "ref"-assign
			assignTo.referenceCopy(assignFrom);
		} else {
			// Doing a regular assign
			assignTo.storeValue(expr.execute());
		}
	}
}




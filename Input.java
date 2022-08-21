class Input implements Stmt {
	Id id;
	
	public void parse() {
		Parser.scanner.nextToken();
		id = new Id();
		id.parse();
		Parser.expectedToken(Core.SEMICOLON);
		Parser.scanner.nextToken();
	}
	
	public void print(int indent) {
		for (int i=0; i<indent; i++) {
			System.out.print("\t");
		}
		System.out.print("input ");
		id.print();
		System.out.println(";");
	}
	
	public void execute() {
		id.storeValue(Executor.getNextData());
	}
}
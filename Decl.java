class Decl implements Stmt {
	DeclInt declInt;
	DeclClass declClass;
	
	public void parse() {
		if (Parser.scanner.currentToken() == Core.INT) {
			declInt = new DeclInt();
			declInt.parse();
		} else {
			declClass = new DeclClass();
			declClass.parse();
		}
	}
	
	public void print(int indent) {
		if (declInt != null) {
			declInt.print(indent);
		} else {
			declClass.print(indent);
		}
	}
	
	public void execute() {
		if (declInt != null) {
			declInt.execute();
		} else {
			declClass.execute();
		}
	}
}
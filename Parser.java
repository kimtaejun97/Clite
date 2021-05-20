import jdk.jfr.Experimental;

import java.util.*;

public class Parser {
    // Recursive descent parser that inputs a C++Lite program and 
    // generates its abstract syntax.  Each method corresponds to
    // a concrete syntax grammar rule, which appears as a comment
    // at the beginning of the method.
  
    Token token;          // current token from the input stream
    Lexer lexer;
  
    public Parser(Lexer ts) { // Open the C++Lite source program
        lexer = ts;                          // as a token stream, and
        token = lexer.next();            // retrieve its first Token
    }
  
    private String match (TokenType t) {
        String value = token.value();
        if (token.type().equals(t))
            token = lexer.next();
        else
            error(t);
        return value;
    }
  
    private void error(TokenType tok) {
        System.err.println("Syntax error: expecting: " + tok 
                           + "; saw: " + token);
        System.exit(1);
    }
  
    private void error(String tok) {
        System.err.println("Syntax error: expecting: " + tok 
                           + "; saw: " + token);
        System.exit(1);
    }
  
    public Program program() {
        // Program --> int main ( ) '{' Declarations Statements '}'
        TokenType[ ] header = {TokenType.Int, TokenType.Main,
                          TokenType.LeftParen, TokenType.RightParen};
        for (int i=0; i<header.length; i++)   // bypass "int main ( )"
            match(header[i]);
        match(TokenType.LeftBrace);
        // student exercise
        Declarations d = declarations();
        Block b = statements();
        match(TokenType.RightBrace);
        return new Program(d, b);  // student exercise
    }
  
    private Declarations declarations () {
        // Declarations --> { Declaration }
        Declarations ds = new Declarations();
        while(isType()){
            declaration(ds);
        }

        return ds;  // student exercise
    }
  
    private void declaration (Declarations ds) {
        // Declaration  --> Type Identifier { , Identifier } ;
        Type t = type();
        Variable v = new Variable(match(TokenType.Identifier));
        Declaration d = new Declaration(v, t);
        ds.add(d);
        while(token.type().equals(TokenType.Comma)){
            match(TokenType.Comma);
            v = new Variable(match(TokenType.Identifier));
            d = new Declaration(v,t);
            ds.add(d);
        }
        match(TokenType.Semicolon);
        // student exercise
    }
  
    private Type type () {
        // Type  -->  int | bool | float | char 
        Type t = null;
        // student exercise
        if(token.type() ==TokenType.Int){
            t = Type.INT;
            match(TokenType.Int);
        }
        else if(token.type() == TokenType.Bool){
            t = Type.BOOL;
            match(TokenType.Bool);
        }
        else if(token.type() == TokenType.Char){
            t = Type.CHAR;
            match(TokenType.Char);
        }
        else if(token.type() == TokenType.Float){
            t = Type.FLOAT;
            match(TokenType.Float);
        }
        else
            error(token.type());

        return t;          
    }
  
    private Statement statement() {
        // Statement --> ; | Block | Assignment | IfStatement | WhileStatement | Call
        Statement s=null;
        if(token.type().equals(TokenType.Semicolon))
            s = new Skip(); //semicolon
        else if(token.type().equals(TokenType.LeftBrace)){
            match(TokenType.LeftBrace);
            s = statements();
            match(TokenType.RightBrace);
        }
        else if(token.type().equals(TokenType.Identifier)){
            s = assignmentOrCall();
        }
        else if(token.type().equals(TokenType.If)){
            s= ifStatement();
        }
        else if(token.type().equals(TokenType.While)){
            s= whileStatement();
        }
        else{
            error(token.type());
        }
        // student exercise
        return s;
    }
  
    private Block statements () {
        // Block --> '{' Statements '}'
        Block b = new Block();
        Statement s;
        while(!token.type().equals(TokenType.RightBrace)){
            s = statement();
            b.members.add(s);
        }
        // student exercise
        return b;
    }

//    private Block block(){
//        // Block --> '{' Statements '}'
//        Block b = new Block();
//        Statement s;
//        match(TokenType.LeftBrace);
//        while(token.type() !=TokenType.RightBrace){
//            s = statement();
//            b.members.add(s);
//        }
//        match(TokenType.RightBrace);
//        return b;
//    }
  
    private Assignment assignment () {
        // Assignment --> Identifier = Expression ;
        Variable t = new Variable(match(TokenType.Identifier));
        match(TokenType.Assign);
        Expression e = expression();
        match(TokenType.Semicolon);

        return new Assignment(t,e);  // student exercise
    }
    private Statement assignmentOrCall(){
        Variable v = new Variable(match(TokenType.Identifier));
        Call c = new Call();

        if(token.type().equals(TokenType.Assign)){
            match(TokenType.Assign);
            Expression src =expression();
            match(TokenType.Semicolon);
            return new Assignment(v, src);
        }
        else if(token.type().equals(TokenType.LeftParen)){
            match(TokenType.LeftParen);
            c.name = v.id();
            c.args = arguments();
            match(TokenType.RightParen);
            match(TokenType.Semicolon);
            return c;
        }
        else{
            error("error in assignOrCall");
        }
        return null;
    }

    private  Expressions arguments(){
        Expressions args = new Expressions();
        while(!token.type().equals(TokenType.RightParen)){
            Expression e = expression();
            args.add(e);

            if(token.type().equals(TokenType.Comma))
                match(TokenType.Comma);
            else if(token.type().equals(TokenType.LeftParen))
                error("Call Expression Error!");
        }
        if (args.size() ==0)
            return null;
        else
            return args;
    }



    private Conditional ifStatement () {
        // IfStatement --> if (Expression ) Statement [ else Statement ]
        match(TokenType.If);
        match(TokenType.LeftParen);
        Expression t =expression();
        match(TokenType.RightParen);
        Statement tp =statement();
        if(token.type().equals(TokenType.Else)){
            Statement ep = statement();
            return new Conditional(t,tp,ep);
        }
        return new Conditional(t,tp);  // student exercise
    }
  
    private Loop whileStatement () {
        // WhileStatement --> while ( Expression ) Statement
        match(TokenType.While);
        match(TokenType.LeftParen);
        Expression t = expression();
        match(TokenType.RightParen);
        Statement b = statement();

        return new Loop(t, b);  // student exercise
    }   

    private Expression expression () {
        // Expression --> Conjunction { || Conjunction }
        Expression e = conjunction();
        while(token.type().equals(TokenType.Or)){
            match(TokenType.Or);
            Operator op = new Operator(Operator.OR);
            Expression cj2 = conjunction();
            e = new Binary(op, e, cj2);
        }
        return e;  // student exercise
    }
  
    private Expression conjunction () {
        // Conjunction --> Equality { && Equality }
        Expression e = equality();
        while(token.type().equals(TokenType.And)){
            match(TokenType.And);
            Operator op = new Operator(Operator.AND);
            Expression eq2 = equality();
            e = new Binary(op, e, eq2);
        }
        return e;  // student exercise
    }
  
    private Expression equality () {
        // Equality --> Relation [ EquOp Relation ]
        Expression e = relation();
        if(isEqualityOp()){
            Operator op = new Operator(match(token.type()));
            Expression rel2 = relation();
            return new Binary(op, e, rel2);
        }
        return e;  // student exercise
    }

    private Expression relation (){
        // Relation --> Addition [RelOp Addition]
        Expression e = addition();
        if (isRelationalOp()){
            Operator op = new Operator(match(token.type()));
            Expression addition2 = addition();
            return new Binary(op,e,addition2);

        }
        return e;  // student exercise
    }
  
    private Expression addition () {
        // Addition --> Term { AddOp Term }
        Expression e = term();
        while (isAddOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = term();
            e = new Binary(op, e, term2);
        }
        return e;
    }
  
    private Expression term () {
        // Term --> Factor { MultiplyOp Factor }
        Expression e = factor();
        while (isMultiplyOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = factor();
            e = new Binary(op, e, term2);
        }
        return e;
    }
  
    private Expression factor() {
        // Factor --> [ UnaryOp ] Primary 
        if (isUnaryOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term = primary();
            return new Unary(op, term);
        }
        else return primary();
    }
  
    private Expression primary () {
        // Primary --> Identifier | Literal | ( Expression ) | C
        //             | Type ( Expression )
        Expression e = null;
        if (token.type().equals(TokenType.Identifier)) {
            Variable v = new Variable((match(TokenType.Identifier)));
            e = v ;
            if(token.type().equals(TokenType.LeftParen)){
                //call Expression
                match(TokenType.LeftParen);
                Call c = new Call();
                c.name = v.id();
                c.args = arguments();
                match(TokenType.RightParen);
                e= c;
            }
        } else if (isLiteral()) {
            e = literal();
        } else if (token.type().equals(TokenType.LeftParen)) {
            token = lexer.next();
            e = expression();       
            match(TokenType.RightParen);
        } else if (isType( )) {
            Operator op = new Operator(match(token.type()));
            match(TokenType.LeftParen);
            Expression term = expression();
            match(TokenType.RightParen);
            e = new Unary(op, term);
        } else error("Identifier | Literal | ( | Type");
        return e;
    }



    private Value literal( ) {
        Value v=null;
        if(token.type().equals(TokenType.FloatLiteral)) {
            v = new FloatValue(Float.parseFloat(token.value()));
            match(TokenType.FloatLiteral);

        }
        else if(token.type().equals(TokenType.IntLiteral)) {
            v = new IntValue(Integer.parseInt(token.value()));
            match(TokenType.IntLiteral);

        }
        else if(token.type().equals(TokenType.CharLiteral)) {
            v = new CharValue((token.value().charAt(0)));
            match(TokenType.CharLiteral);

        }
        else if(token.value() == "true") {
            v = new BoolValue(true);
            match(TokenType.True);

        }
        else if (token.value() =="false") {
            v = new BoolValue(false);
            match(TokenType.False);
        }
        else{
            error(token.type());
        }
        return v;

    }
  

    private boolean isAddOp( ) {
        return token.type().equals(TokenType.Plus) ||
               token.type().equals(TokenType.Minus);
    }
    
    private boolean isMultiplyOp( ) {
        return token.type().equals(TokenType.Multiply) ||
               token.type().equals(TokenType.Divide);
    }
    
    private boolean isUnaryOp( ) {
        return token.type().equals(TokenType.Not) ||
               token.type().equals(TokenType.Minus);
    }
    
    private boolean isEqualityOp( ) {
        return token.type().equals(TokenType.Equals) ||
            token.type().equals(TokenType.NotEqual);
    }
    
    private boolean isRelationalOp( ) {
        return token.type().equals(TokenType.Less) ||
               token.type().equals(TokenType.LessEqual) || 
               token.type().equals(TokenType.Greater) ||
               token.type().equals(TokenType.GreaterEqual);
    }
    
    private boolean isType( ) {
        return token.type().equals(TokenType.Int)
            || token.type().equals(TokenType.Bool) 
            || token.type().equals(TokenType.Float)
            || token.type().equals(TokenType.Char);
    }
    
    private boolean isLiteral( ) {
        return token.type().equals(TokenType.IntLiteral) ||
            isBooleanLiteral() ||
            token.type().equals(TokenType.FloatLiteral) ||
            token.type().equals(TokenType.CharLiteral);
    }
    
    private boolean isBooleanLiteral( ) {
        return token.type().equals(TokenType.True) ||
            token.type().equals(TokenType.False);
    }
    
    public static void main(String args[]) {
        Parser parser  = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display();           // display abstract syntax tree
    } //main

} // Parser

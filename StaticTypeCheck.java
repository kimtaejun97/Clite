// StaticTypeCheck.java

import java.util.*;

// Static type checking for Clite is defined by the functions 
// V and the auxiliary functions typing and typeOf.  These
// functions use the classes in the Abstract Syntax of Clite.


public class StaticTypeCheck {

    public static TypeMap typing (Declarations d) {
        TypeMap map = new TypeMap();
        for (Declaration di : d) 
            map.put (di.v, di.t);
        return map;
    }

    public static void check(boolean test, String msg) {
        if (test)  return;
        System.err.println(msg);
        System.exit(1);
    }

    public static void V (Declarations d) {
        // 모든 선언의 이름은 중복되지 않아야 함.
        for (int i=0; i<d.size() - 1; i++)
            for (int j=i+1; j<d.size(); j++) {
                Declaration di = d.get(i);
                Declaration dj = d.get(j);
                check( ! (di.v.equals(dj.v)),
                       "duplicate declaration: " + dj.v);
            }
    } 

    public static void V (Program p) {
        V (p.decpart);
        V (p.body, typing (p.decpart));
    } 

    public static Type typeOf (Expression e, TypeMap tm) {
        if (e instanceof Value) return ((Value)e).type;
        if (e instanceof Variable) {
            Variable v = (Variable)e;

            check (tm.containsKey(v), "undefined variable: " + v);
            return (Type) tm.get(v);
        }
        if (e instanceof Binary) {
            Binary b = (Binary)e;
            if (b.op.ArithmeticOp( ))
                if (typeOf(b.term1,tm)== Type.FLOAT)
                    return (Type.FLOAT);
                else return (Type.INT);
            if (b.op.RelationalOp( ) || b.op.BooleanOp( )) 
                return (Type.BOOL);
        }
        if (e instanceof Unary) {
            Unary u = (Unary)e;
            if (u.op.NotOp( ))        return (Type.BOOL);
            else if (u.op.NegateOp( )) return typeOf(u.term,tm);
            else if (u.op.intOp( ))    return (Type.INT);
            else if (u.op.floatOp( )) return (Type.FLOAT);
            else if (u.op.charOp( ))  return (Type.CHAR);
        }
        if (e instanceof Call){
            Call c = (Call)e;
            if (c.name.equals("getInt")) return Type.INT;
            else if(c.name.equals("getFloat")) return Type.FLOAT;

        }
        throw new IllegalArgumentException("should never reach here");
    } 

    public static void V (Expression e, TypeMap tm) {
        // 모든 value는 유효.
        if (e instanceof Value) 
            return;

        //TypeMap에 존재하는지 검사. 즉 변수가 선언되어 있는지.
        if (e instanceof Variable) { 
            Variable v = (Variable)e;
            check( tm.containsKey(v)
                   , "undeclared variable: " + v);
            return;
        }
        if (e instanceof Binary) {
            Binary b = (Binary) e;
            Type typ1 = typeOf(b.term1, tm);
            Type typ2 = typeOf(b.term2, tm);
            // 각 term이 유효한지.
            V (b.term1, tm);
            V (b.term2, tm);

            //term의 타입 검사
            // 산술 : 두 term의 타입이 동일하고 int,float인지
            if (b.op.ArithmeticOp( ))  
                check( typ1 == typ2 &&
                       (typ1 == Type.INT || typ1 == Type.FLOAT)
                       , "type error for " + b.op);
            //관계 : 두 term의 타입이 동일.
            else if (b.op.RelationalOp( )) 
                check( typ1 == typ2 , "type error for " + b.op);
            //boolean : 두 term의 타입 모두 bool
            else if (b.op.BooleanOp( ))
                check( typ1 == Type.BOOL && typ2 == Type.BOOL,
                       b.op + ": non-bool operand");
            else
                throw new IllegalArgumentException("should never reach here");
            return;
        }
        // student exercise
        else if(e instanceof Unary){
            Unary u = (Unary)e;
            Type t =  typeOf(u.term, tm);
            //term이 유효한지.
            V(u.term, tm);

            if(u.op.NotOp()){
                check(t ==Type.BOOL, "type error :: NotOp "+u.op);
            }

            //형변환.
            else if(u.op.NegateOp()){
                check(t==Type.INT || t==Type.FLOAT,"type error :: NegateOp"+u.op);
            }
            else if(u.op.equals(Operator.FLOAT)){
                check(t==Type.INT, "non-int operand");
            }
            else if(u.op.equals(Operator.INT)){
                check(t==Type.FLOAT || t==Type.CHAR, "non-float/char operand");
            }
            else if(u.op.equals(Operator.CHAR)){
                check(t==Type.INT, "non-int operand");
            }

            else{
                throw new IllegalArgumentException("should never reach here");
            }
            return ;
        }
        if(e instanceof Call){
            Call c = (Call)e;
            if(c.name.equals("getInt") || c.name.equals("getFloat")) return;
            else if(c.name.equals("put")){
                if(c.args ==null)
                    return;
                // args 유효성 검사.
                else{
                    for(int i=0; i<c.args.size(); i++){
                        Expression arg = c.args.get(i);
                        V(arg,tm);
                    }
                }
            }
            else{
                System.out.println("undefined function call" + ((Call) e).args);
                check(false,"undefined function call");
            }
        }


        throw new IllegalArgumentException("should never reach here");
    }

    // 문장의 유효
    public static void V (Statement s, TypeMap tm) {
        if ( s == null )
            throw new IllegalArgumentException( "AST error: null statement");
        //skip문은 항상 유효.
        if (s instanceof Skip) return;
        if (s instanceof Assignment) {
            Assignment a = (Assignment)s;
            //target이 선언되어 있어야 함.
            check( tm.containsKey(a.target)
                   , " undefined target in assignment: " + a.target);
            //source가 유효.
            V(a.source, tm);
            Type ttype = (Type)tm.get(a.target);
            Type srctype = typeOf(a.source, tm);

            //타입이 같지 않을 때 캐스팅 가능한지 검사.
            if (ttype != srctype) {
                if (ttype == Type.FLOAT)
                    check( srctype == Type.INT
                           , "mixed mode assignment to " + a.target);
                else if (ttype == Type.INT)
                    check( srctype == Type.CHAR
                           , "mixed mode assignment to " + a.target);
                else
                    check( false
                           , "mixed mode assignment to " + a.target);
            }
            return;
        } 
        // student exercise IF,LOOP, BLOCK
        else if(s instanceof Conditional){
            Conditional c = (Conditional) s;
            //test식이 유효한지.
            V(c.test,tm);
            Type tType = typeOf(c.test, tm);
            //test의 반환이 bool 타입인지
            if(tType == Type.BOOL){
                V(c.thenbranch, tm);
                V(c.elsebranch, tm);
                return ;
            }
            else{
                check(false, "type error :: Conditional "+c.test);
            }
        }
        else if(s instanceof Loop){
            Loop l =(Loop) s;
            V(l.test, tm);
            Type tType = typeOf(l.test,tm);
            if(tType ==Type.BOOL){
                V(l.body, tm);
                return ;
            }
            else{
                check(false, "type error ::Loop "+l.test);
            }
        }
        else if(s instanceof Block){
            Block b = (Block) s;
            // Block안의 모든 문장이 유효한지.
            for(Statement si : b.members){
                V(si, tm);
                return ;
            }

        }
        else if (s instanceof Call){
            Call c = (Call) s;
            // Expression에서 이미 체크함.
            return ;
        }
        throw new IllegalArgumentException("should never reach here");
    }

    public static void main(String args[]) {
        Parser parser  = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display();           // student exercise
        System.out.println("\nBegin type checking...");
        System.out.println("Type map:");
        TypeMap map = typing(prog.decpart);
        map.display();   // student exercise
        V(prog);
    } //main

} // class StaticTypeCheck


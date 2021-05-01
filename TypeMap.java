import java.util.*;

public class TypeMap extends HashMap<Variable, Type> { 

// TypeMap is implemented as a Java HashMap.  
// Plus a 'display' method to facilitate experimentation.
    public void display(){
        for(Variable v : this.keySet()){
            System.out.println("<"+v.toString()+","+this.get(v)+">");
        }
    }

}

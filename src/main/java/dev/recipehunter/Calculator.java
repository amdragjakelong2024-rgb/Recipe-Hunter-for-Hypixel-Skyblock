package dev.recipehunter;
/** Small arithmetic parser; no scripting engine, reflection or code execution. */
public final class Calculator {
 private final String text;private int pos;
 private Calculator(String s){text=s.replace(" ","");if(text.length()>200)throw new IllegalArgumentException("Expression too long");}
 public static double evaluate(String s){Calculator p=new Calculator(s);double v=p.sum();if(p.pos!=p.text.length()||!Double.isFinite(v))throw new IllegalArgumentException("Invalid expression");return v;}
 private boolean take(char c){if(pos<text.length()&&text.charAt(pos)==c){pos++;return true;}return false;}
 private double sum(){double v=product();while(true){if(take('+'))v+=product();else if(take('-'))v-=product();else return v;}}
 private double product(){double v=atom();while(true){if(take('*'))v*=atom();else if(take('/'))v/=atom();else return v;}}
 private double atom(){if(take('+'))return atom();if(take('-'))return -atom();if(take('(')){double v=sum();if(!take(')'))throw new IllegalArgumentException("Missing )");return v;}int start=pos;while(pos<text.length()&&(Character.isDigit(text.charAt(pos))||text.charAt(pos)=='.'))pos++;if(start==pos)throw new IllegalArgumentException("Number expected");return Double.parseDouble(text.substring(start,pos));}
}

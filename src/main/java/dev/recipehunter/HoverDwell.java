package dev.recipehunter;
public final class HoverDwell {
 private String key="";private long since,last;
 public boolean ready(String value,long now){if(value.isEmpty()||!value.equals(key)||now-last>250){key=value;since=now;}last=now;return !key.isEmpty()&&now-since>=3000;}
}

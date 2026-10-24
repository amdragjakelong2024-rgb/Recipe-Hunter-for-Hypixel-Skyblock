package dev.recipehunter;
/** Optional approximate continuous mining model; user supplies block/drop/uptime assumptions. */
public final class MiningModel {
 public static double rate(double speed,double fortune,double strength,double drops,double uptime){
  if(!Double.isFinite(speed+fortune+strength+drops+uptime)||speed<=0||fortune<0||strength<=0||drops<=0||uptime<=0||uptime>1)throw new IllegalArgumentException("Invalid mining assumptions");
  // Four-tick lower bound, intentionally conservative: no instamine or abilities modelled.
  double seconds=Math.max(4,Math.round(strength*30/speed))/20.0;
  return 3600/seconds*drops*(1+fortune/100)*uptime;
 }
}

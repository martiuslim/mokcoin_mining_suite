package aa;

// Created by kevinsteppe on 4/8/15.
// modified by Mok on 1/1/18 to include the getTimeForDisplay()

public class StopWatch {

    private long start = 0;
    private long end = 0;
    public final int id;

    // constructor
    public StopWatch() {
        id = (int) (Math.random() * 1000);
    }

    // constructor
    public StopWatch(int id) {
        this.id = id;
    }

    // starts the stop watch
    public void start() {
        start = System.currentTimeMillis();
    }

    // Stops the watch, then returns the elapsed time.
    public long stop() {
        end = System.currentTimeMillis();
        return getTime();
    }

    /**
     * If end() has been called, then returns milliseconds elapsed between start
     * and end. If end() has not been called yet, returns milliseconds since
     * start was called (does not stop the watch)
     */
    public long getTime() {
        if (end == 0) {
            return System.currentTimeMillis() - start;
        } else {
            return (end - start);
        }
    }

    // sets start and end to 0
    public void reset() {
        start = end = 0;
    }
    
    // get the Time in the appropriate units for printing purposes
    // the same as getTime(), except that this method returns a String instead. 
    // The String will be in this format "<xx>h, <xx>m, <xx>s, <xx>ms"
    @Override
    public String toString(){
      long time = getTime(); // time is now in milliseconds
      return convertTimeToString(time);
    }
    
    // takes in time in milliseconds and returns a String of this format: "<xx> hrs, <xx> mins, <xx> s, <xx> ms"
    private String convertTimeToString(long time){
      String displayString = "";
      
      // 3_600_000 ms = 1 hour
      if (time/3_600_000 >= 1){
        long hours = time/3_600_000;
        displayString += hours + "h ";
        time %= 3_600_000;         
      }
      
      // 60_000 ms = 1 min
      if (time/60_000 >= 1){
        long min = time/60_000;
        displayString += min + "m ";
        time %= 60_000;
      }
      
      // 1000 ms = 1 s
      if (time/1000 >= 1){
        long sec = time/1000;
        displayString += sec + "s ";
        time %= 1000;
      }   

      // remaining ms
      displayString += time + "ms";
      return displayString;  
    }
    
    // takes in two timings (in milliseconds) and returns a String showing the absolute difference.
    // does not matter whether time1 or time2 is smaller.
    // The String will be in this format "<xx>h, <xx>m, <xx>s, <xx>ms"
    public String getTimeDifferenceForDisplay(long time1, long time2){   
      return convertTimeToString(Math.abs(time1 - time2));  
    }      
}

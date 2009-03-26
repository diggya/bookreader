public class TBookTimer implements Runnable {
    public TBookTimer(TBookMIDlet tbookmidlet) {
        TBM = tbookmidlet;
    }

    public void setPaused(boolean flag) { 
      CT=new Thread(this);
      CT.start();
    }

    public void run() {
      do {
        try {
          long l = System.currentTimeMillis();
          TBM.onTimer();
          long l1 = System.currentTimeMillis();
          l = 1000L - (l1 - l);
          if(l <= 0L) l = 200L;
          wait(l);
        } catch(InterruptedException interruptedexception) { }
      } while(true);
    }

    private TBookMIDlet TBM;
    private Thread CT;
}

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

public class TBookCanvas extends Canvas {
    public TBookCanvas(TBookMIDlet tbookmidlet) { TBM = tbookmidlet; }
    protected void paint(Graphics g) { TBM.paint(g); }
    public void keyPressed(int i) { TBM.keyPressed(i, getGameAction(i)); }
    public void setLight(int i) { }
    private TBookMIDlet TBM;
}

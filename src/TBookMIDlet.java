import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public class TBookMIDlet extends MIDlet implements CommandListener {
    public TBookMIDlet() {
        MBCommandCancel = new Command("Return", 3, 90);
        MBCommandApply = new Command("Apply", 3, 30);
        MBCommandGoto = new Command("GoTo", 3, 20);
        MBCommandSelect = new Command("Select", 4, 10);
        MBCommandOK = new Command("Ok", 4, 30);
        MBMenuChoiceGroup = new ChoiceGroup[10];
        MBMenuGauge = new Gauge[10];
        MBBookmarkPos = new int[5];
        MBBookmarkName = new String[5];
        FirstRun = true;
        MBookRenderableText = new byte[4000];
        MBookPart1 = new byte[8192];
        MBookPos1 = -1;
        MBookPart2 = new byte[8192];
        MBookPos2 = -1;
        MBRenderNextPos = 0;
        MBRenderPrevPos = 0;
        MBRenderBG_R = 255;
        MBRenderBG_G = 255;
        MBRenderBG_B = 255;
        MBRenderFG_R = 0;
        MBRenderFG_G = 0;
        MBRenderFG_B = 0;
        MBPartNo = 0;
        MBFCount = 1;
        String filename;
        InputStream F;
        int FPos=0;
        int fsize=0;

        MBBookTimer = new TBookTimer(this);

// Load config
        try {
            F=getClass().getResourceAsStream("/cfg.dat");
            MBTextLength = (F.read()<<24)+(F.read()<<16)+(F.read()<<8)+F.read();
            MBRenderBG_R = F.read()&0xFF;
            MBRenderBG_G = F.read()&0xFF;
            MBRenderBG_B = F.read()&0xFF;
            MBRenderFG_R = F.read()&0xFF;
            MBRenderFG_G = F.read()&0xFF;
            MBRenderFG_B = F.read()&0xFF;
            MBPartNo 	 = F.read()&0xFF;
            MBFCount	 = F.read()&0xFF;
            fsize =        (F.read()<<8)+F.read();
            MBRenderBookName     = CfgReadLine(F);
            MBRenderBookFont     = CfgReadLine(F);
            MBRenderBookRenderer = CfgReadLine(F);
            F.close();
        } catch(Exception exception) {
          System.out.println("Config IO error: " + exception.getMessage());
        }


        try {
// font.dat format
// 	byte 		Font Height
//	byte		Font Size, in characters (N)
//	word		Font Rendering size, in words
// N times:
//	byte		Character width
//	word		UCS2 representation
//	word[...]	Character view
//            MBFCount=1;
     
            MBFCharW = new short[MBFCount][256];
            MBFPtr   = new short[MBFCount][256];
            MBFCMap  = new int  [MBFCount][256];
            MBFRender= new short[fsize];
            MBFHeight= new short[MBFCount];
            MBFChars = new short[MBFCount];
            
            

            for (int i=0; i<MBFCount; i++) {
              filename="/fn"+i+".dat";
              F=getClass().getResourceAsStream(filename);
              MBFHeight[i] = (short)F.read();
              MBFChars [i] = (short)(F.read()&0xFF);
              for(short k = 0; k < MBFChars[i]; k++) {
                MBFCharW [i][k] = (short) (F.read()&0xFF);
                MBFPtr   [i][k] = (short) FPos;
                MBFCMap  [i][k] = (F.read()<<8)+F.read();
                if(MBFCharW[i][k]<=0) continue;
                for(int l=0; l<MBFCharW[i][k]; l++) {
                  MBFRender[FPos]=(short)((F.read()<<8)+F.read());
                  FPos++;
                }
              }
              F.close();
            }
        } catch(Exception exception) {
          System.out.println("Font IO error: " + exception.getMessage());
        }

        if(MBTextLength <= 0)  MBTextLength = 8000;
        try { MBMenuDivisor = Image.createImage("/point.png"); }
        catch(Exception exception3) { System.out.println("Image: " + exception3.getMessage()); }
        OpenCFG(17);
        LoadAllCFG();
        CloseCFG();
        MBOpensCount++;
        MBDisplay = Display.getDisplay(this);
        MBookCanvas = new TBookCanvas(this);
        MBDisplay.setCurrent(MBookCanvas);
        MBookCanvas.repaint();
        MBFCurr=0;
    }

    public void startApp() { }

    public void pauseApp() { }

    public void destroyApp(boolean flag) {
        try {
            if((MBRenderCurrentPos > 0) & (!FirstRun)) {
                MBSaveBookmark(true);
                SaveBookmarkCFG();
            }
        }
        catch(Exception exception) { }
    }

    public synchronized void paint(Graphics g) {
        long l = System.currentTimeMillis();
        if(FirstRun) MBScreenStartup(g);
                else MBScreenRender(g);
        int i = (int)(System.currentTimeMillis() - l);
        if(i > MBStatMaxPaintTime) MBStatMaxPaintTime = i;
    }

    private void MBScreenStartup(Graphics g) {
        MBScreenXSz = g.getClipWidth();
        MBScreenYSz = g.getClipHeight();
        int i = 0;
        if(MBScreenYSz == 144) MBScreenYSz = 208;
        g.setColor(0xffffff);
        g.fillRect(0, 0, MBScreenXSz, MBScreenYSz);
        Font font = Font.getFont(32, 1, 0);
        g.setFont(font);
        int k = font.getHeight();
        String s3 = "BookReader V09.1";
        int l = font.stringWidth(s3);
        g.setColor(0);
        Graphics g1 = g;
        g.drawString(s3, MBScreenXSz - l >> 1, i + 2, 20);
        i = i + k + 5;
        s3 = MBRenderBookName;
        font = Font.getFont(64, 0, 8);
        g.setFont(font);
        boolean flag = true;
        String s = " ";
        String s1 = "";
        String s2;
        for(; flag; flag = s2.length() > 0) {
            l = s3.indexOf(' ') + 1;
            if(l == 0) {
                s2 = s3;
                s3 = "";
            } else {
                s2 = s3.substring(0, l);
                s3 = s3.substring(l);
            }
            k = font.stringWidth(s);
            int j = font.stringWidth(s2);
            flag = false;
            if(k > MBScreenXSz) flag = true;
            if(k + j > MBScreenXSz) flag = true;
            if(s2 == "") flag = true;
            if(flag) {
                g.drawString(s, MBScreenXSz - k >> 1, i + 2, 20);
                i += font.getHeight();
                s = " ";
            }
            s = s + s2;
        }

        if(MBPartNo != 0) {
            font = Font.getFont(32, 1, 0);
            g.setFont(font);
            k = font.getHeight();
            s3 = "Vol " + MBPartNo;
            l = font.stringWidth(s3);
            g.setColor(0);
            g.drawString(s3, MBScreenXSz - l >> 1, i + 2, 20);
            i = i + k + 1;
        }
        font = Font.getFont(32, 0, 8);
        g.setFont(font);
        k = font.getHeight();
        s3 = "Handmade";
        l = font.stringWidth(s3);
        g.setColor(0);
        g.drawString(s3, MBScreenXSz - l >> 1, MBScreenYSz - k - 2, 20);
    }

    private void ReadBookPart(int i) {
        boolean flag1 = false;
        boolean flag = false;
        if(i == MBookPos1) {
            flag1 = true;
            flag = true;
        }
        if(i - 1 == MBookPos1) {
            for(int j = 0; j < MBookPart1.length; j++) MBookPart2[j] = MBookPart1[j];
            MBookPos2 = MBookPos1;
            MBookLen2 = MBookLen1;
            flag = true;
        }
        if(i == MBookPos2) {
            for(int k = 0; k < MBookPart1.length; k++) MBookPart1[k] = MBookPart2[k];
            MBookPos1 = MBookPos2;
            MBookLen1 = MBookLen2;
            flag1 = true;
        }
        if(i==0) {
            MBookPos2 = -1;
            MBookLen2 = 0;
            MBookPart2[0] = 0;
            flag = true;
        }
        if(!flag1)
            try {
                MBookPos1 = i;
                InputStream inputstream = getClass().getResourceAsStream("/book" + i + ".dat");
                DataInputStream datainputstream = new DataInputStream(inputstream);
                MBookLen1 = datainputstream.read(MBookPart1);
                if(MBookLen1 < 8192)
                    MBookPart1[MBookLen1] = 0;
                datainputstream.close();
            }
            catch(Exception exception) {
                System.out.println("Main IO error: " + exception.getMessage());
            }
        if(!flag)
            try {
                i--;
                MBookPos2 = i;
                InputStream inputstream1 = getClass().getResourceAsStream("/book" + i + ".dat");
                DataInputStream datainputstream1 = new DataInputStream(inputstream1);
                MBookLen2 = datainputstream1.read(MBookPart2);
                if(MBookLen2 < 8192) MBookPart2[MBookLen2] = 0;
                datainputstream1.close();
            }
            catch(Exception exception1) {
                System.out.println("Main IO error: " + exception1.getMessage());
            }
    }

    private synchronized void ParkLoadedBooks() {
        int i = MBRenderPos >> 13;
        int j = MBRenderPos & 0x1fff;
        int k = MBTextLength >> 13;
        boolean flag = false;
        if(j > 4192 && i != k && i != MBookPos2) {
            i++;
            j -= 8192;
        }
        if(i == MBookPos1 && j < 4192) flag = true;
        if(i == MBookPos1 && k == i)   flag = true;
        if(i == MBookPos2 && j > 2000) {
            j -= 8192;
            flag = true;
        }
        if(!flag) ReadBookPart(i);
        int l = j - 2000;
        int i1 = 0;
        do {
            if(i1 >= 4000) break;
            if(l < 0) MBookRenderableText[i1] = MBookPart2[8192 + l];
                 else MBookRenderableText[i1] = MBookPart1[l];
            if(++l >= 8192) break;
            i1++;
        } while(true);
    }

    private boolean IsWordWrapable(int i) {
        int ai[] = { 32 };
        for(int j = 0; j < ai.length; j++) if(ai[j] == i) return true;
        return false;
    }

//      i - position
//      j - how much characters
//      k - direction: 1 - forward, -1 - back

    private int GetLineH(int i, int j, int k) {
        int h=-1;
        int rp;
        int c;
        for(int p=0; p<j; p++) {
            rp=i+p*k+2000;
            if (rp<0)     return h;
            if (rp>=4000) return h;
            c=MBookRenderableText[rp] & 0xff;
            if ((c>3)&&(c<10)) { if (h<MBFHeight[c-4]) { h=MBFHeight[c-4]; } }
            if (h<MBFHeight[MBFCurr]) { h=MBFHeight[MBFCurr]; }
        }
        return h;
    }

// returns renderable characters from desired position
// 	i - position
//	j - render width
//	k - count direction: 1 - forward, -1 - back
    private int RenderableCharCount(int i, int j, int k) {
        int l = 0;	// Current render length
        int i1 = -1;
        int ccur=MBFCurr;
        for(int k1 = 0; k1 < 1998; k1++) {
            int j1 = i + k1 * k + 2000; 
            if(j1 < 0)          { return k1; }
            if(j1 > 3999)       { return k1; }
            int l1 = MBookRenderableText[j1] & 0xff;
            if((l1 == 0)&&(k>0)){ return k1;	} 	// EOF
            if(l1 == 1)         { return k1 + 1;} 	// Carriage Return
            if(l1 == 2)         i1 = k1;		// Space found
            if(l1 == 3)         i1 = k1;		// Word wrap marker
            if((l1>3)&&(l1<10)) { ccur=l1-4; }		// Switch current font
            l += MBFCharW[ccur][l1];			// Add string length
            if(l > j) {
                if(MBDisplayWordWrap != 0) { return k1; }
                if(i1 == -1) { return k1; }
                        else { return i1 + 1; }
            }
            if(l1 == 3) l -= MBFCharW[ccur][l1]; // Do not count word wraps length
        }
        return 1;
    }

    private void MBScreenRender(Graphics g) {
        g.setColor(MBRenderBG_R, MBRenderBG_G, MBRenderBG_B);
        g.fillRect(0, 0, MBScreenXSz, MBScreenYSz);
        g.setColor(MBRenderFG_R, MBRenderFG_G, MBRenderFG_B);
        int rpos = 0; 	// Rendering position
        int lineH;	// Line Height
        int rdc; 	// Render one line char count
        int H;		// Render Height
        int W;		// Render Width
        int rx, ry, cx, cy;// Base rendering position;
        int blackcount;
        int cchar=-1;
        MBRenderCurrentPos = MBRenderPos;
        try {
            W  = MBScreenXSz-2;
            H  = MBScreenYSz;
            ry = 0;
            do {
                rx = 0;
// Renderable char count
                rdc = RenderableCharCount(rpos, W, 1);
                if (rdc == 0) break; // no need to render
                ry+= GetLineH(rpos, rdc, 1);
                if (ry >= H) break;   // Rendered till end of screen
                MBRenderPos = MBRenderPos + rdc;
                for(int ci = 0; ci < rdc; ci++) {
                  cchar=MBookRenderableText[2000 + rpos] & 0xff;
                  rpos++;
                  if ((cchar>=4)&&(cchar<10)) { MBFCurr=cchar-4; continue; }
                  if (cchar>=MBFChars[MBFCurr]) continue; // Invalid character
                  int charW = MBFCharW[MBFCurr][cchar];
                  if (charW <= 0) continue; // not renderable character, zero width
                  if ((cchar == 3)&&(ci!=(rdc-1))) continue; // not renderable word wrap
                  for(cx=0; cx<charW; cx++) {
                    int iptr=MBFPtr[MBFCurr][cchar];
                    int scanline=MBFRender[iptr+cx];
                    blackcount=-1;
                    for(cy=0; cy< MBFHeight[MBFCurr]; cy++) {
                      if((scanline & 1) != 0) blackcount++; else if(blackcount !=-1) {
                        g.drawLine(rx+cx, ry-cy, rx+cx, ry-cy+blackcount);
                        blackcount = -1;
                      }
                      scanline >>= 1;
                    }
                    if(blackcount != -1) {
                      g.drawLine(rx+cx, ry-cy, rx+cx, ry-cy+blackcount);
                    }
                  }
                  rx+=charW;
                }
            } while(true);

            if(MBTextLength == 0) {
              g.setColor(255, 0, 0);
              g.drawLine(MBScreenXSz-1, MBScreenYSz - 1, MBScreenXSz-1, 0);
            } else {
              int k1 = (MBRenderPos * MBScreenYSz) / MBTextLength;
              g.setColor(0, 255, 0);
              g.drawLine(MBScreenXSz-1, 0, MBScreenXSz-1, k1);
              g.setColor(255, 0, 0);
              g.drawLine(MBScreenXSz-1, k1, MBScreenXSz-1, MBScreenYSz-1);
              g.setColor(0);
              g.drawLine(MBScreenXSz-2, k1-1, MBScreenXSz-2, k1);
            }
            int k = MBRenderPos - MBRenderCurrentPos;
            if(k > MBStatMaxCharsRendered) MBStatMaxCharsRendered = k;
        } catch(Exception exception) { 
          System.out.println("Debug: Exception: "+exception.getMessage());
        }
        MBRenderNextPos = MBRenderPos;
        MBRenderPos = MBRenderCurrentPos;
    }

    public synchronized void keyPressed(int i, int j) {
        _$370(true);
        if(FirstRun) {
            FirstStartupTime = System.currentTimeMillis();
            FirstRun = false;
            MBRenderPos = MBBookmarkPos[0];
            ParkLoadedBooks();
            MBBookTimer.setPaused(false);
            SCRepaint();
            getPrevPos();
        } else {
            switch(i) {
            case -10: 
            case -6: 
                MBMenuMain();
                return;

            case 49: // '1'
                MBMenuAutoscrolling();
                return;

            case 50: // '2'
                MBMenuDisplay();
                return;

            case 51: // '3'
                MBMenuBacklight();
                return;

            case 52: // '4'
                MBMenuNavigator();
                return;

            case 53: // '5'
                MBSaveBookmark(false);
                SaveBookmarkCFG();
                Notify("BookMark", "BookMark added");
                return;

            case 54: // '6'
                MBMenuLoadBookmark();
                return;

            case -4: 
            case -1: 
            case 55: // '7'
                MBRenderNextPos = MBRenderPos;
                MBRenderPos = MBRenderPrevPos;
                ParkLoadedBooks();
                SCRepaint();
                getPrevPos();
                break;

            case 56: // '8'
                MBMenuInfo();
                return;

            case 57: // '9'
                MBMenuAbout();
                return;

            case -3: 
            case -2: 
            case 48: // '0'
                MBRenderPrevPos = MBRenderPos;
                MBRenderPos = MBRenderNextPos;
                ParkLoadedBooks();
                SCRepaint();
                break;
            }
        }
    }

    private void getPrevPos() {
        int k = -1;
        int rs= -1;
        int i;
        int j;
        if(MBDisplayOrientation == 1 || MBDisplayOrientation == 2) {
          i = MBScreenXSz;
          j = MBScreenYSz - 2;
        } else {
          i = MBScreenYSz;
          j = MBScreenXSz - 2;
        }
        
        while (i>0) {
          rs = RenderableCharCount(k, j, -1); if (rs<=0) break;
          i -= GetLineH(k, rs, -1);
          k -=rs;
        }

        MBRenderPrevPos = MBRenderPos + k + 1;
        if(MBRenderPrevPos < 0) MBRenderPrevPos = 0;
    }

    private void SCRepaint() {
        MBookCanvas.repaint();
        MBookCanvas.serviceRepaints();
    }

    private void MBMenuBacklight() {
        MBMenuFBacklight = new Form("Backlight setup");
        MBMenuChoiceGroup[0] = new ChoiceGroup("Backlight", 1);
        MBMenuChoiceGroup[0].append("Always off", null);
        MBMenuChoiceGroup[0].append("Always on", null);
        MBMenuChoiceGroup[0].append("10 sec", null);
        MBMenuChoiceGroup[0].append("20 sec", null);
        MBMenuChoiceGroup[0].append("30 sec", null);
        MBMenuChoiceGroup[0].append("40 sec", null);
        MBMenuChoiceGroup[0].setSelectedIndex(MBBacklightTime, true);
        MBMenuFBacklight.append(MBMenuChoiceGroup[0]);
        MBMenuGauge[0] = new Gauge("Brightness (1-100%)", true, 100, MBBacklightLevel);
        MBMenuFBacklight.append(MBMenuGauge[0]);
        MBMenuFBacklight.addCommand(MBCommandApply);
        MBMenuFBacklight.setCommandListener(this);
        MBDisplayable = MBDisplay.getCurrent();
        MBDisplay.setCurrent(MBMenuFBacklight);
    }

    private void MBMenuDisplay() {
        MBMenuFDisplay = new Form("Display setup");
        MBMenuChoiceGroup[0] = new ChoiceGroup("Layout", 1);
        MBMenuChoiceGroup[0].append("Portrait", null);
        MBMenuChoiceGroup[0].append("Lanscape", null);
        MBMenuChoiceGroup[0].setSelectedIndex(MBDisplayOrientation, true);
        MBMenuFDisplay.append(MBMenuChoiceGroup[0]);
        MBMenuChoiceGroup[1] = new ChoiceGroup("Word wrap", 1);
        MBMenuChoiceGroup[1].append("Enable", null);
        MBMenuChoiceGroup[1].append("Disable", null);
        MBMenuChoiceGroup[1].setSelectedIndex(MBDisplayWordWrap, true);
        MBMenuFDisplay.append(MBMenuDivisor);
        MBMenuFDisplay.append(MBMenuChoiceGroup[1]);
        MBMenuFDisplay.addCommand(MBCommandApply);
        MBMenuFDisplay.setCommandListener(this);
        MBDisplayable = MBDisplay.getCurrent();
        MBDisplay.setCurrent(MBMenuFDisplay);
    }

    private void MBMenuNavigator() {
        MBMenuFNaviagator = new Form("Navigator");
        MBMenuChoiceGroup[0] = new ChoiceGroup("Go to", 1);
        MBMenuChoiceGroup[0].append("First page", null);
        MBMenuChoiceGroup[0].append("Last page", null);
        MBMenuChoiceGroup[0].append("to Current", null);
        MBMenuChoiceGroup[0].append("Position", null);
        MBMenuChoiceGroup[0].setSelectedIndex(2, true);
        MBMenuFNaviagator.append(MBMenuChoiceGroup[0]);
        MBMenuGauge[0] = new Gauge("Position (%)", true, 100, (MBRenderCurrentPos * 100) / MBTextLength);
        MBMenuFNaviagator.append(MBMenuGauge[0]);
        MBMenuFNaviagator.addCommand(MBCommandGoto);
        MBMenuFNaviagator.setCommandListener(this);
        MBDisplayable = MBDisplay.getCurrent();
        MBDisplay.setCurrent(MBMenuFNaviagator);
    }

    private void MBMenuAutoscrolling() {
        MBMenuFAutoscroll = new Form("Autoscrolling");
        MBMenuChoiceGroup[0] = new ChoiceGroup("Autoscrolling", 1);
        MBMenuChoiceGroup[0].append("Disable", null);
        MBMenuChoiceGroup[0].append("1 sec", null);
        MBMenuChoiceGroup[0].append("2 sec", null);
        MBMenuChoiceGroup[0].append("3 sec", null);
        MBMenuChoiceGroup[0].append("4 sec", null);
        MBMenuChoiceGroup[0].append("5 sec", null);
        MBMenuChoiceGroup[0].append("6 sec", null);
        MBMenuChoiceGroup[0].append("7 sec", null);
        MBMenuChoiceGroup[0].setSelectedIndex(MBAutoscrollTime, true);
        MBMenuFAutoscroll.append(MBMenuChoiceGroup[0]);
        MBMenuFAutoscroll.addCommand(MBCommandApply);
        MBMenuFAutoscroll.setCommandListener(this);
        MBDisplayable = MBDisplay.getCurrent();
        MBDisplay.setCurrent(MBMenuFAutoscroll);
    }

    private void MBMenuLoadBookmark() {
        MBMenuFBookmarks = new Form("BookMarks");
        MBMenuChoiceGroup[0] = new ChoiceGroup("Saved bookmarks", 1);
        MBMenuChoiceGroup[0].append("Current", null);
        int i = 0;
        for(int j = 0; j < 5; j++)
            if(MBBookmarkPos[j] > 0)
            {
                MBMenuChoiceGroup[0].append((MBBookmarkPos[j] * 100)/MBTextLength+"%, "+MBBookmarkName[j], null);
                i++;
            }

        if(i > 0) MBMenuChoiceGroup[0].setSelectedIndex(0, true);
        MBMenuFBookmarks.append(MBMenuChoiceGroup[0]);
        if(i > 0) MBMenuFBookmarks.addCommand(MBCommandGoto);
        else      MBMenuFBookmarks.addCommand(MBCommandCancel);
        MBMenuFBookmarks.setCommandListener(this);
        MBDisplayable = MBDisplay.getCurrent();
        MBDisplay.setCurrent(MBMenuFBookmarks);
    }

    private void Notify(String s, String s1) {
        Alert alert = new Alert(s, s1, null, AlertType.INFO);
        alert.setTimeout(700);
        MBDisplay.setCurrent(alert);
    }

    private void MBMenuHelp() {
        MBMenuFHelp = new Form("Help");
        MBMenuFHelp.append(MBMenuDivisor);
        MBMenuFHelp.append(new StringItem("Dial, Left softkey, Fire: ", "Open main menu  \r\n"));
        MBMenuFHelp.append(MBMenuDivisor);
        MBMenuFHelp.append(new StringItem("Key 1: ", "Setup autoscrolling options  \r\n"));
        MBMenuFHelp.append(MBMenuDivisor);
        MBMenuFHelp.append(new StringItem("Key 2: ", "Setup display options  \r\n"));
        MBMenuFHelp.append(MBMenuDivisor);
        MBMenuFHelp.append(new StringItem("Key 3: ", "Setup backlight options  \r\n"));
        MBMenuFHelp.append(MBMenuDivisor);
        MBMenuFHelp.append(new StringItem("Key 4: ", "Open navigator menu  \r\n"));
        MBMenuFHelp.append(MBMenuDivisor);
        MBMenuFHelp.append(new StringItem("Key 5: ", "Save bookmark  \r\n"));
        MBMenuFHelp.append(MBMenuDivisor);
        MBMenuFHelp.append(new StringItem("Key 6: ", "Open saved bookmarks  \r\n"));
        MBMenuFHelp.append(MBMenuDivisor);
        MBMenuFHelp.append(new StringItem("Key 7, Up, Left: ", "Go to previous page  \r\n"));
        MBMenuFHelp.append(MBMenuDivisor);
        MBMenuFHelp.append(new StringItem("Key 8: ", "Show book info and statistics  \r\n"));
        MBMenuFHelp.append(MBMenuDivisor);
        MBMenuFHelp.append(new StringItem("Key 9: ", "Show information about mjBook  \r\n"));
        MBMenuFHelp.append(MBMenuDivisor);
        MBMenuFHelp.append(new StringItem("Key 0, *, #, Down, Right  ", "Go to next page"));
        MBMenuFHelp.addCommand(MBCommandOK);
        MBMenuFHelp.setCommandListener(this);
        MBDisplayable = MBDisplay.getCurrent();
        MBDisplay.setCurrent(MBMenuFHelp);
    }

    private void MBMenuInfo() {
        MBMenuFHelp = new Form("Information");
        MBMenuFHelp.append(MBMenuDivisor);
        MBMenuFHelp.append(new StringItem("Book details: ", MBRenderBookName + "  \r\n"));
        MBMenuFHelp.append(new StringItem("Font details: ", MBRenderBookFont + "  \r\n"));
        MBMenuFHelp.append(new StringItem("Renderer: ", MBRenderBookRenderer + "  \r\n"));
        MBMenuFHelp.append(new StringItem("Book statistics:  ", "Size " + MBTextLength + " byte(s), readed " + (MBRenderPos * 100) / MBTextLength + "%, total reading time " + MBReadingTime() + ", book was opend " + MBOpensCount + " time(s)  \r\n"));
        MBMenuFHelp.addCommand(MBCommandOK);
        MBMenuFHelp.setCommandListener(this);
        MBDisplayable = MBDisplay.getCurrent();
        MBDisplay.setCurrent(MBMenuFHelp);
    }

    private void MBMenuAbout() {
        MBMenuFHelp = new Form("About");
        MBMenuFHelp.append(MBMenuDivisor);
        MBMenuFHelp.append(new StringItem("Version: ", "V09.1\r\n"));
        MBMenuFHelp.append(new StringItem("Statistics: ", "Max paint time " + MBStatMaxPaintTime + " ms, max char in screen " + MBStatMaxCharsRendered + ", perfomance " + (MBStatMaxCharsRendered * 1000) / MBStatMaxPaintTime + " char/sec"));
        MBMenuFHelp.addCommand(MBCommandOK);
        MBMenuFHelp.setCommandListener(this);
        MBDisplayable = MBDisplay.getCurrent();
        MBDisplay.setCurrent(MBMenuFHelp);
    }

    private void MBMenuMain() {
        MBMenuLMain = new List("Menu", 3);
        MBMenuLMain.append("Save BookMark", null);
        MBMenuLMain.append("Load BookMark", null);
        MBMenuLMain.append("Navigator", null);
        MBMenuLMain.append("Display options", null);
        MBMenuLMain.append("Light options", null);
        MBMenuLMain.append("Autoscroll options", null);
        MBMenuLMain.append("Book info", null);
        MBMenuLMain.append("Help", null);
        MBMenuLMain.append("About", null);
        MBMenuLMain.append("Exit", null);
        MBMenuLMain.addCommand(MBCommandSelect);
        MBMenuLMain.addCommand(MBCommandCancel);
        MBMenuLMain.setCommandListener(this);
        MBDisplayable = MBDisplay.getCurrent();
        MBDisplay.setCurrent(MBMenuLMain);
    }

    public void commandAction(Command command, Displayable displayable) {
        boolean flag = false;
        boolean flag1 = false;
        if(displayable == MBMenuLMain)
            if(command != MBCommandCancel)
                switch(MBMenuLMain.getSelectedIndex())
                {
                case 0: // '\0'
                    MBSaveBookmark(false);
                    SaveBookmarkCFG();
                    Notify("BookMark", "BookMark added");
                    return;

                case 1: // '\001'
                    MBMenuLoadBookmark();
                    return;

                case 2: // '\002'
                    MBMenuNavigator();
                    return;

                case 3: // '\003'
                    MBMenuDisplay();
                    return;

                case 4: // '\004'
                    MBMenuBacklight();
                    return;

                case 5: // '\005'
                    MBMenuAutoscrolling();
                    return;

                case 6: // '\006'
                    MBMenuInfo();
                    return;

                case 7: // '\007'
                    MBMenuHelp();
                    return;

                case 8: // '\b'
                    MBMenuAbout();
                    return;

                case 9: // '\t'
                    destroyApp(true);
                    notifyDestroyed();
                    return;
                }
            else flag1 = true;
        if(displayable == MBMenuFDisplay && command == MBCommandApply) {
            MBDisplayOrientation = MBMenuChoiceGroup[0].getSelectedIndex();
            MBDisplayWordWrap = MBMenuChoiceGroup[1].getSelectedIndex();
            flag = true;
        }
        if(displayable == MBMenuFBacklight && command == MBCommandApply) {
            MBBacklightTime = MBMenuChoiceGroup[0].getSelectedIndex();
            MBBacklightLevel = MBMenuGauge[0].getValue();
            flag = true;
        }
        if(displayable == MBMenuFNaviagator && command == MBCommandGoto) {
            flag1 = true;
            switch(MBMenuChoiceGroup[0].getSelectedIndex())
            {
            case 0: // '\0'
                MBRenderPos = 0;
                MBRenderCurrentPos = 0;
                ParkLoadedBooks();
                break;

            case 1: // '\001'
                MBRenderPos = MBTextLength - 100;
                ParkLoadedBooks();
                getPrevPos();
                break;

            case 3: // '\003'
                MBRenderPos = (MBMenuGauge[0].getValue() * MBTextLength) / 100;
                ParkLoadedBooks();
                break;
            }
        }
        if(displayable == MBMenuFBookmarks && command == MBCommandGoto) {
            int i = MBMenuChoiceGroup[0].getSelectedIndex();
            flag1 = true;
            if(i >= 1) {
                i--;
                MBRenderPos = MBBookmarkPos[i];
                ParkLoadedBooks();
                getPrevPos();
            }
        }
        if(displayable == MBMenuFAutoscroll && command == MBCommandApply) {
            MBAutoscrollTime = MBMenuChoiceGroup[0].getSelectedIndex();
            flag = true;
        }
        if(flag) SaveAllCFG();
        if(flag1) MBDisplayable = MBookCanvas;
        MBDisplay.setCurrent(MBDisplayable);
    }

    private void MBSaveBookmark(boolean flag) {
        for(int i = 4; i > 0; i--) {
            MBBookmarkPos[i] = MBBookmarkPos[i - 1];
            MBBookmarkName[i] = MBBookmarkName[i - 1];
        }

        MBBookmarkPos[0] = MBRenderCurrentPos;
        
        byte a[] = new byte[40];
        byte b = 0;
        int j = 2000;
        while(b<40) a[b++] = MBookRenderableText[j++];
        if (flag) {
          MBBookmarkName[0]="(A) "+ConvertCodePage(a);
        } else {
          MBBookmarkName[0]=ConvertCodePage(a);
        }
    }

    private void LoadAllCFG() {
        int i = 3;
        for(int j = 0; j < 5; j++) {
            MBBookmarkPos[j] =LoadCFG(i++, 0);
            MBBookmarkName[j]=LoadCFG(i++, "");
        }

        MBOpensCount         =LoadCFG(1, 0);
        MBBacklightTime      =LoadCFG(12, 1);
        MBBacklightLevel     =LoadCFG(13, 100);
        MBDisplayOrientation =LoadCFG(14, 0);
        MBDisplayWordWrap    =LoadCFG(15, 0);
        MBAutoscrollTime     =LoadCFG(16, 0);
        MBReadTime           =LoadCFG(17, 0);
    }

    private void SaveAllCFG() {
        OpenCFG(17);
        SaveCFG(1,  MBOpensCount + 1);
        SaveCFG(12, MBBacklightTime);
        SaveCFG(13, MBBacklightLevel);
        SaveCFG(14, MBDisplayOrientation);
        SaveCFG(15, MBDisplayWordWrap);
        SaveCFG(16, MBAutoscrollTime);
        SaveCFG(17, MBReadTime + (int)((System.currentTimeMillis() - FirstStartupTime) / 1000L));
        CloseCFG();
    }

    private void SaveBookmarkCFG() {
        int i = 3;
        OpenCFG(17);
        for(int j = 0; j < 5; j++) {
            SaveCFG(i++, MBBookmarkPos[j]);
            SaveCFG(i++, MBBookmarkName[j]);
        }
        CloseCFG();
    }

    private int LoadCFG(int i, int j) {
        String s = "";
        try {
          s = new String(MBRecordStore.getRecord(i));
          return Integer.parseInt(s);
        } catch(Exception exception) {
          return j;
        }
    }

    private String LoadCFG(int i, String s) {
      try {
        return new String(LoadCFG(i, s.getBytes()));
      } catch(Exception exception) {
        return s;
      }
    }

    private byte[] LoadCFG(int i, byte abyte0[]) {
        try {
          return MBRecordStore.getRecord(i);
        } catch(RecordStoreException recordstoreexception) {
          System.err.println("Get store " + i + " error: " + recordstoreexception.getMessage());
          return abyte0;
        }
    }

    private void SaveCFG(int i, int j) {
        SaveCFG(i, Integer.toString(j));
    }

    private void SaveCFG(int i, String s) {
        SaveCFG(i, s.getBytes());
    }

    private void SaveCFG(int i, byte abyte0[]) {
        try {
            MBRecordStore.setRecord(i, abyte0, 0, abyte0.length);
        } catch(RecordStoreException recordstoreexception) {
            System.err.println("Save store " + i + " error: " + recordstoreexception.getMessage());
        }
    }

    private void OpenCFG(int i) {
        try {
            MBRecordStore = RecordStore.openRecordStore("book", true);
            int j = MBRecordStore.getNumRecords();
            byte abyte0[] = " ".getBytes();
            if(j < i) {
                for(int k = j; k < i; k++) MBRecordStore.addRecord(abyte0, 0, abyte0.length);
            }
        }
        catch(RecordStoreException recordstoreexception) {
            System.err.println("Open store error: " + recordstoreexception.getMessage());
        }
    }

    private void CloseCFG() {
        try {
            MBRecordStore.closeRecordStore();
        } catch(RecordStoreException recordstoreexception) {
            System.err.println("Close store error: " + recordstoreexception.getMessage());
        }
    }

    private void _$370(boolean flag)
    {
        long l = System.currentTimeMillis();
        switch(MBBacklightTime) {
        case 0: // '\0'
            MBookCanvas.setLight(0);
            break;

        case 1: // '\001'
            MBookCanvas.setLight(MBBacklightLevel);
            break;

        default:
            if(MBAutoscrollTime > 0) {
                MBookCanvas.setLight(MBBacklightLevel);
                break;
            }
            if(flag) {
                MBookCanvas.setLight(MBBacklightLevel);
                _$270 = l;
                break;
            }
            if(l - _$270 < (long)(10000 * (MBBacklightTime - 1)))
                MBookCanvas.setLight(MBBacklightLevel);
            else
                MBookCanvas.setLight(0);
            break;
        }
    }

    private String MBReadingTime() {
        int i = MBReadTime + (int)((System.currentTimeMillis() - FirstStartupTime) / 1000L);
        int j = i / 3600;
        String s;
        if(j < 10) s = "0"; else s = "";
        s = s + Integer.toString(j) + ":";
        i %= 3600;
        j = i / 60;
        if(j < 10) s = s + "0";
        s = s + Integer.toString(j) + ":";
        j = i % 60;
        if(j < 10) s = s + "0";
        return s + Integer.toString(j);
    }

    public void onTimer() {
        if(MBookCanvas.isShown()) {
            _$370(false);
            if(MBAutoscrollTime > 0) {
                MBAutoScrollTmCnt++;
                if(MBAutoScrollTmCnt > MBAutoscrollTime) {
                    MBAutoScrollTmCnt = 0;
                    keyPressed(48, 2);
                }
            }
        }
    }

    private String ConvertCodePage(byte abyte0[]) {
        char ac[] = new char[abyte0.length];
        int cc=MBFCurr;
        int j;
        int l=0;
        for(int i = 0; i < abyte0.length; i++) {
            if(abyte0[i] < 0) j = 256 + abyte0[i]; else j = abyte0[i];
            if((j>3)&&(j<10)) { cc=j-4; }
            if ((j>10)||(j==2)) {
              j=MBFCMap[cc][j];
              ac[l++]=(char)j;
            }
        }
        return new String(ac);
    }

    private String CfgReadLine(InputStream lis) throws IOException {
        int strlen = lis.read();
        String s = new String("");
        for (byte j=0; j<strlen; j++) s=s+(char)(lis.read()*256+lis.read());
        return s;
    }

    private Display MBDisplay;
    private TBookCanvas MBookCanvas;
    private Displayable MBDisplayable;
    private final String MBVersion = "V09.1";
    private byte MBookRenderableText[];
    private int MBRenderPos;
    private int MBRenderPrevPos;
    private int MBRenderNextPos;
    private int MBRenderCurrentPos;
    private int MBScreenXSz;
    private int MBScreenYSz;
    private boolean FirstRun;
    private int MBTextLength;
    private byte MBookPart1[];
    private byte MBookPart2[];
    private int MBookPos1;
    private int MBookPos2;
    private int MBookLen1;
    private int MBookLen2;
    private String MBRenderBookName;
    private String MBRenderBookFont;
    private String MBRenderBookRenderer;
    private Form MBMenuFDisplay;
    private Form MBMenuFBacklight;
    private Form MBMenuFNaviagator;
    private Form MBMenuFHelp;
    private Form MBMenuFBookmarks;
    private Form MBMenuFAutoscroll;
    private List MBMenuLMain;
    private Image MBMenuDivisor;
    private ChoiceGroup MBMenuChoiceGroup[];
    private Gauge MBMenuGauge[];
    private Command MBCommandSelect;
    private Command MBCommandOK;
    private Command MBCommandApply;
    private Command MBCommandCancel;
    private Command MBCommandGoto;
    private int MBBacklightTime;
    private int MBBacklightLevel;
    private int MBOpensCount;
    private int MBDisplayOrientation;
    private int MBDisplayWordWrap;
    private int MBDisplayLineSpacing;
    private int MBDisplayCharSpacing;
    private int MBRenderBG_R;
    private int MBRenderBG_G;
    private int MBRenderBG_B;
    private int MBRenderFG_R;
    private int MBRenderFG_G;
    private int MBRenderFG_B;
    private int MBReadTime;
    private int MBStatMaxPaintTime;
    private int MBStatMaxCharsRendered;
    private int MBAutoscrollTime;
    private int MBAutoScrollTmCnt;
    private int MBBookmarkPos[];
    private String MBBookmarkName[];
    private int MBPartNo;
    private RecordStore MBRecordStore;
    private long _$270;
    private long FirstStartupTime;
    private TBookTimer MBBookTimer;


    private short MBFRender[];		// Renderable area information
    private short MBFHeight[]; 		// Font height
    private short MBFChars[];		// Characters in font
    private short MBFPtr[][];		// Pointers to characters
    private short MBFCharW[][];		// Character's width
    private int	  MBFCount;		// How many fonts system uses
    private int   MBFCurr;		// Current font number;
    private int   MBFCMap[][];		// Character map
}

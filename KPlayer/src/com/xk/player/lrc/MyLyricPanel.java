/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xk.player.lrc;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JPanel;

import com.xk.player.core.BasicController;
import com.xk.player.core.BasicPlayerEvent;
import com.xk.player.core.BasicPlayerListener;
import com.xk.player.tools.FileUtils;
import com.xk.player.tools.JSONUtil;
import com.xk.player.tools.KrcText;
import com.xk.player.tools.LrcParser;
import com.xk.player.tools.Util;
import com.xk.player.ui.PlayUI;

/**
 *
 * @author hadeslee
 */
public class MyLyricPanel extends JPanel implements Runnable , BasicPlayerListener  {

	private int temp=-1;
	private List<XRCLine> lines;
	private int cur=0;
	private boolean isUp=true;
	private boolean first=true;
    private static final long serialVersionUID = 20071214L;
    private int now=0;
    private long nowTime=0;
    private PlayUI ui;
    private ReentrantLock lock;
	private Condition cond;
	private Condition drawCond;
	private boolean paused=false;
	private boolean drawing =false;
    
    
    public MyLyricPanel(PlayUI ui) {
    	this.ui=ui;
    	lock=new ReentrantLock();
		cond=lock.newCondition();
		drawCond=lock.newCondition();
        Thread th = new Thread(this);
        th.setDaemon(true);
        th.start();
        this.setDoubleBuffered(true);
    }

    public List<XRCLine> getLines() {
		return lines;
	}

	public void setLines(List<XRCLine> lines) {
		if(drawing ){
			lock.lock();
			try {
				drawCond.await();
			} catch (InterruptedException e) {
			} finally {
				lock.unlock();
			}
		}
		this.lines = lines;
		nowTime=0;
		cur=0;
		now=0;
		first=true;
		isUp=true;
		paused=false;
	}



	
	
    protected void paintComponent(Graphics g) {
    	temp*=-1;
    	setSize(getSize().width, getSize().height+temp);
        Graphics2D gd = (Graphics2D) g;
        if(null!=lines){
        	drawing=true;
        	update(gd);
        	drawing=false;
        	lock.lock();
			try {
				drawCond.signalAll();
			} finally {
				lock.unlock();
			}
        }
        gd.dispose();
    }

    private void update(Graphics2D g){
    	if(nowTime==0){
			g.dispose();
			return;
		}
    	long time=ui.getLrcOffset()+nowTime;
    	Font ft=new Font("楷体",Font.PLAIN,36);
    	g.setFont(ft);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		
		XRCLine currentLine=lines.get(cur);
		if(currentLine.start!=null&&currentLine.length!=null&&(time>currentLine.start+currentLine.length)){
			cur++;
			if(cur<lines.size()){
				currentLine=lines.get(cur);
				isUp=!isUp;
				now=0;
			}else{
				cur--;
			}
		}
		if(currentLine.start==null||currentLine.length==null){
			g.dispose();
			return;
		}
		
        
        XRCLine other=null;
        if(cur==0){
			if(currentLine.nodes.size()>1){
				other=lines.get(cur+1);
			}
		}else if((double)(time-currentLine.start)/currentLine.length>0.01){
			if(cur+1<=lines.size()-1){
				other=lines.get(cur+1);
			}
		}else if((double)(time-currentLine.start)/currentLine.length<=0.01){
			other=lines.get(cur-1);
		}
        if(null!=other){
        	Graphics2D gc=(Graphics2D) g.create();
        	gc.setPaint(Color.GREEN);
        	FontMetrics fm=gc.getFontMetrics();
        	GlyphVector gv=ft.createGlyphVector(fm.getFontRenderContext(), other.getWord());
        	Shape shape=gv.getOutline();
        	if(isUp){
        		gc.translate(100, 100+fm.getAscent());
        	}else{
        		gc.translate(50, 50+fm.getAscent());
        	}
        	gc.fill(shape);
        }
        
        for(int i=currentLine.nodes.size()-1;i>=0;i--){
        	XRCNode node=currentLine.nodes.get(i);
        	if(time>currentLine.start+node.start){
        		now=i;
        		break;
        	}
        }
        float off=0;
        for(int i=0;i<now;i++){
        	XRCNode node=currentLine.nodes.get(i);
        	off+=Util.getStringWidth(node.word, g);
        }
        if(currentLine.nodes.size()>0&&currentLine.start!=null&&currentLine.length!=null){
        	 XRCNode node=currentLine.nodes.get(now);
             float percent=(float)(time-(currentLine.start+node.start))/node.length;
             if(percent>1){
            	 percent=1;
             }
             off+=Util.getStringWidth(node.word,g)*percent;
             int baseLeft=isUp?50:100;
             if(off<=0){
            	 off=1;
             }
             g.setPaint(new LinearGradientPaint(baseLeft, 0f,(isUp?50:100)+off , 0f, new float[]{0.98f, 1f}, new Color[]{Color.RED, Color.GREEN}));
             Util.drawString(g.create(), currentLine.getWord(), baseLeft,(isUp?50:100));
        }
        if(first){
        	first=false;
    	}
        
    }

    private void pause(boolean paused){
		this.paused=paused;
		if(!paused){
			lock.lock();
			try {
				cond.signalAll();
			} finally{
				lock.unlock();
			}
			
		}
	}


    public void run() {
        while (true) {
        	if(paused){
				lock.lock();
				try {
					cond.await();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally{
					lock.unlock();
				}
			}
            try {
                Thread.sleep(100);
                if(getParent().isVisible()&&!paused&&null!=lines){
                	repaint();
                }
            } catch (Exception exe) {
                exe.printStackTrace();
            }
        }
    }

    @Override
	public void opened(Object stream, Map<String,Object> properties) {
    	nowTime=0;
		lines=null;
		cur=0;
		pause(false);
		Long allLength=(Long) properties.get("duration");
		if(stream instanceof File){
			File file=(File) stream;
			String filename=file.getAbsolutePath();
			File songWord = new File(filename.substring(0, filename
					.lastIndexOf("."))
					+ ".lrc");
			File xrcWord = new File(filename.substring(0, filename
					.lastIndexOf("."))
					+ ".zlrc");
			File krcWord = new File(filename.substring(0, filename
					.lastIndexOf("."))
					+ ".krc");
			List<XRCLine> lines=null;
			if(xrcWord.exists()){
				String data=FileUtils.readString(xrcWord.getAbsolutePath());
				lines=JSONUtil.toBean(data, JSONUtil.getCollectionType(List.class, XRCLine.class));
			}else if (krcWord.exists()) {
				lines=KrcText.fromKRC(krcWord.getAbsolutePath());
			}else if (songWord.exists()) {
				try {
					LrcParser parser = new LrcParser(allLength);
					lines = parser.parser(songWord.getAbsolutePath());
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			if(null!=lines){
				setLines(lines);
			}
		}
	}

	@Override
	public void progress(int bytesread, long microseconds, byte[] pcmdata,
			Map<String,Object> properties) {
		long now=ui.jumpedMillan+microseconds/1000;
		if(now-nowTime>70){
			nowTime=now;
		}
		
	}

	@Override
	public void stateUpdated(BasicPlayerEvent event) {
		if(event.getCode()==BasicPlayerEvent.PAUSED){
			pause(true);
		}else if(event.getCode()==BasicPlayerEvent.RESUMED){
			pause(false);
		}
	}

	@Override
	public void setController(BasicController controller) {
		// TODO Auto-generated method stub
		
	}

}

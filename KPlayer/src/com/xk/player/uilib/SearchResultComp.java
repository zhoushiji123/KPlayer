package com.xk.player.uilib;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.xk.player.tools.FileUtils;
import com.xk.player.tools.Loginer;
import com.xk.player.tools.LrcInfo;
import org.eclipse.wb.swt.SWTResourceManager;
import com.xk.player.tools.SongSeacher;
import com.xk.player.tools.SongSeacher.SearchInfo;
import com.xk.player.ui.LTableItem;
import com.xk.player.ui.PlayUI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;

public class SearchResultComp extends Composite implements ICallable{

	private MyList list;
	private ICallback callBack;
	private Label label;
	private String path;
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public SearchResultComp(Composite parent, int style) {
		super(parent, style);
		
		setBackgroundImage(parent.getParent().getBackgroundImage());
		setBackgroundMode(SWT.INHERIT_DEFAULT);
		
		label = new Label(this, SWT.NONE);
		label.setBounds(10, 5, 374, 17);
		label.setFont(SWTResourceManager.getFont("微软雅黑", 10, SWT.NORMAL));
		label.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		
		
		//关闭按钮
		Image clo=SWTResourceManager.getImage(PlayUI.class,"/images/close.png");
		Image clofoc=SWTResourceManager.getImage(PlayUI.class,"/images/close_focus.png");
		ColorLabel close = new ColorLabel(this, SWT.NONE,clo,clofoc);
		close.setBounds(442, 0, 28, 19);
		
		list = new MyList(this,455,260);
		list.setBounds(10, 30, 455, 260);
		list.setMask(180);
		list.setSimpleSelect(true);
		
		close.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(MouseEvent mouseevent) {
				callBack.callback(null);
			}
			
		});

	}

	public void setData(List<SearchInfo>infos){
		label.setText("为您搜索到"+infos.size()+"个结果！");
		list.clearAll();
		SearchInfo head=new SearchInfo();
		head.name="歌名";
		head.singer="歌手";
		head.url="";
		LTableItem lo=new LrcItem(head);
		lo.setHead(true);
		list.addItem(lo);
		for(SearchInfo info:infos){
			LTableItem item=new LrcItem(info);
			list.addItem(item);
		}
	}
	
	@Override
	protected void checkSubclass() {
	}

	@Override
	public void setCallBack(ICallback callBack) {
		this.callBack=callBack;
		
	}
	
	public void setPath(String path) {
		this.path = path;
	}

	class LrcItem extends LTableItem{

		public LrcItem(SearchInfo info) {
			super(info);
		}

		@Override
		protected void download() {
			if(downloading){
				return;
			}
			downloading=true;
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					String url=info.url;
					String html=Loginer.getInstance("player").getHtml(url);
					persent=30;
					flush();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					LrcInfo lrcs=SongSeacher.perseFromHTML(html);
					persent=60;
					flush();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					Map<Long ,String>ls=lrcs.getInfos();
					StringBuffer sb=new StringBuffer();
					for(Long time:ls.keySet()){
						String text=ls.get(time);
						long mil=(time%1000);
						long sec=time/1000;
						long mun=sec/60;
						long second=sec%60;
						sb.append("[").append(format(mun))
						.append(":").append(format(second)).append(".")
						.append(format(mil)).append("]").append(text).append("\r\n");
					}
					String lrcPath=path.substring(0, path.lastIndexOf("."))+".lrc";
					File file=new File(lrcPath);
					if(file.exists()){
						file.delete();
					}
					try {
						file.createNewFile();
					} catch (IOException e) {
					}
					persent=80;
					flush();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					FileUtils.writeString(sb.toString(), file);
					persent=100;
					flush();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					downloading=false;
					flush();
				}
			}).start();
			
		}
		
		private String format(long time){
			if(time<10){
				return "0"+time;
			}
			if(time>99){
				return time/10+"";
			}
			return time+"";
		}
	}
}

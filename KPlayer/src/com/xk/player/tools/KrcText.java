package com.xk.player.tools;
import java.io.BufferedReader;
import java.io.File;  
import java.io.FileInputStream;  
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.xk.player.lrc.XRCLine;
import com.xk.player.lrc.XRCNode;

public class KrcText  
{  
    private static final char[] miarry = { '@', 'G', 'a', 'w', '^', '2', 't',  
            'G', 'Q', '6', '1', '-', 'Î', 'Ò', 'n', 'i' };  
    
    public static void main(String[]args){
    	String filenm="e:/download/张靓颖 - 画心.krc";
    	try {
			System.out.println(new KrcText().getKrcText(filenm));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
      
    public static List<XRCLine> fromKRC(String filenm) {  
    	List<XRCLine> lines=new ArrayList<XRCLine>();
        StringReader reader;
		try {
			reader = new StringReader(new KrcText().getKrcText(filenm));
		} catch (IOException e) {
			return lines;
		}
        BufferedReader br=new BufferedReader(reader);
        String line=null;
        try {
			while((line=br.readLine())!=null){
				XRCLine xline=new XRCLine();
				String regLine = "\\[[0-9]*,[0-9]*\\]";   
			    // 编译   
			    Pattern pattern = Pattern.compile(regLine);   
			    Matcher matcher = pattern.matcher(line);   
  
			    if (matcher.find()) {   
			        String msg = matcher.group();  
			        msg=msg.replace("[", "").replace("]", "");
			        String[]spls=msg.split(",");
			        if(spls.length==2){
			        	xline.start=Long.parseLong(spls[0]);
			        	xline.length=Long.parseLong(spls[1]);
			        	lines.add(xline);
			        }
			        
			    }
			    String regNode="\\<[0-9]*,[0-9]*,[0-9]*\\>(\\w+'*\\w*\\s*|[\u4E00-\u9FA5]{1})";
			    Pattern patternNode = Pattern.compile(regNode);   
			    Matcher matcherNode = patternNode.matcher(line);   
  
			    while (matcherNode.find()) {   
			    	int groupCount = matcherNode.groupCount();
			    	for(int i=0;i<=groupCount;i++){
			    		String msg=matcherNode.group(i);
			    		XRCNode node=new XRCNode();
			    		int last=msg.indexOf(">")+1;
			    		node.word=msg.substring(last, msg.length());
			    		msg=msg.substring(0, last).replace("<", "").replace(">", "");
			    		String[]spls=msg.split(",");
			    		if(spls.length==3){
			    			node.start=Long.parseLong(spls[0]);
			    			node.length=Long.parseLong(spls[1]);
			    			xline.nodes.add(node);
			    		}
			    	}
			        
			    }
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return lines;
    }  
      
    /** 
     *  
     * @param filenm krc文件路径加文件名 
     * @return krc文件处理后的文本 
     * @throws IOException 
     */  
    public String getKrcText(String filenm) throws IOException  
    {  
        File krcfile = new File(filenm);  
        byte[] zip_byte = new byte[(int) krcfile.length()];  
        FileInputStream fileinstrm = new FileInputStream(krcfile);  
        byte[] top = new byte[4];  
        fileinstrm.read(top);  
        fileinstrm.read(zip_byte);  
        fileinstrm.close();
        int j = zip_byte.length;  
        for (int k = 0; k < j; k++)  
        {  
            int l = k % 16;  
            int tmp67_65 = k;  
            byte[] tmp67_64 = zip_byte;  
            tmp67_64[tmp67_65] = (byte) (tmp67_64[tmp67_65] ^ miarry[l]);  
        }  
        String krc_text = new String(ZLibUtils.decompress(zip_byte), "utf-8");  
        return krc_text;  
    }  
}  